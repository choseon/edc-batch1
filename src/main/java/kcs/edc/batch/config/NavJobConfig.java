package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.jobs.nav.nav003m.Nav003mTasklet;
import kcs.edc.batch.jobs.nav.nav004m.Nav004mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NavJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${scheduler.nav.isActive}")
    private Boolean isActive;

    @Scheduled(cron = "${scheduler.nav.cron}")
    public void launcher() throws Exception {

        log.info("NAVJobConfig launcher...");
        log.info("isActive: {}", this.isActive);
        if (!this.isActive) return;

        // D-4일 기준
        String baseDt = LocalDateTime.now().minusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(navJob(), jobParameters);
    }

    @Bean
    public Job navJob() {
        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_NAV + CmmnConst.POST_FIX_JOB)
                .start(nav003mStep())
//                .next(nav004mStep())
                .build();
    }

    /**
     * 네이버카페 나이키매니아 판매데이터 수집 Step 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step nav003mStep() {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_NAV003M + CmmnConst.POST_FIX_STEP)
                .tasklet(nav003mTasklet(null))
                .build();
    }

    /**
     * 네이버카페 나이키매니아 판매데이터 수집 Tasklet 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @StepScope
    public Nav003mTasklet nav003mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Nav003mTasklet();
    }

    /**
     * 네이버카페 중고나라 판매데이터 수집 Step 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step nav004mStep() {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_NAV004M + CmmnConst.POST_FIX_STEP)
                .tasklet(nav004mTasklet(null))
                .build();
    }

    /**
     * 네이버카페 중고나라 판매데이터 수집 Tasklet 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @StepScope
    public Nav004mTasklet nav004mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Nav004mTasklet();
    }
}
