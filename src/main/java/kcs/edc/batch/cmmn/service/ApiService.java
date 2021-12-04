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
    private ApiProperties apiProperties; // OpenApi information (all)

    private ApiProperties.ApiProp apiProp; // OpenApi information (job)

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    public void init(String jobId) {

        this.apiProp = this.apiProperties.getApiProp(jobId);

        if (Objects.isNull(this.apiProp)) {
            throw new NullPointerException("apiProperty is null");
        }
        log.debug("ApiService init() >> jobId: {}", jobId);
    }

    /**
     * UriComponentsBuilder 생성
     *
     * @return
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
     *
     * @param uri
     * @param resonseType
     * @param <T>
     * @return
     */
    public <T> T sendApiForEntity(URI uri, Class<T> resonseType)  throws JsonProcessingException {

//        try {
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

//        } catch (JsonProcessingException | RestClientException e) {
//            log.info(e.getMessage());
//        }
//        return null;
    }

    /**
     * @param uri
     * @param request
     * @param targetClass
     * @param <T>
     * @return
     */
    public <T> T sendApiPostForObject(URI uri, Object request, Class<T> targetClass) throws JsonProcessingException, RestClientException {

//        try {
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

//        } catch (JsonProcessingException | RestClientException e) {
//            log.info(e.getMessage());
//        }
//        return null;
    }

    public <T> T sendApiExchange(URI uri, HttpMethod httpMethod, HttpEntity<String> entity, Class<T> targetClass) {


        try {
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
        } catch (JsonProcessingException | RestClientException e) {
            log.info(e.getMessage());
        }
        return null;
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
