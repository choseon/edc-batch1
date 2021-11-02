package kcs.edc.batch.jobs.opd.iac003l.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 기업개황정보 데이터수집 Tasklet Class
 */
public class Iac003lTasklet {

    /******************* API 인증키 정보 **************************************/
    //private static String crtfcKey = "70ba99fc377c718ce8bba08ae757ef98def20a4b"; //API 인증키 (운영) IP 211.173.37.77에서 무제한 사용 가능
    /*** 테스트 인증키 ***/
    private static String crtfcKey = "1d469738d973a289b44a4e923fe1466dcf18e5a4"; //API 인증키
    //private static String crtfcKey = "8c45ec2be39327de0815ab43c7a766b1d47bbcd6"; //API 인증키
    //private static String crtfcKey = "baff79aeea865f50f1b7ef811ede36ff79ee1031"; //API 인증키
    /**********************************************************************/

    /******************* 실행 서버에 따라 변경해야 하는 값 **************************************/
    private static String filePathNm = "D:\\test\\"; //API 호출하여 제공받은 파일이 저장되는 루트 경로
    private static String strSep = "\\"; // 디렉토리를 구분하는 문자 (윈도우 \\ , 유닉스or리눅스 /)
    /*********************************************************************************/

    private static String dailyFilePathNm = ""; //날짜별로 생성할 디렉토리의 경로명

    private static int totCnt = 0;
    private static int stockCnt = 0;

    public static void main(String[] args) {

        dailyFilePathNm = creDir();// 파일을 저장할 오늘날짜의 디렉토리 생성 (루트 경로 하위로 디렉토리 생성)


        fileDelete();//기존 파일이 남아있으면 삭제(테스트의 경우가 아니면 여기 탈일은 없음)

        String dwnlFileNm = ""; //API 호출하여 제공받을 파일의 파일명 저장 변수

        try {

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(5000); //타임아웃 설정 5초
            factory.setReadTimeout(5000);//타임아웃 설정 5초
            RestTemplate restTemplate = new RestTemplate(factory);

            // 고유번호 압축 파일 다운로드
            String url = "https://opendart.fss.or.kr/api/corpCode.xml";

            UriComponents uri = UriComponentsBuilder.fromHttpUrl(url+"?"+"crtfc_key=" + crtfcKey).build();

            Path file = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
                Path zipFile = Files.createTempFile("opendart-", ".zip");
                Files.write(zipFile, response.getBody().readAllBytes());

                return zipFile;
            });


