package kcs.edc.batch.jobs.opd.iac016l.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 금융감독원 OpenDart 기업공시정보 데이터수집 Tasklet
 */
public class Iac016lTasklet extends CmmnJob implements Tasklet {


    /******************* API 인증키 정보 **************************************/
    //private static String crtfcKey = "70ba99fc377c718ce8bba08ae757ef98def20a4b"; //API 인증키 (운영) IP 211.173.37.77에서 무제한 사용 가능
    /*** 테스트 인증키 ***/
    private static String crtfcKey = "1d469738d973a289b44a4e923fe1466dcf18e5a4"; //API 인증키
    //private static String crtfcKey = "8c45ec2be39327de0815ab43c7a766b1d47bbcd6"; //API 인증키
    //private static String crtfcKey = "baff79aeea865f50f1b7ef811ede36ff79ee1031"; //API 인증키
    /*********************************************************************************/

    /******************* 실행 서버에 따라 변경해야 하는 값 **************************************/
    private static String filePathNm = "D:\\test\\"; //API 호출하여 제공받은 파일이 저장되는 루트 경로
    private static String strSep = "\\"; // 디렉토리를 구분하는 문자 (윈도우 \\ , 유닉스or리눅스 /)
    /*********************************************************************************/

    private static String dailyFilePathNm = ""; //날짜별로 생성할 디렉토리의 경로명

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        dailyFilePathNm = creDir();//오늘날짜의 디렉토리 생성

        fileDelete();//기존 파일이 남아있으면 삭제

