package kcs.edc.batch.jobs.uct.uct001m;

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
import kcs.edc.batch.cmmn.vo.HiveFileVO;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
import java.net.URI;
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


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart(this.threadNum, this.partitionList.size());

        // apiService에  Custom RestTemplate Setting
        this.apiService.setRestTemplate(getRestTemplate());

        // 국가목록 조회
        List<String> areaList = getAreaList();

        for (String r : this.partitionList) { // 신고국가

            for (String p : areaList) { // 파트너국가

                if (r.equals(p)) continue;

                while (true) {

                    String suffixFileName = this.baseYear + "_" + r + "_" + p;
                    boolean tempFileExsists = this.fileService.tempFileExsists(suffixFileName);
                    if (tempFileExsists) break;

                    try {
                        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                        builder.replaceQueryParam("r", r);
                        builder.replaceQueryParam("p", p);
                        builder.replaceQueryParam("ps", this.baseYear);
                        URI uri = builder.build().encode().toUri();

                        Uct001mVO resultVO = this.apiService.sendApiForEntity(uri, Uct001mVO.class);

                        if (Objects.isNull(resultVO)) break;
                        if (resultVO.getValidation() == null) break;
                        if (!"Ok".equals(resultVO.getValidation().getStatus().get("name"))) break;
                        if ("0".equals(resultVO.getValidation().getCount().get("value"))) break;

                        List<Uct001mVO.Item> dataset = resultVO.getDataset();
                        for (Uct001mVO.Item item : dataset) {
                            this.resultList = new ArrayList<>();

                            // 결과값 체크
                            if (item.getYr().equals("0") || item.getRtCode().equals("0") ||
                                    item.getPtTitle().equals("0") || item.getPtCode().equals("0")) {
                                throw new Exception("result is '0'");
                            }

                            item.setCletFileCtrnDt(DateUtil.getCurrentDate());
                            this.resultList.add(item);
                        }

                        this.fileService.makeTempFile(this.resultList, suffixFileName);


                    } catch (Exception e) {

//                        log.info("thread #{}, r {}, p {}, ps {} >> {}", this.threadNum, r, p, this.baseYear, e.getMessage());

//                        if(e.getMessage().contains("409")) {
//                            e.printStackTrace();
//                        }

                        if (!e.getMessage().contains("500")) {
                            log.info("thread #{}, r {}, p {}, ps {} >> {}", this.threadNum, r, p, this.baseYear, e.getMessage());
                        } else {
                            log.debug("thread #{}, r {}, p {}, ps {} >> {}", this.threadNum, r, p, this.baseYear, e.getMessage());
                        }
                    }
                    break;
                }
            }
        }

        this.writeCmmnLogEnd(this.threadNum);

        return RepeatStatus.FINISHED;
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
            return null;
        }

        return pList;
    }
}
