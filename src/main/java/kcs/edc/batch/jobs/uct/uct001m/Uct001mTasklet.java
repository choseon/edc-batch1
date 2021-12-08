package kcs.edc.batch.jobs.uct.uct001m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.uct.uct001m.vo.Uct001mVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * UN Comtrade HS6 단위코드 기준의 각국 수출데이터 수집
 */
@Slf4j
public class Uct001mTasklet extends CmmnJob implements Tasklet {

    @Value("#{stepExecutionContext[threadNum]}")
    private String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    private List<String> partitionList;

    @Value("#{jobParameters[baseYear]}")
    private String baseYear;

    @Value("${uct.period}")
    private int period;

    @Value("${scheduler.uct.baseline}")
    private String baseline;

    int totalFileCnt;

    @Override
    public void beforeStep(StepExecution stepExecution) {


        super.beforeStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        if (ObjectUtils.isEmpty(this.threadNum)) {
            this.writeCmmnLogStart();
        } else {
            this.writeCmmnLogStart(this.threadNum, this.partitionList.size());
        }


        try {

            if (ObjectUtils.isEmpty(this.baseDt)) {
                this.baseDt = DateUtil.getCurrentDate("yyyyMM");
            }

            if (!ObjectUtils.isEmpty(this.baseYear)) {
                saveLastModified("", "");
            } else {

//                this.baseYear = DateUtil.getBaseLineDate("Y-1");
//
//                Properties prop = getLastModified();
//                String propBaseYear = (String) prop.get("baseYear");
//                String propBaseDt = (String) prop.get("baseDt");
//                log.info("propBaseYear: {}, propBaseDt: {}", propBaseYear, propBaseDt);
//
//                if (ObjectUtils.isEmpty(propBaseDt) && ObjectUtils.isEmpty(propBaseYear)) {
//                    this.baseYear = DateUtil.getBaseLineDate("Y-1");
//                    saveLastModified(this.baseDt, this.baseYear);
//                } else {
//                    this.baseYear = DateUtil.getBaseLineDate("Y-2");
//                }
//                if(this.baseDt.equals(propBaseDt) && !this.baseYear.equals(propBaseYear)) {
//                    this.baseYear = propBaseYear;
//                }
            }
            log.info("baseYear: {}", this.baseYear);

            // apiService에  Custom RestTemplate Setting
            this.apiService.setRestTemplate(getRestTemplate());

            // 신고국가 목록
            List<String> rList = ObjectUtils.isEmpty(this.partitionList) ? getAreaList() : this.partitionList;
            // 파트너국가 목록
            List<String> pList = getAreaList();
            // 생성되어야할 총파일갯수
            this.totalFileCnt = rList.size() * (pList.size() - 1);

            int resultCnt = 0;
            do {
                resultCnt = callApi(rList, pList, this.baseYear);
            } while (resultCnt > 0);

            if (resultCnt == 0) {

                // 파일생성전 최종검증차원에서 한번 더 돌린다
                // 이미 생성된 파일은 바로 넘어가기 때문에 정상적으로 파일이 생성된 상태라면 짧은시간 소요됨.
                callApi(rList, pList, this.baseYear);

                // 생성되어야할 총파일갯수 생성된 임시파일갯수 비교교
                int tempFileCnt = this.fileService.getTempFileCnt();
                if (this.totalFileCnt == tempFileCnt) {
                    // 파일병합 및 로그파일생성
                    this.fileService.mergeTempFile(this.jobId, this.baseYear);
                    // 최종수정일 prop update
                    saveLastModified(this.baseDt, this.baseYear);
                }
            }

        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            if (ObjectUtils.isEmpty(this.threadNum)) {
                this.writeCmmnLogEnd();
            } else {
                this.writeCmmnLogEnd(this.threadNum, this.partitionList.size());
            }
        }


        return RepeatStatus.FINISHED;
    }


