package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.biz.biz001m.Biz001mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
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
 * 중소기업연구원 중소벤처기업부 기업마당 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BizJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${scheduler.biz.isActive}")
    private Boolean isActive;

    @Value("${scheduler.biz.baseline}")
    private String baseline;

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 데이터수집 Batch Launcher 설정
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.biz.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        String baseDt = DateUtil.getBaseLineDate(this.baseline);
        log.info(">>>>> baseline: {}, baseDt: {}", this.baseline, baseDt);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(bizJob(), jobParameters);
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
     * 중소기업연구원 중소벤처기업부 기업마당 데이터수집 Batch Job 설정
     *
     * @return
     */
    @Bean
    public Job bizJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_BIZ + CmmnConst.POST_FIX_JOB)
                .start(biz001mStep())
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 최신공고 Batch Step 설정
     */
    @Bean
    public Step biz001mStep() {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_BIZ001M + CmmnConst.POST_FIX_STEP)
                .tasklet(biz001mTasklet(null))
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 최신공고 Batch Tasklet 설정
     */
    @Bean
    @StepScope
    public Biz001mTasklet biz001mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Biz001mTasklet();
    }

}
