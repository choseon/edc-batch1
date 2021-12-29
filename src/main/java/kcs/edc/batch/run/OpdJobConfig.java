package kcs.edc.batch.run;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.jobs.opd.opd001m.Opd001mTasklet;
import kcs.edc.batch.jobs.opd.opd002m.Opd002mTasklet;
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

import java.util.List;

/**
 * 금융감독원 OpenDart 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpdJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${job.info.opd.isActive}")
    private Boolean isActive;

    /**
     * 금융감독원 OpenDart 데이터수집 launcher 설정
     */
    @Scheduled(cron = "${job.info.opd.cron}")
    public void launcher() {

        if (!this.isActive) return;
        log.info(">>>>> {} launcher..... ", this.getClass().getSimpleName().substring(0, 6));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(opdJob(), jobParameters);
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
     * 금융감독원 OpenDart 데이터수집 Job 설정
     *
     * @return
     */
    @Bean
    public Job opdJob() {

        return jobBuilderFactory.get(CmmnProperties.JOB_GRP_ID_OPD + CmmnProperties.POST_FIX_JOB)
                .start(opd001mStep())
                .next(opd002mStep(null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업개황정보 데이터수집 Step 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step opd001mStep() {

        return stepBuilderFactory.get(CmmnProperties.JOB_ID_OPD001M + CmmnProperties.POST_FIX_STEP)
                .tasklet(opd001mTasklet(null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업개황정보 데이터수집 Tasklet 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @StepScope
    public Opd001mTasklet opd001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Opd001mTasklet();
    }

    /**
     * 금융감독원 OpenDart  기업공시정보 데이터수집 Step 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step opd002mStep(
            @Value("#{jobExecutionContext[companyCodeList]}") List<String> companyCodeList) {
        return stepBuilderFactory.get(CmmnProperties.JOB_ID_OPD002M + CmmnProperties.POST_FIX_STEP)
                .tasklet(opd002mTasklet(null, null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업공시정보 데이터수집 Tasklet 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @StepScope
    public Opd002mTasklet opd002mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[companyCodeList]}") List<String> companyCodeList) {
        return new Opd002mTasklet();
    }
}
