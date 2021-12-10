package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {

    private Map<String, SchedulerProp> jobs = new HashMap<>();

    @Getter
    @Setter
    public static class SchedulerProp {
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


    public SchedulerProp getScedulerProp(String jobGroupId) {

        return this.jobs.get(jobGroupId);
    }

}
