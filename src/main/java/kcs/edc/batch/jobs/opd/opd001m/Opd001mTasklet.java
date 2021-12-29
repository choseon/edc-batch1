package kcs.edc.batch.jobs.opd.opd001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.opd.opd001m.vo.Opd001mVO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 금융감독원 OpenDart 기업개황정보 데이터수집 Tasklet
 */
@Slf4j
@StepScope
public class Opd001mTasklet extends CmmnJob implements Tasklet {

    private String crtfcKey;

    private List<String> companyCodeList;

    @Value("${opd.callApiDelayTime}")
    private int callApiDelayTime;

    @Value("${opd.corpCodeUrl}")
    private String corpCodeUrl;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);
        this.crtfcKey = this.apiService.getJobPropHeader(this.jobGroupId, "crtfcKey");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("companyCodeList", this.companyCodeList);
        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        try {
            this.companyCodeList = getCompanyCodeList();
            if(ObjectUtils.isEmpty(this.companyCodeList)) return null;

            for (String companyCode : this.companyCodeList) {

                UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                builder.replaceQueryParam("crtfc_key", this.crtfcKey);
                builder.replaceQueryParam("corp_code", companyCode);
                URI uri = builder.build().toUri();

                Thread.sleep(this.callApiDelayTime); //10분에 1000건을 넘지 않기 위한 지연
                Opd001mVO resultVO = this.apiService.sendApiForEntity(uri, Opd001mVO.class);
                resultVO.setCletFileCrtnDt(DateUtil.getCurrentDate());
                this.resultList.add(resultVO);

                log.info("[{}/{}] corpCode: {}, corpName: {}",
                        this.itemCnt++, this.companyCodeList.size(), resultVO.getCorp_code(), resultVO.getCorp_name());
            }

            // 파일생성
            this.fileService.makeFile(this.resultList);

        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } catch (ParserConfigurationException e) {
            this.makeErrorLog(e.getMessage());
        } catch (ParseException e) {
            this.makeErrorLog(e.getMessage());
        } catch (SAXException e) {
            this.makeErrorLog(e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
        } catch (InterruptedException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 고유번호 압축 파일 다운로드 URL을 호출하여 기업고유번호 조회
     *
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private List<String> getCompanyCodeList() throws IOException, ParserConfigurationException, ParseException, SAXException {

        // 고유번호 압축 파일 다운로드 URL
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().fromHttpUrl(this.corpCodeUrl);
        builder.queryParam("crtfc_key", this.crtfcKey);
        URI uri = builder.build().toUri();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); //타임아웃 설정 5초
        factory.setReadTimeout(5000);//타임아웃 설정 5초
        RestTemplate restTemplate = new RestTemplate(factory);

        // api 호출하여 압축파일 다운로드
        Path tempFile = restTemplate.execute(uri.toString(), HttpMethod.GET, null, response -> {
            Path zipFile = Files.createTempFile("opendart-", ".zip");
            InputStream ins = response.getBody();
            byte[] bytes = IOUtils.toByteArray(ins);
            Files.write(zipFile, bytes);
            return zipFile;
        });

        List<String> resultList = null;
        // 압축 해제
        String downloadFilePath = null;
        byte[] buf = Files.readAllBytes(tempFile);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(buf));

        // 압축파일 copy
        String dataTempPath = this.fileService.getTempPath();
        ZipEntry zipEntry = null;

        zipEntry = zipInputStream.getNextEntry();

        if (zipEntry != null) {
            File dir = new File(dataTempPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.info("폴더 생성 실패", dir.getPath());
                    return null;
                }
            }
            downloadFilePath = dataTempPath + zipEntry.getName(); //압축 해제하여 생성한 파일명
            File downloadFile = new File(downloadFilePath);
            if (downloadFile.exists()) { // 파일이 존재하면 삭제
                Files.delete(downloadFile.toPath());
            }
            Files.copy(zipInputStream, Paths.get(downloadFilePath)); // 파일 복사
        }
        zipInputStream.closeEntry();
        zipInputStream.close();

        // xml parsing
        if (!ObjectUtils.isEmpty(downloadFilePath)) {
            resultList = xmlParsing(downloadFilePath);
        }

        // 압축파일 삭제
        Files.delete(tempFile);

        // 다운로드 Temp파일 삭제
        this.fileService.cleanTempFile();

        return resultList;

    }

    /**
     * XML 파싱하여 기업 고유번호 추출
     *
     * @param xmlFilePath
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private List<String> xmlParsing(String xmlFilePath) throws ParserConfigurationException, IOException, SAXException, ParseException {
        log.info("dwnlFileNm >> {}", xmlFilePath);

        String lastModifyDt = this.baseDt; //이전 배치를 실행한 날짜
        String modifyDt = ""; //개황정보 수정일자

        List<String> resultList = new ArrayList<>();

        File xmlFile = new File(xmlFilePath);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // XML DOCTYPE 선언 비활성화
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList nList = document.getElementsByTagName("list");

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;

            Element eElement = (Element) nNode;
            //고유번호 가져오기
            modifyDt = eElement.getElementsByTagName("modify_date").item(0).getTextContent();

            //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
            if (getNewDataYN(lastModifyDt, modifyDt)) {
                resultList.add(eElement.getElementsByTagName("corp_code").item(0).getTextContent());
            }
        }

        return resultList;
    }

    //이전 배치 실행일자 이후에 개황정보 수정한 자료인지 여부
    private boolean getNewDataYN(String lastModifyDt, String modifyDt) throws ParseException {
        boolean returnValue = false;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -7);
        String strYesterDay = dateFormat.format(calendar.getTime());

        Date yesterDay = dateFormat.parse(strYesterDay); //어제날짜
        Date lastModifyDate = dateFormat.parse(lastModifyDt);
        Date modifyDate = dateFormat.parse(modifyDt);

        //개황정보 수정일이 최종 배치 수행일자와 어제날짜 사이에 있으면 True
        if ((modifyDate.equals(lastModifyDate) || modifyDate.before(lastModifyDate)) && (modifyDate.equals(yesterDay) || modifyDate.after(yesterDay))) {
            returnValue = true;
        }
        return returnValue;
    }
}
