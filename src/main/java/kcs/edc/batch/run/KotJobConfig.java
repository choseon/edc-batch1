package kcs.edc.batch.run;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.jobs.kot.kot001m.Kot001mTasklet;
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
 * 대한무역투자진흥공사 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KotJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${job.info.kot.isActive}")
    private Boolean isActive;

    /**
     * 대한무역투자진흥공사 데이터수집 launcher 설정
     */
    @Scheduled(cron = "${job.info.kot.cron}")
    public void launcher() {

        if (!this.isActive) return;
        log.info(">>>>> {} launcher..... ", this.getClass().getSimpleName().substring(0, 6));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(kotJob(), jobParameters);
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
     * 대한무역투자진흥공사 데이터수집 Job 설정
     *
     * @return
     */
    @Bean
    public Job kotJob() {

        return jobBuilderFactory.get(CmmnProperties.JOB_GRP_ID_KOT + CmmnProperties.POST_FIX_JOB)
                .start(kot001mStep(null))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Step
     */
    @Bean
    @JobScope
    public Step kot001mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get(CmmnProperties.JOB_ID_KOT001M + CmmnProperties.POST_FIX_STEP)
                .tasklet(kot001mTasklet(baseDt))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
     */
    @Bean
    @StepScope
    public Kot001mTasklet kot001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Kot001mTasklet();
    }
}
