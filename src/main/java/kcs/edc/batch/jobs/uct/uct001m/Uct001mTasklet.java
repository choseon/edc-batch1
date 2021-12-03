package kcs.edc.batch.jobs.uct.uct001m;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.uct.uct001m.vo.Uct001mVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
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

    private List<String> baseYearList = new ArrayList<>();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart(this.threadNum, this.partitionList.size());

        if (ObjectUtils.isEmpty(this.baseDt)) {
            this.baseDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        // apiService에  Custom RestTemplate Setting
        this.apiService.setRestTemplate(getRestTemplate());

        // 신고국가 목록
        List<String> rList = ObjectUtils.isEmpty(this.partitionList) ? getAreaList() : this.partitionList;

        // 파트너국가 목록
        List<String> pList = getAreaList();

        // 2년치 데이터 돌린 후 exceptionCnt 체크하여 반복
        // 이미 생성된 파일은 체크하여 바로 넘어기 때문에 생성안된 파일만 다시 api 호출하여 파일생성하고 끝난다.
        int resultCnt = 0;
        do {
//            for (int i = 0; i < this.period; i++) {
//                String year = String.valueOf(Integer.parseInt(this.baseYear) - i);
//                this.baseYearList.add(year);
            resultCnt = callApi(rList, pList, this.baseYear);
//            }
        } while (resultCnt != 0);


        this.writeCmmnLogEnd(this.threadNum, this.partitionList.size());

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
    private int callApi(List<String> reportList, List<String> partnerList, String ps) {
        int resultCnt = 0;
        for (String r : reportList) { // 신고국가
            for (String p : partnerList) { // 파트너국가

                if (r.equals(p)) continue;

                String suffixFileName = ps + "_" + r + "_" + p;
                boolean tempFileExsists = this.fileService.isTempFileExsists(suffixFileName);
                if (tempFileExsists) {
                    log.info("tempFileExsists: {}", suffixFileName);
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
                                throw new IllegalAccessException("result is '0'");
                            }

                            item.setCletFileCrtnDt(this.baseDt);
                            this.resultList.add(item);
                        }

                        // temp파일 생성 후 리스트 초기화
                        this.fileService.makeTempFile(this.resultList, suffixFileName);
                        this.resultList.clear();

                        break; // 무한루프를 종료

                    } catch (Exception e) {

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
    public List<String> getAreaList() {

        List<String> pList = new ArrayList<>();
        JsonArray jsonArray = null;
        try {

            String resourcePath = this.fileService.getResourcePath();
            String filePath = resourcePath + CmmnConst.RESOURCE_FILE_NAME_UCT_AREA;
            jsonArray = FileUtil.readJsonFile(filePath, "results");

            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String id = jsonObject.get("id").getAsString();
                if (id.equals("all")) continue;
                pList.add(id);
            }

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        }

        return pList;
    }
}
