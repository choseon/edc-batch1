package kcs.edc.batch.run;

import kcs.edc.batch.cmmn.jobs.CmmnFileTasklet;
import kcs.edc.batch.cmmn.property.CmmnProperties;
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
 * 공통 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${job.info.com.isActive}")
    private Boolean isActive;

    /**
     * 공통 Batch launcher 설정
     */
    @Scheduled(cron = "${job.info.com.cron}")
    public void launcher() {

        if (!this.isActive) return;
        log.info(">>>>> {} launcher..... ", this.getClass().getSimpleName().substring(0, 6));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(comJob(), jobParameters);
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
     * 공통 Batch Job 설정
     *
     * @return
     */
    @Bean
    public Job comJob() {

        return jobBuilderFactory.get(CmmnProperties.JOB_GRP_ID_COM + CmmnProperties.POST_FIX_JOB)
                .start(backupFileCleanStep())
                .build();
    }

    /**
     * 백업파일제거 Step 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step backupFileCleanStep() {

        return stepBuilderFactory.get(CmmnProperties.JOB_GRP_ID_COM + CmmnProperties.POST_FIX_FILE_CLEAN_STEP)
                .tasklet(backupFileCleanTasklet(null))
                .build();
    }

    /**
     * 백업파일제거 Tasklet 설정
     *
     * @param jobId
     * @return
     */
    @Bean
    @StepScope
    public CmmnFileTasklet backupFileCleanTasklet(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        return new CmmnFileTasklet(CmmnProperties.CMMN_FILE_ACTION_TYPE_BACKUP_CLEAN);
    }

}
