package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * application.yml 설정에서 scheduler 정보를 자동으로 매핑하는 클래스
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "job")
public class JobProperties {

    private Map<String, JobProp> info = new HashMap<>();

    @Getter
    @Setter
    public static class JobProp {

        /**
         * 배치 하위 노드 리스트
         */
        private List<String> nodes;

        /**
         * 배치 활성화여부
         */
        private Boolean isActive;

        /**
         * 배치시간
         */
        private String cron;

        /**
         * 배치주기 (daily, monthly...)
         */
        private String cycle;

        /**
         * 배치기준일
         */
        private String baseline;

        /**
         * 배치실행기간
         */
        private int period;
    }


    public JobProp getJobProp(String jobGroupId) {

        return this.info.get(jobGroupId);
    }

}
