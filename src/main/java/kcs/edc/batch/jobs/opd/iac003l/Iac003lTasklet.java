package kcs.edc.batch.jobs.opd.iac003l;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.jobs.opd.iac003l.vo.Iac003lVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 금융감독원 OpenDart 기업개황정보 데이터수집 Tasklet
 */
@Slf4j
@StepScope
public class Iac003lTasklet extends CmmnJob implements Tasklet {

    private String crtfcKey;
    private String rootPath;
    private String dailyFilePath;
    private static String strSep = "\\"; // 디렉토리를 구분하는 문자 (윈도우 \\ , 유닉스or리눅스 /)

    private List<String> companyCodeList;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);
        this.crtfcKey = this.apiService.getJobPropHeader(getJobGroupId(), "crtfcKey");
        this.rootPath = this.fileService.getRootPath();
        this.dailyFilePath = makeDirectory(this.fileService.getRootPath());

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("companyCodeList", this.companyCodeList);
        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        this.companyCodeList = getCompanyCodeList();
        log.info("CompanyCodeList.size(): {}", this.companyCodeList.size());

        for (String companyCode : this.companyCodeList) {

            UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
            builder.replaceQueryParam("crtfc_key", this.crtfcKey);
            builder.replaceQueryParam("corp_code", companyCode);
            URI uri = builder.build().toUri();

            Thread.sleep(50);

            Iac003lVO resultVO = this.apiService.sendApiForEntity(uri, Iac003lVO.class);
            log.info("resultVO: {}", resultVO);
            this.resultList.add(resultVO);

            if(this.resultList.size() == 10) break; // test
        }

        // 파일생성
        this.fileService.makeFile(this.resultList);

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    private String makeDirectory(String rootPath) {

        SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
        SimpleDateFormat MM = new SimpleDateFormat("MM");
        SimpleDateFormat dd = new SimpleDateFormat("dd");

        Calendar calendar = Calendar.getInstance();

        Date dateObj = calendar.getTime();
        String strYYYY = yyyy.format(dateObj);
        String strMM = MM.format(dateObj);
        String strDD = dd.format(dateObj);

        File directory = new File(rootPath + strYYYY + strSep + strMM + strSep + strDD + strSep + "corp");

        if (!directory.exists()) {
            directory.mkdirs();
        } else {

            File parentFile = directory.getParentFile();
            log.info("parentFile: {}", parentFile);
            parentFile.delete();
        }

        return directory.getPath() + this.strSep;
    }


    private List<String> getCompanyCodeList() throws IOException, ParserConfigurationException, SAXException {

        String url = "https://opendart.fss.or.kr/api/corpCode.xml";
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().fromHttpUrl(url);
        builder.queryParam("crtfc_key", this.crtfcKey);
        URI uri = builder.build().toUri();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); //타임아웃 설정 5초
        factory.setReadTimeout(5000);//타임아웃 설정 5초
        RestTemplate restTemplate = new RestTemplate(factory);

        Path file = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
            Path zipFile = Files.createTempFile("opendart-", ".zip");
            InputStream ins = response.getBody();
            byte[] bytes = IOUtils.toByteArray(ins);
            Files.write(zipFile, bytes);
            return zipFile;
        });

        // 압축 해제하여 xml파일 생성
        String downloadFileName = "";
        Path zipFile = file;
        byte[] buf = Files.readAllBytes(zipFile);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(buf));
        ZipEntry zipEntry = null;

        if ((zipEntry = zipInputStream.getNextEntry()) != null) {
            downloadFileName = this.dailyFilePath + zipEntry.getName(); //압축 해제하여 생성한 파일명
            Files.copy(zipInputStream, Paths.get(downloadFileName));
        }
        zipInputStream.closeEntry();
        zipInputStream.close();

        return xmlParsing(downloadFileName);

    }

    //XML 파싱하여 기업 고유번호 추출
    private List<String> xmlParsing(String dwnlFileNm) throws ParserConfigurationException, SAXException, IOException {

        String lastModifyDt = getLastModifyDt(); //이전 배치를 실행한 날짜
        String modifyDt = ""; //개황정보 수정일자
        String stockCd = ""; //거래소코드

        List<String> resultList = new ArrayList<>();

        File file = new File(dwnlFileNm);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(file);
        document.getDocumentElement().normalize();

        NodeList nList = document.getElementsByTagName("list");

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                //고유번호 가져오기
                modifyDt = eElement.getElementsByTagName("modify_date").item(0).getTextContent();
                stockCd = eElement.getElementsByTagName("stock_code").item(0).getTextContent();
                stockCd = stockCd.trim();

                //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
                if (getNewDataYN(lastModifyDt, modifyDt)) {
                    if (!"".equals(stockCd)) {
                        resultList.add(eElement.getElementsByTagName("corp_code").item(0).getTextContent());
                    }
                }
            }
        }
        return resultList;
    }

    //이전 배치 실행일자 이후에 개황정보 수정한 자료인지 여부
    private boolean getNewDataYN(String lastModifyDt, String modifyDt) {
        boolean returnValue = false;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            String strYesterDay = dateFormat.format(calendar.getTime());


            Date yesterDay = dateFormat.parse(strYesterDay); //어제날짜
            Date lastModifyDate = dateFormat.parse(lastModifyDt);
            Date modifyDate = dateFormat.parse(modifyDt);

            //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
            if ((modifyDate.equals(lastModifyDate) || modifyDate.after(lastModifyDate)) && (modifyDate.equals(yesterDay) || modifyDate.before(yesterDay))) {
                returnValue = true;
            }
        } catch (ParseException ex) {
        }
        return returnValue;
    }

    //시작일자 가져오기  (이전 배치 실행시 적용한 종료일 항목값)
    private String getLastModifyDt() {

        File file = new File(this.rootPath + "corpLastModifyDate.txt");
        String returnDate = "";

        try {
            if (file.exists()) {

                BufferedReader inFile = new BufferedReader(new FileReader(file));
                String sLine = null;

                if ((sLine = inFile.readLine()) != null)
                    returnDate = sLine;

                inFile.close();

            } else {
                returnDate = "19000101";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnDate;
    }


}