    /**
     * API 호출
     *
     * @param reportList  신고국가
     * @param partnerList 파트너국가
     * @param ps          년도
     * @return
     */
    private int callApi(List<String> reportList, List<String> partnerList, String ps) throws FileNotFoundException, IllegalAccessException, JsonProcessingException {

        int index = 0;
        int resultCnt = 0;
        for (String r : reportList) { // 신고국가
            for (String p : partnerList) { // 파트너국가

                if (r.equals(p)) continue;

                if (++index % 1000 == 0) {
                    log.info("[{}/{}]", index, this.totalFileCnt);
                }

                String suffixFileName = ps + "_" + r + "_" + p;
                boolean tempFileExsists = this.fileService.isTempFileExsists(suffixFileName);
                if (tempFileExsists) {
                    log.info("[{}/{}] tempFileExsists: {}", index, this.totalFileCnt, suffixFileName);
                    continue;
                }

                int exceptionCnt = 0;
                while (true) { // exception이 많이 발생하기 때문에 exception이 발생한 경우 결과 나올때까지 무한루프 돌린다.

                    try {

                        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                        builder.replaceQueryParam("r", r);
                        builder.replaceQueryParam("p", p);
                        builder.replaceQueryParam("ps", ps);
                        URI uri = builder.build().encode().toUri();

                        Uct001mVO resultVO = this.apiService.sendApiForEntity(uri, Uct001mVO.class);

                        if (Objects.isNull(resultVO)) continue;
                        if (resultVO.getValidation() == null) continue;
                        if (!"Ok".equals(resultVO.getValidation().getStatus().get("name"))) continue;
                        if ("0".equals(resultVO.getValidation().getCount().get("value"))) continue;

                        List<Uct001mVO.Item> dataset = resultVO.getDataset();
                        for (Uct001mVO.Item item : dataset) {

                            // 결과값 체크
                            if (item.getYr().equals("0") || item.getRtCode().equals("0") ||
                                    item.getPtTitle().equals("0") || item.getPtCode().equals("0")) {
                                throw new RestClientException("result is '0'");
                            }

                            item.setCletFileCrtnDt(this.baseDt);
                            this.resultList.add(item);
                        }

                        // temp파일 생성 후 리스트 초기화
                        this.fileService.makeTempFile(this.resultList, suffixFileName);
                        this.resultList.clear();

                        break; // 무한루프 종료

                    } catch (RestClientException e) {

                        if (e.getMessage().contains("UnknownHostException")) {
                            this.makeErrorLog(e.getMessage());
                            return -1;
                        }
                        if (e.getMessage().contains("500")) {
                            log.debug("thread #{}, r {}, p {}, ps {} >> {}", this.threadNum, r, p, ps, e.getMessage());
                        } else {
                            log.info("thread #{}, r {}, p {}, ps {} >> {}", this.threadNum, r, p, ps, e.getMessage());
                            if (exceptionCnt++ >= 20) {
                                resultCnt++;
                                break;
                            }
                        }

                    }
                }
            }
        }
        return resultCnt;
    }

    /**
     * RestTemplate Custom settings
     *
     * @return
     */
    private RestTemplate getRestTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(20000);
        httpRequestFactory.setReadTimeout(600000);
        HttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(20)
                .build();
        httpRequestFactory.setHttpClient(httpClient);
        RestTemplate template = new RestTemplate(httpRequestFactory);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        template.getMessageConverters().add(converter);

        return template;
    }

    /**
     * 파트너국가 목록 조회
     * 리소스파일을 조회하여 ISO 3166-1 국가 리스트 반환
     *
     * @return
     */
    public List<String> getAreaList() throws FileNotFoundException {

        List<String> pList = new ArrayList<>();
        JsonArray jsonArray = null;

        String resourcePath = this.fileService.getResourcePath();
        String filePath = resourcePath + CmmnConst.RESOURCE_FILE_NAME_UCT_AREA;
        jsonArray = FileUtil.readJsonFile(filePath, "results");

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String id = jsonObject.get("id").getAsString();
            if (id.equals("all")) continue;
            pList.add(id);
        }

        return pList;
    }

    public void saveLastModified(String baseDt, String baseYear) throws IOException {

        String filePath = this.fileService.getResourcePath();
        String fileName = "uct_last_modified.txt";
        FileOutputStream stream = new FileOutputStream(filePath + fileName);

        Properties prop = new Properties();
        prop.setProperty("baseDt", baseDt);
        prop.setProperty("baseYear", baseYear);
        prop.setProperty("lastModified", DateUtil.getCurrentTime());

        prop.store(stream, "saveLastModified");
        stream.close();
    }

    public Properties getLastModified() throws IOException {
        String filePath = this.fileService.getResourcePath();
        String fileName = "uct_last_modified.txt";
        FileInputStream stream = new FileInputStream(filePath + fileName);

        Properties prop = new Properties();
        prop.load(stream);
        stream.close();
        return prop;
    }

}
