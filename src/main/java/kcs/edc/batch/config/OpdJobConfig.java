package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
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

    @Value("${scheduler.jobs.opd.isActive}")
    private Boolean isActive;

    @Value("${scheduler.jobs.opd.baseline}")
    private String baseline;

    /**
     * 금융감독원 OpenDart Job launcher
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.jobs.opd.cron}")
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
            jobLauncher.run(opdJob(), jobParameters);
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
     * 금융감독원 OpenDart Job
     *
     * @return
     */
    @Bean
    public Job opdJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_OPD + CmmnConst.POST_FIX_JOB)
                .start(opd001mStep())
                .next(opd002mStep(null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업개황정보 데이터수집 Step
     *
     * @return
     */
    @Bean
    @JobScope
    public Step opd001mStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_OPD001M + CmmnConst.POST_FIX_STEP)
                .tasklet(opd001mTasklet(null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업개황정보 데이터수집 Tasklet
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
     * 금융감독원 OpenDart  기업공시정보 데이터수집 Step
     *
     * @return
     */
    @Bean
    @JobScope
    public Step opd002mStep(
            @Value("#{jobExecutionContext[companyCodeList]}") List<String> companyCodeList) {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_OPD002M + CmmnConst.POST_FIX_STEP)
                .tasklet(opd002mTasklet(null, null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업공시정보 데이터수집 Tasklet
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
