package kcs.edc.batch.run;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.jobs.nav.nav003m.Nav003mTasklet;
import kcs.edc.batch.jobs.nav.nav004m.Nav004mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 네이버카페 (나이키매니아, 중고나라) 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NavJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${job.info.nav.isActive}")
    private Boolean isActive;

    /**
     * 네이버카페 데이터수집 launcher 설정
     */
    @Scheduled(cron = "${job.info.nav.cron}")
    public void launcher() {

        if (!this.isActive) return;
        log.info(">>>>> {} launcher..... ", this.getClass().getSimpleName().substring(0, 6));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(navJob(), jobParameters);
        } catch (JobExecutionAlreadyRunningException e) {
            log.info(e.getMessage());
        } catch (JobRestartException e) {
            log.info(e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info(e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.info(e.getMessage());
        }
    }


    /**
     * 네이버카페 데이터수집 Job 설정
     *
     * @return
     */
    @Bean
    public Job navJob() {

        return jobBuilderFactory.get(CmmnProperties.JOB_GRP_ID_NAV + CmmnProperties.POST_FIX_JOB)
                .start(nav003mStep())
                .next(nav004mStep())
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
        return stepBuilderFactory.get(CmmnProperties.JOB_ID_NAV003M + CmmnProperties.POST_FIX_STEP)
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
        return stepBuilderFactory.get(CmmnProperties.JOB_ID_NAV004M + CmmnProperties.POST_FIX_STEP)
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
