package kcs.edc.batch.cmmn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.edc.batch.cmmn.property.ApiProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ApiService {

    @Autowired
    private ApiProperty apiProperty; // OpenApi information (all)

    private ApiProperty.JobProp jobProp; // OpenApi information (job)

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    public void setJobId (String jobId) {
        this.jobProp = this.apiProperty.getJobProp(jobId);
    }

    /**
     * UriComponentsBuilder 생성
     * @return
     */
    public UriComponentsBuilder getUriComponetsBuilder() {

        // property loading
//        jobProp = apiProperty.getJobProp(getCurrentJobId());

        // baseUrl setting
        String baseUrl = this.jobProp.getBaseUrl();

        // parameter setting
        MultiValueMap<String, String> param = this.jobProp.getParam();

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().fromHttpUrl(baseUrl).queryParams(param);

        return builder;
    }

    /**
     * Send Open Api
     * @param uri
     * @param targetClass
     * @param <T>
     * @return
     */
    public <T> T sendApiForEntity(URI uri, Class<T> targetClass) {

        log.debug("uri {}", uri);

        ResponseEntity<String> forEntity = this.restTemplate.getForEntity(uri, String.class);
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
            t = this.objectMapper.readValue(resultJson, targetClass);
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
        }
        return t;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> getjobPropParam(String key) {
        return this.jobProp.getParam().get(key);
    }
}
