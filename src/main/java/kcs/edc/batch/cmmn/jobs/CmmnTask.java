package kcs.edc.batch.cmmn.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.edc.batch.cmmn.property.ApiProperties;
import kcs.edc.batch.cmmn.property.FileProperties;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.FileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.util.*;


@Slf4j
@StepScope
public class CmmnTask {

    @Autowired
    protected ApiProperties apiProperties; // OpenApi information (all)
    protected ApiProperties.JobProp jobProp; // OpenApi information (job)

    @Autowired
    protected FileProperties fileProperties;

    @Value("${path.storePath}")
    protected String storePath;

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집일자
    protected String from;
    protected String until;

    private String startTime = DateUtil.getCurrentTime();
    private String endTime;

    protected URI uri;
    protected RestTemplate restTemplate = new RestTemplate();
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<Object> resultList = new ArrayList<>();

    protected ExecutionContext jobExecutionContext;

    protected String accessKey;

    @Autowired
    protected FileService fileService;

    /**
     * OpenApiProp에서 url과 parameter를 조회하여 url 생성
     * 추후 동적 parameter를 추가하기 위해 UriComponentBuilder 타입으로 반환
     * url 사용시 builder.toUriString()이나 builder.toString()으로 사용
     *
     * @return uriComponentBuilder
     */
    public UriComponentsBuilder getUriComponetsBuilder() {

        // property loading
        jobProp = apiProperties.getJobProp(getCurrentJobId());

        // baseUrl setting
        String baseUrl = jobProp.getBaseUrl();

        // parameter setting
        MultiValueMap<String, String> param = jobProp.getParam();

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().fromHttpUrl(baseUrl).queryParams(param);

        return builder;
    }

    public <T> T sendApiForEntity(URI uri, Class<T> targetClass) {

        log.debug("uri {}", uri);

        ResponseEntity<String> forEntity = restTemplate.getForEntity(uri, String.class);
        String resultJson = forEntity.getBody();

        if (Objects.isNull(resultJson)) {
            log.info("uri {}", uri);
            log.info("resultJson is null");
            return null;
        } else {
            log.debug("resultJson {}", resultJson);
        }

        T t = null;

        try {
            t = objectMapper.readValue(resultJson, targetClass);
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
        }
        return t;
    }

    /**
     * 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
     *
     * @return grpName 배치그룹명
     */
    protected String getJobGrpName() {

        String className = this.getClass().getSimpleName();

        // 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
        String grpName = className.substring(0, 3).toLowerCase();

        return grpName;
    }

    /**
     * ClassName에서 JobId 추출하여 return ex) Nav001mTasklet -> nav001m
     *
     * @return jobId 배치잡ID
     */
    protected String getCurrentJobId() {

        String className = this.getClass().getSimpleName();
        String jobId = null;
        if (className.contains("Tasklet")) {
            jobId = className.replace("Tasklet", "").toLowerCase();
        } else {
            jobId = className;
        }
        return jobId;
    }

    protected FileVO getFileVO(String jobId) {
        return new FileVO(storePath, jobId, baseDt);
    }

    protected <T> void makeFile(String jobId, List<T> list) {
        FileVO fileVO = getFileVO(jobId);
        if (list.size() == 0) {
            log.info("[{}] Datafile file make start", fileVO.getTableName());
            log.info("skip.... no data");
            makeLogFile(fileVO, list);
            return;
        }
        makeDataFile(fileVO, list);
        makeLogFile(fileVO, list);
    }

    protected <T> void makeTempFile(String jobId, List<T> list) {

        FileVO fileVO = getFileVO(jobId);
        log.info("[{}] Temp file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

        if (list.size() == 0) {
            log.info("list is null");
            return;
        }


            String filePath = fileVO.getTempFilePath();
            String fileName = fileVO.getTempFileName();

            // temp 파일이 존재하면 모두 삭제
//            FileUtil.deleteFiles(filePath);

            FileUtil.makeFile(filePath, fileName, list);

            log.info("[{}] completed :  {} ", fileVO.getTableName(), fileVO.getTempFileFullPath());

    }

    private <T> void makeDataFile(FileVO fileVO, List<T> list) {

        log.info("[{}] Data file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

            String dataFilePath = fileVO.getDataFilePath();
            String dataFileName = fileVO.getDataFileName();
            FileUtil.makeFile(dataFilePath, dataFileName, list);

            log.info("[{}] completed : {}", fileVO.getTableName(), fileVO.getDataFileFullPath());

    }

    private <T> void makeLogFile(FileVO fileVO, List<T> list) {

        log.info("[{}] Log file make start", fileVO.getTableName());

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(baseDt);
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Success");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);


            // 로그 파일 생성
            String logFilePath = fileVO.getLogFilePath();
            String logFileName = fileVO.getLogFileName();
            FileUtil.makeFile(logFilePath, logFileName, arrayList);

            log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFileFullPath());

    }

    public void writeCmmnLogStart() {
        log.info("####################################################");
        log.info("START JOB :::: {}", getCurrentJobId());
        log.info("####################################################");
        log.info("baseDt : {}", baseDt);
//        log.info("##### START JOB :::: {} ########################################", getJobId());
    }

    public void writeCmmnLogStart(String threadNum, int listSize) {
        log.info("####################################################");
        log.info("threadNum : {}, baseDt : {}, list size : {}", threadNum, baseDt, listSize);
    }

    public void writeCmmnLogEnd() {
        log.info("####################################################");
        log.info("END JOB :::: {}", getCurrentJobId());
        log.info("####################################################");
    }
}
