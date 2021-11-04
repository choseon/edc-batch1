package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 데이터수집을 하기 위한 Open API 정보를 담는 클래스
 * application.yml의 "api" 그룹을 자동으로 매핑하여 Configuration Bean으로 등록한다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    /**
     * Open API  jobs map info
     */
    private Map<String, ApiProp> jobs = new HashMap<>();

    @Getter
    @Setter
    public static class ApiProp {

        /**
         * Open API url
         */
        private String baseUrl;

        /**
         * HTTP URL header 정보
         */
        private Map<String, String> header;

        /**
         * URL parameters
         * parameter를 MultiValueMap에 담는다
         */
        private MultiValueMap<String, String> param;

    }

    /**
     * 전체 OPEN API중 특정잡의 정보를 조회한다.
     *
     * @param jobName 특정 jobName
     * @return
     */
    public ApiProp getApiProp(String jobName) {
        return this.jobs.get(jobName);
    }
}