            // 압축 해제하여 xml파일 생성
            Path zipFile = file;
            byte[] buf = Files.readAllBytes(zipFile);
            ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(buf));
            ZipEntry zipEntry = null;

            if ((zipEntry = zipInputStream.getNextEntry()) != null) {
                Files.copy(zipInputStream, Paths.get(filePathNm + dailyFilePathNm + zipEntry.getName()));
                dwnlFileNm = zipEntry.getName(); //압축 해제하여 생성한 파일명
            }
            zipInputStream.closeEntry();
            zipInputStream.close();


            xmlParsing(dwnlFileNm);//xml 파싱하여 json파일로 저장

            saveModifyDt();// 처리일 저장

        } catch (HttpClientErrorException e) {

            System.out.println(e.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
        }


    }
    //XML 파싱하여 기업 고유번호 추출
    private static void xmlParsing(String dwnlFileNm)  throws ParserConfigurationException, SAXException {

        try {
            String lastModifyDt = getLastModifyDt(); //이전 배치를 실행한 날짜
            String modifyDt = ""; //개황정보 수정일자
            String stockCd = ""; //거래소코드

            ArrayList arrCorp = new ArrayList();

            StringBuffer jsonWriteString = new StringBuffer();

            File file = new File(filePathNm + dailyFilePathNm + dwnlFileNm);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            document.getDocumentElement().normalize();

            NodeList nList = document.getElementsByTagName("list");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                totCnt++;
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    //고유번호 가져오기
                    modifyDt = eElement.getElementsByTagName("modify_date").item(0).getTextContent();
                    stockCd =  eElement.getElementsByTagName("stock_code").item(0).getTextContent();
                    stockCd = stockCd.trim();


                    //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
                    if(getNewDataYN(lastModifyDt, modifyDt)){
                        if(!"".equals(stockCd)) {
                            stockCnt++;
                            arrCorp.add(eElement.getElementsByTagName("corp_code").item(0).getTextContent());
                        }
                    }
                }
            }

            if(arrCorp.size()>0) {
                jsonWriteString = company(arrCorp); //고유번호로 개황정보 가져오기
                FileWriter writer=null;
                /***************************json 파일 생성하기**************************************/
                /***************************json 파일 생성하기**************************************/
                try {
                    writer = new FileWriter(filePathNm + dailyFilePathNm + "company.json");
                    writer.write(jsonWriteString.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(jsonWriteString.toString());
                /********************************************************************************/
                /********************************************************************************/
            }

            System.out.println("totCnt====>" + totCnt);
            System.out.println("stockCnt====>" + stockCnt);

        }
        catch(IOException e) {
            System.out.println(e);
        }
    }


    // 개황정보 가져오기
    private static StringBuffer company(ArrayList arrCorp) {

        String jsonInString = "";
        StringBuffer jsonWriteString = new StringBuffer();

        try {

            for(int i=0;i<arrCorp.size();i++){

                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(5000); //타임아웃 설정 5초
                factory.setReadTimeout(5000);//타임아웃 설정 5초
                RestTemplate restTemplate = new RestTemplate(factory);

                HttpHeaders header = new HttpHeaders();
                HttpEntity<?> entity = new HttpEntity<>(header);

                // 개황정보 가져오기
                String url = "https://opendart.fss.or.kr/api/company.json";

                UriComponents uri = UriComponentsBuilder.fromHttpUrl(url+"?crtfc_key=" + crtfcKey + "&corp_code=" + arrCorp.get(i)).build();

                ResponseEntity<Map> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, Map.class);

                ObjectMapper mapper = new ObjectMapper();
                jsonInString = mapper.writeValueAsString(resultMap.getBody());

                jsonWriteString.append(jsonInString + "\n");
            }
            System.out.println(jsonWriteString.toString());

        } catch (HttpClientErrorException  e) {

            System.out.println(e.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return jsonWriteString;
    }

    private static void fileDelete() {
        File file1 = new File(filePathNm + dailyFilePathNm + "CORPCODE.xml");
        if(file1.exists()){
            file1.delete();
        }
        File file2 = new File(filePathNm + dailyFilePathNm + "company.json");
        if(file2.exists()){
            file2.delete();
        }
    }


    private static String creDir() {

        SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
        SimpleDateFormat MM = new SimpleDateFormat("MM");
        SimpleDateFormat dd = new SimpleDateFormat("dd");

        Calendar calendar = Calendar.getInstance();

        Date dateObj = calendar.getTime();
        String strYYYY = yyyy.format(dateObj);
        String strMM = MM.format(dateObj);
        String strDD = dd.format(dateObj);


        File Folder = new File(filePathNm + strYYYY );

        if (!Folder.exists()) {
            Folder.mkdir();
        }

        Folder = new File(filePathNm + strYYYY + strSep + strMM );

        if (!Folder.exists()) {
            Folder.mkdir();
        }

        Folder = new File(filePathNm + strYYYY + strSep + strMM + strSep + strDD );

        if (!Folder.exists()) {
            Folder.mkdir();
        }

        Folder = new File(filePathNm + strYYYY + strSep + strMM + strSep + strDD  + strSep + "corp");

        if (!Folder.exists()) {
            Folder.mkdir();
        }

        return strYYYY + strSep + strMM + strSep + strDD + strSep + "corp" + strSep;
    }

    //시작일자 가져오기  (이전 배치 실행시 적용한 종료일 항목값)
    private static String getLastModifyDt() {

        File file = new File(filePathNm + "corpLastModifyDate.txt");
        String returnDate = "";

        try {
            if(file.exists()) {

                BufferedReader inFile = new BufferedReader(new FileReader(file));
                String sLine = null;

                if( (sLine = inFile.readLine()) != null )
                    returnDate =  sLine;

                inFile.close();

            }else {
                returnDate = "19000101";

            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return returnDate;
    }

    //이전 배치 실행일자 이후에 개황정보 수정한 자료인지 여부
    private static boolean getNewDataYN(String lastModifyDt, String modifyDt) {
        boolean returnValue = false;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyyMMdd");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            String strYesterDay = dateFormat.format(calendar.getTime());


            Date yesterDay = dateFormat.parse(strYesterDay); //어제날짜
            Date lastModifyDate = dateFormat.parse(lastModifyDt);
            Date modifyDate = dateFormat.parse(modifyDt);

            //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
            if((modifyDate.equals(lastModifyDate) || modifyDate.after(lastModifyDate)) && (modifyDate.equals(yesterDay) || modifyDate.before(yesterDay))){
                returnValue = true;
            }
        } catch (ParseException ex) {
        }
        return returnValue;
    }

    //처리일 저장
    private static void saveModifyDt() {
        // 이번 배치에
        try {
            FileWriter writer=null;

            writer = new FileWriter(filePathNm + "corpLastModifyDate.txt");
            writer.write(getTargetDate(0));

            writer.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

    }

    //n일전 날짜 가져오기
    private static String getTargetDate(int n) {

        String returnDate = "";

        SimpleDateFormat schDate = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DATE, -n);
        returnDate = schDate.format(calendar.getTime());

        return returnDate;
    }
}
