package kcs.edc.batch.cmmn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.edc.batch.cmmn.property.ApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ApiService {

    @Autowired
    private ApiProperties apiProperties; // ApiProperties를 자동주입

    private ApiProperties.ApiProp apiProp; // Job ApiProperty

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 초기화
     * jobId를 넘겨받아 자동으로 주입받은 ApiProperties 전체목록에서 해당 jobID 정보를 조회한다.
     *
     * @param jobId 배치ID
     */
    public void init(String jobId) {

        this.apiProp = this.apiProperties.getApiProp(jobId);
        log.debug("ApiService init() >> jobId: {}", jobId);
    }

    /**
     * UriComponentsBuilder 생성
     * apiProp에 담고있는 baseUrl과 parameter를 셋팅하여 builder를 리턴한다.
     *
     * @return UriComponentsBuilder
     */
    public UriComponentsBuilder getUriComponetsBuilder() {

        // baseUrl setting
        String baseUrl = this.apiProp.getBaseUrl();

        // parameter setting
        MultiValueMap<String, String> param = this.apiProp.getParam();

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().fromHttpUrl(baseUrl).queryParams(param);

        return builder;
    }

    /**
     * Send Open Api
     * RestTemplate의 getForEntity()를 이용하여 호출한 uri의 결과를 String 타입으로 리턴받는다.
     * 리턴받은 String 결과를 ObjectMapper를 이용하여 responseType으로 리턴한다.
     *
     * @param uri         OpenApi URI
     * @param resonseType 리턴받을 Class Type
     * @param <T>
     * @return
     * @throws JsonProcessingException
     * @throws RestClientException
     */
    public <T> T sendApiForEntity(URI uri, Class<T> resonseType) throws JsonProcessingException, RestClientException {

        ResponseEntity<String> forEntity = this.restTemplate.getForEntity(uri, String.class);
        String resultJson = forEntity.getBody();

        if (Objects.isNull(resultJson)) {
            log.info("uri {}", uri);
            log.info("resultJson is null");
            return null;
        } else {
            log.debug("uri {}", uri);
            log.debug("resultJson {}", resultJson);
        }
        this.objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return this.objectMapper.readValue(resultJson, resonseType);
    }

    /**
     * RestTemplate의 postForObject()를 이용하여 호출한 uri의 결과를 String 타입으로 리턴받는다.
     * 리턴받은 String 결과를 ObjectMapper를 이용하여 responseType으로 리턴한다.
     *
     * @param uri         OpenApi URI
     * @param request     Send request Option
     * @param targetClass 리턴받을 Class Type
     * @param <T>
     * @return
     * @throws JsonProcessingException
     * @throws RestClientException
     */
    public <T> T sendApiPostForObject(URI uri, Object request, Class<T> targetClass) throws JsonProcessingException, RestClientException {

        String resultJson = this.restTemplate.postForObject(uri, request, String.class);

        if (Objects.isNull(resultJson)) {
            log.info("uri {}", uri);
            log.info("resultJson is null");
            return null;
        } else {
            log.debug("uri {}", uri);
            log.debug("resultJson {}", resultJson);
        }
        return this.objectMapper.readValue(resultJson, targetClass);

    }

    /**
     * RestTemplate의 exchange()를 이용하여 호출한 uri의 결과를 String 타입으로 리턴받는다.
     * 리턴받은 String 결과를 ObjectMapper를 이용하여 responseType으로 리턴한다.
     *
     * @param uri         OpenApi URI
     * @param httpMethod  Send httpMethod Option
     * @param entity      Send entity Option
     * @param targetClass 리턴받을 Class Type
     * @param <T>
     * @return
     * @throws JsonProcessingException
     */
    public <T> T sendApiExchange(URI uri, HttpMethod httpMethod, HttpEntity<String> entity, Class<T> targetClass) throws JsonProcessingException {

        ResponseEntity<String> exchange = this.restTemplate.exchange(uri, httpMethod, entity, String.class);
        String resultJson = exchange.getBody();

        if (Objects.isNull(resultJson)) {
            log.info("uri {}", uri);
            log.info("resultJson is null");
            return null;
        } else {
            log.debug("uri {}", uri);
            log.debug("resultJson {}", resultJson);
        }

        return this.objectMapper.readValue(resultJson, targetClass);
    }

    /**
     * RestTemplate setting
     *
     * @param restTemplate
     */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * API parameter값 조회
     *
     * @param key 조회할 파라미터명
     * @return
     */
    public List<String> getJobPropParam(String key) {
        return this.apiProp.getParam().get(key);
    }

    /**
     * API header값 조회
     *
     * @param key
     * @return
     */
    public String getJobPropHeader(String jobGrpName, String key) {
        ApiProperties.ApiProp apiProp = this.apiProperties.getApiProp(jobGrpName);
        return apiProp.getHeader().get(key);
    }
}
