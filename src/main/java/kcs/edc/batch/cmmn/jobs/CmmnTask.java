package kcs.edc.batch.cmmn.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.lang.Nullable;
import kcs.edc.batch.cmmn.property.ApiProperty;
import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtils;
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
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@StepScope
public class CmmnTask {

    @Autowired
    protected ApiProperty apiProperty; // OpenApi information (all)
    protected ApiProperty.JobProp jobProp; // OpenApi information (job)

    @Autowired
    protected FileProperty fileProperty;

    @Value("${path.storePath}")
    protected String storePath;

    @Value("#{jobParameters[cletDt]}")
    protected String cletDt; // 수집일자
    protected String from;
    protected String until;

    private String startTime = DateUtils.getCurrentTime();
    private String endTime;

    protected URI uri;
    protected RestTemplate restTemplate = new RestTemplate();
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<Object> resultList = new ArrayList<>();

    protected ExecutionContext jobExecutionContext;

    protected String accessKey;

    /**
     * OpenApiProp에서 url과 parameter를 조회하여 url 생성
     * 추후 동적 parameter를 추가하기 위해 UriComponentBuilder 타입으로 반환
     * url 사용시 builder.toUriString()이나 builder.toString()으로 사용
     *
     * @return uriComponentBuilder
     */
    public UriComponentsBuilder getUriComponetsBuilder() {

        // property loading
        jobProp = apiProperty.getJobProp(getJobId());

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
    protected String getJobId() {

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
        return new FileVO(storePath, jobId, cletDt);
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

        try {

            String filePath = fileVO.getTempFilePath();
            String fileName = fileVO.getTempFileName();

            // temp 파일이 존재하면 모두 삭제
//            FileUtil.deleteFiles(filePath);

            FileUtil.makeFile(filePath, fileName, list);

            log.info("[{}] completed :  {} ", fileVO.getTableName(), fileVO.getTempFileFullPath());

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

    private <T> void makeDataFile(FileVO fileVO, List<T> list) {

        log.info("[{}] Data file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

        try {

            String dataFilePath = fileVO.getDataFilePath();
            String dataFileName = fileVO.getDataFileName();
            FileUtil.makeFile(dataFilePath, dataFileName, list);

            log.info("[{}] completed : {}", fileVO.getTableName(), fileVO.getDataFileFullPath());


        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

    private <T> void makeLogFile(FileVO fileVO, List<T> list) {

        log.info("[{}] Log file make start", fileVO.getTableName());

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(cletDt);
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(startTime);
        logVO.setEndTime(DateUtils.getCurrentTime());
        logVO.setJobStat("Success");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        try {

            // 로그 파일 생성
            String logFilePath = fileVO.getLogFilePath();
            String logFileName = fileVO.getLogFileName();
            FileUtil.makeFile(logFilePath, logFileName, arrayList);

            log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFileFullPath());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void writeCmmnLogStart() {
        log.info("####################################################");
        log.info("START JOB :::: {}", getJobId());
        log.info("####################################################");
        log.info("Collect Date : {}", cletDt);
//        log.info("##### START JOB :::: {} ########################################", getJobId());
    }

    public void writeCmmnLogStart(String threadNum, int listSize) {
        log.info("####################################################");
        log.info("threadNum : {}, cletDt : {}, list size : {}", threadNum, cletDt, listSize);
    }

    public void writeCmmnLogEnd() {
        log.info("####################################################");
        log.info("END JOB :::: {}", getJobId());
        log.info("####################################################");
    }

    /**
     * tsv 파일을 읽고 리스트에 담아 결과값 반환
     *
     * @param jobId
     * @return
     */
    protected List<Object[]> getMergeListFromTsvFile(String jobId) {
        FileVO fileVO = getFileVO(jobId);
        String tempFilePath = fileVO.getTempFilePath();
        List<Object[]> list = FileUtil.getListFromTsvFile(tempFilePath);
        return list;
    }

    protected void mergeFile(String jobId) {

        try {
            FileVO fileVO = getFileVO(jobId);
            String tempFilePath = fileVO.getTempFilePath();
            String dataFilePath = fileVO.getDataFilePath();
            String dataFileName = fileVO.getDataFileName();
            FileUtil.mergeFile(tempFilePath,dataFilePath,dataFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