        try {

            ArrayList<String> corpList = getCorpList(); //기업 고유번호 목록 가져오기

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(5000); //타임아웃 설정 5초
            factory.setReadTimeout(5000);//타임아웃 설정 5초
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders header = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(header);

            // 공시검색
            String url = "https://opendart.fss.or.kr/api/list.json";

            String bgn_de = getSchDate(); //시작일 (이전 배치가 실행된 날짜 가져오기)
            if(null==bgn_de || "".equals(bgn_de)) { //없으면
                bgn_de = getTargetDate(1095); //조회 시작일을 3년전 날짜로
            }

            String end_de = getTargetDate(1); //종료일 (어제날짜)
            // 배치가 매일매일 정상 실행될 경우 bgn_de와 end_de 값은 어제 날짜가 됨

            String last_reprt_at = "Y";//	최종보고서 검색여부
            String pblntf_ty = "";//	공시유형코드

            String strParam = "";
            StringBuffer strResult = new StringBuffer();
            String saveFileNm = "";

            int pageNo = 1;
            int totPageNo = 0;

            String stockCd = "";

            String [] cd = getPblntfDetailTy(); //공시상세유형 가져오기

            for(int corpLoop=0; corpLoop < corpList.size();corpLoop++) {
                String corp_code = (String)corpList.get(corpLoop); //기업 고유번호
                for(int cdCnt=0;cdCnt < cd.length;cdCnt++) { //공시상세유형 별로 조회
                    pageNo = 1;
                    do {

                        pblntf_ty = cd[cdCnt].substring(0, 1); //공시유형코드

                        strParam = "&corp_code=" + corp_code;
                        strParam = strParam + "&bgn_de=" + bgn_de + "&end_de=" + end_de + "&last_reprt_at=" + last_reprt_at ;
                        strParam = strParam + "&pblntf_ty=" + pblntf_ty + "&pblntf_detail_ty=" + cd[cdCnt];
                        strParam = strParam + "&page_no=" + String.valueOf(pageNo) + "&page_count=100";

                        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url+"?"+"crtfc_key=" + crtfcKey + strParam).build();

                        ResponseEntity<Map> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, Map.class);

                        ObjectMapper mapper = new ObjectMapper();
                        String jsonInString = mapper.writeValueAsString(resultMap.getBody());

                        /*JSONParser jsonParser = new JSONParser();
                        JSONObject jsonObject = (JSONObject)jsonParser.parse(jsonInString);

                        String status = (String)jsonObject.get("status");// 수신상태

                        if("000".equals(status)) { //정상

                            totPageNo = Integer.parseInt(String.valueOf(jsonObject.get("total_page")));

                            JSONArray list = (JSONArray) jsonObject.get("list");

                            for(int i=0; i < list.size(); i++) {
                                JSONObject listObject = (JSONObject) list.get(i);

                                stockCd = (String)listObject.get("stock_code");
                                stockCd = stockCd.trim();
                                if(!"".equals(stockCd)) {

                                    saveFileNm = (String)listObject.get("rcept_no") + "_" + pblntf_ty + "_" + cd[cdCnt] + ".zip";

                                    //보고서 원문파일 가져와서 저장하기
                                    saveReportFile((String)listObject.get("rcept_no"), dailyFilePathNm, saveFileNm);

                                    strResult.append("{\"corp_cls\":\"" + (String)listObject.get("corp_cls") + "\"");
                                    strResult.append(",\"corp_name\":\"" + (String)listObject.get("corp_name") + "\"");
                                    strResult.append(",\"stock_code\":\"" + (String)listObject.get("stock_code") + "\"");
                                    strResult.append(",\"report_nm\":\"" + (String)listObject.get("report_nm") + "\"");
                                    strResult.append(",\"rcept_no\":\"" + (String)listObject.get("rcept_no") + "\"");
                                    strResult.append(",\"flr_nm\":\"" + (String)listObject.get("flr_nm") + "\"");
                                    strResult.append(",\"rcept_dt\":\"" + (String)listObject.get("rcept_dt") + "\"");
                                    strResult.append(",\"rm\":\"" + (String)listObject.get("rm") + "\"");
                                    strResult.append(",\"pblntf_ty\":\"" + pblntf_ty + "\"");
                                    strResult.append(",\"pblntf_detail_ty\":\"" + cd[cdCnt] + "\"");
                                    strResult.append(",\"file_path_nm\":\"" + filePathNm + "\""); //API 호출하여 받아온 파일의 저장 경로의 루트 경로
                                    strResult.append(",\"rcpn_file_path_nm\":\"" + dailyFilePathNm + "\""); //년/월/일로 생성한 경로
                                    strResult.append(",\"srbk_file_nm\":\"" + (String)listObject.get("report_nm") + ".zip\"");
                                    strResult.append(",\"sorg_file_nm\":\"" + saveFileNm + "\"},\n");


                                }

                            }
                            pageNo++;
                        }else {
                            totPageNo = 0;
                        }*/

                    }while(totPageNo >= pageNo);
                }
            }
            /***************************json 파일 생성하기**************************************/
            /***************************json 파일 생성하기**************************************/
            FileWriter writer=null;

            if(null!=strResult) {
                try {
                    writer = new FileWriter(filePathNm + dailyFilePathNm + "report.json");
                    writer.write(strResult.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println(strResult.toString());
            /********************************************************************************/
            /********************************************************************************/
            saveEndDate();//배치 수행한 날짜 저장 (다음 배치 수행시 시작일값으로 사용하기 위함)


        } catch (HttpClientErrorException e) {
            System.out.println(e.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return RepeatStatus.FINISHED;
    }


    private static void fileDelete() {
        File file1 = new File(filePathNm + dailyFilePathNm + "list.xml");
        if(file1.exists()){
            file1.delete();
        }
        File file2 = new File(filePathNm + dailyFilePathNm + "report.json");
        if(file2.exists()){
            file2.delete();
        }
        File file3 = new File(filePathNm + dailyFilePathNm + "CORPCODE.xml");
        if(file3.exists()){
            file3.delete();
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

        Folder = new File(filePathNm + strYYYY + strSep + strMM + strSep + strDD  + strSep + "rept");

        if (!Folder.exists()) {
            Folder.mkdir();
        }

        return strYYYY + strSep + strMM + strSep + strDD + strSep + "rept" + strSep;

    }
    //시작일자 가져오기  (이전 배치 실행시 적용한 종료일 항목값)
    private static String getSchDate() {

        File file = new File(filePathNm + "reptLastModifyDate.txt");
        String returnDate = "";

        try {
            if(file.exists()) {

                BufferedReader inFile = new BufferedReader(new FileReader(file));
                String sLine = null;

                if( (sLine = inFile.readLine()) != null )
                    returnDate =  sLine;

                inFile.close();

            }else {


            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return returnDate;
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
    //종료일 저장
    private static void saveEndDate() {
        // 이번 배치에
        try {
            FileWriter writer=null;

            writer = new FileWriter(filePathNm + "reptLastModifyDate.txt");
            writer.write(getTargetDate(0));

            writer.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

    }

    //공시상세유형
    private static String[] getPblntfDetailTy() {
        String[] cd = new String[57];

        cd[0] = "A001"; //	사업보고서
        cd[1] = "A002"; //	반기보고서
        cd[2] = "A003"; //	분기보고서
        cd[3] = "A004"; //	등록법인결산서류(자본시장법이전)
        cd[4] = "A005"; //	소액공모법인결산서류
        cd[5] = "B001"; //	주요사항보고서
        cd[6] = "B002"; //	주요경영사항신고(자본시장법 이전)
        cd[7] = "B003"; //	최대주주등과의거래신고(자본시장법 이전)
        cd[8] = "C001"; //	증권신고(지분증권)
        cd[9] = "C002"; //	증권신고(채무증권)
        cd[10] = "C003"; //	증권신고(파생결합증권)
        cd[11] = "C004"; //	증권신고(합병등)
        cd[12] = "C005"; //	증권신고(기타)
        cd[13] = "C006"; //	소액공모(지분증권)
        cd[14] = "C007"; //	소액공모(채무증권)
        cd[15] = "C008"; //	소액공모(파생결합증권)
        cd[16] = "C009"; //	소액공모(합병등)
        cd[17] = "C010"; //	소액공모(기타)
        cd[18] = "C011"; //	호가중개시스템을통한소액매출
        cd[19] = "D001"; //	주식등의대량보유상황보고서
        cd[20] = "D002"; //	임원ㆍ주요주주특정증권등소유상황보고서
        cd[21] = "D003"; //	의결권대리행사권유
        cd[22] = "D004"; //	공개매수
        cd[23] = "E001"; //	자기주식취득/처분
        cd[24] = "E002"; //	신탁계약체결/해지
        cd[25] = "E003"; //	합병등종료보고서
        cd[26] = "E004"; //	주식매수선택권부여에관한신고
        cd[27] = "E005"; //	사외이사에관한신고
        cd[28] = "E006"; //	주주총회소집공고
        cd[29] = "E007"; //	시장조성/안정조작
        cd[30] = "E008"; //	합병등신고서(자본시장법 이전)
        cd[31] = "E009"; //	금융위등록/취소(자본시장법 이전)
        cd[32] = "F001"; //	감사보고서
        cd[33] = "F002"; //	연결감사보고서
        cd[34] = "F003"; //	결합감사보고서
        cd[35] = "F004"; //	회계법인사업보고서
        cd[36] = "F005"; //	감사전재무제표미제출신고서
        cd[37] = "G001"; //	증권신고(집합투자증권-신탁형)
        cd[38] = "G002"; //	증권신고(집합투자증권-회사형)
        cd[39] = "G003"; //	증권신고(집합투자증권-합병)
        cd[40] = "H001"; //	자산유동화계획/양도등록
        cd[41] = "H002"; //	사업/반기/분기보고서
        cd[42] = "H003"; //	증권신고(유동화증권등)
        cd[43] = "H004"; //	채권유동화계획/양도등록
        cd[44] = "H005"; //	수시보고
        cd[45] = "H006"; //	주요사항보고서
        cd[46] = "I001"; //	수시공시
        cd[47] = "I002"; //	공정공시
        cd[48] = "I003"; //	시장조치/안내
        cd[49] = "I004"; //	지분공시
        cd[50] = "I005"; //	증권투자회사
        cd[51] = "I006"; //	채권공시
        cd[52] = "J001"; //	대규모내부거래관련
        cd[53] = "J002"; //	대규모내부거래관련(구)
        cd[54] = "J004"; //	기업집단현황공시
        cd[55] = "J005"; //	비상장회사중요사항공시
        cd[56] = "J006"; //	기타공정위공시

        //A : 정기공시
        //B : 주요사항보고
        //C : 발행공시
        //D : 지분공시
        //E : 기타공시
        //F : 외부감사관련
        //G : 펀드공시
        //H : 자산유동화
        //I : 거래소공시
        //j : 공정위공시

        return cd;

    }

    //보고서 원문 파일 다운받기
    public static void saveReportFile(String rceptNo, String dailyFilePathNm, String fileNm) {

        try {

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(5000); //타임아웃 설정 5초
            factory.setReadTimeout(5000);//타임아웃 설정 5초
            RestTemplate restTemplate = new RestTemplate(factory);

            String url = "https://opendart.fss.or.kr/api/document.xml";

            String strParam = "&rcept_no=" + rceptNo ;

            UriComponents uri = UriComponentsBuilder.fromHttpUrl(url+"?"+"crtfc_key=" + crtfcKey + strParam).build();
            //		Path file = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
            //	        Path zipFile = Files.createTempFile("opendart-", ".zip");
            //	        Files.write(zipFile, response.getBody().readAllBytes());
            //
            //	        return zipFile;
            //	    });
            Path file = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
                Path zipFile = Files.createTempFile("opendart-", ".zip");
                InputStream ins = response.getBody();
                byte[] bytes = IOUtils.toByteArray(ins);
                Files.write(zipFile, bytes);
                return zipFile;
            });

            Path zipFile = file;
            byte[] buf = Files.readAllBytes(zipFile);

            //압축파일 저장하기
            saveZipFile(dailyFilePathNm, fileNm, buf);

        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }


    //압축파일 저장하기
    public static void saveZipFile(String dailyFilePathNm, String fileNm, byte[] buf){

        if(buf != null){

            try{
                File file = new File(filePathNm + dailyFilePathNm + fileNm);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buf);
                fos.close();
            }catch(Throwable e){
                e.printStackTrace(System.out);
            }
        }

    }

    //기업 고유번호 가져오기
    private static ArrayList<String> getCorpList(){

        ArrayList<String> corpList = new ArrayList<String>();

        try {

            String dwnlFileNm = "";

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(5000); //타임아웃 설정 5초
            factory.setReadTimeout(5000);//타임아웃 설정 5초
            RestTemplate restTemplate = new RestTemplate(factory);

            // 고유번호 압축 파일 다운로드
            String url = "https://opendart.fss.or.kr/api/corpCode.xml";

            UriComponents uri = UriComponentsBuilder.fromHttpUrl(url+"?"+"crtfc_key=" + crtfcKey).build();

            Path file = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
                Path zipFile = Files.createTempFile("opendart-", ".zip");
                InputStream ins = response.getBody();
                byte[] bytes = IOUtils.toByteArray(ins);
                Files.write(zipFile, bytes);
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


            corpList = xmlParsing(dwnlFileNm);//xml 파싱하여 기업 고유번호 자져오기
        } catch (HttpClientErrorException  e) {

            System.out.println(e.toString());

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return corpList;
    }

    //XML 파싱하여 기업 고유번호 추출
    private static ArrayList<String> xmlParsing(String dwnlFileNm)  throws ParserConfigurationException, SAXException {

        ArrayList<String> arrCorp = new ArrayList<String>();

        try {

            String stockCd = ""; //거래소코드



            File file = new File(filePathNm + dailyFilePathNm + dwnlFileNm);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            document.getDocumentElement().normalize();

            NodeList nList = document.getElementsByTagName("list");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    stockCd =  eElement.getElementsByTagName("stock_code").item(0).getTextContent();
                    stockCd = stockCd.trim();
                    //상장회사이면 기업 고유번호 갖고오기
                    if(!"".equals(stockCd)) {
                        arrCorp.add(eElement.getElementsByTagName("corp_code").item(0).getTextContent());
                    }

                }
            }


        }
        catch(IOException e) {
            System.out.println(e);
        }

        return arrCorp;
    }
}
