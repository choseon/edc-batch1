package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 데이터수집을 하기 위한 Open API 정보를 담는 클래스
 * api-info.yml의 api 그룹을 자동으로 매핑
 */
@Getter
@Setter
@Component
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperty {

    /**
     * Open API  jobs map info
     */
    private Map<String, JobProp> jobs = new HashMap<>();

    @Getter
    @Setter
    @ToString
    public static class JobProp {

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

    public JobProp getJobProp(String jobName) {
        return this.jobs.get(jobName);
    }

}
