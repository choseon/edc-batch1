package kcs.edc.batch.jobs.uct.uct001m;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.uct.uct001m.vo.Uct001mVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * UN Comtrade HS6 단위코드 기준의 각국 수출데이터 수집
 */
@Slf4j
@StepScope
public class Uct001mTasklet extends CmmnJob implements Tasklet {

    @Autowired
    protected FileProperty fileProperty;

    @Value("#{stepExecutionContext[threadNum]}")
    protected String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    protected List<String> partitionList;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart(threadNum, partitionList.size());

        this.apiService.setRestTemplate(getRestTemplate());
        List<String> areaList = getAreaList();

        for (String r : this.partitionList) { // 신고국가

            for (String p : areaList) { // 파트너국가

                if (r.equals(p)) continue;

                log.info("threadNum {}, r {}, p {}, ps", threadNum, r, p, "2015");

                UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                builder.replaceQueryParam("r", r);
                builder.replaceQueryParam("p", p);
                builder.replaceQueryParam("ps", "2015");
                URI uri = builder.build().encode().toUri();

                Uct001mVO resultVO = null;
                try {
                    resultVO = this.apiService.sendApiForEntity(uri, Uct001mVO.class);

                    if (Objects.isNull(resultVO)) return RepeatStatus.FINISHED;
                    if (resultVO.getValidation() == null) continue;
                    if (!"Ok".equals(resultVO.getValidation().getStatus().get("name"))) continue;
                    if ("0".equals(resultVO.getValidation().getCount().get("value"))) continue;

                    List<Uct001mVO.Item> dataset = resultVO.getDataset();
                    for (Uct001mVO.Item item : dataset) {
                        item.setCletDt(this.cletDt);
                        this.resultList.add(item);
                    }

                } catch (Exception e) {
                    if (!e.getMessage().contains("500")) {
                        log.info(e.getMessage());
                    }
                }
            }

            if (this.resultList.size() > 0) {
                this.fileService.makeTempFile(this.resultList);
                this.resultList = new ArrayList<>();
            }
        }

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    /**
     * http 호출을 위한 RestTemplate 기본값 셋팅하여 return
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
     * 리소스 파일을 읽어 국가목록 조회
     * @return
     */
    private List<String> getAreaList() {
        //        String resourcePath = "C:/dev/edc-batch/resources/";
//        String resourcePath = fileProperty.getResourcePath();
        String resourcePath = this.fileService.getResourcePath();
        String filePath = resourcePath + JobConstant.RESOURCE_FILE_NAME_UCT_AREA;

        List<String> pList = new ArrayList<>();
        // ISO 3166-1 국가 리스트
        JsonArray jsonArray = null;
        try {
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
