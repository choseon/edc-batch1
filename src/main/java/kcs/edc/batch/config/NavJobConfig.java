package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NavJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${scheduler.jobs.nav.isActive}")
    private Boolean isActive;

    @Value("${scheduler.jobs.nav.baseline}")
    private String baseline;

    @Scheduled(cron = "${scheduler.jobs.nav.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        String baseDt = DateUtil.getBaseLineDate(this.baseline);
        log.info("baseline: {}, baseDt: {}", this.baseline, baseDt);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(navJob(), jobParameters);
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

    @Bean
    public Job navJob() {
        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_NAV + CmmnConst.POST_FIX_JOB)
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
