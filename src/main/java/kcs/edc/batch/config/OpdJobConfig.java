package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.jobs.opd.iac003l.Iac003lTasklet;
import kcs.edc.batch.jobs.opd.iac016l.Iac016lTasklet;
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

    @Value("${scheduler.opd.isActive}")
    private Boolean isActive;

    /**
     * 금융감독원 OpenDart Job launcher
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.opd.cron}")
    public void launcher() throws Exception {

        log.info("OpdJobConfig launcher...");
        log.info("isActive: {}", this.isActive);
        if (!this.isActive) return;

        // 수집기준일 : D-1
        String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(opdJob(), jobParameters);
    }

    /**
     * 금융감독원 OpenDart Job
     *
     * @return
     */
    @Bean
    public Job opdJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_OPD + CmmnConst.POST_FIX_JOB)
                .start(iac003lStep())
                .next(iac016lStep(null))
                .build();
    }

    /**
     * 금융감독원 OpenDart 기업개황정보 데이터수집 Step
     *
     * @return
     */
    @Bean
    @JobScope
    public Step iac003lStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_IAC016l + CmmnConst.POST_FIX_STEP)
                .tasklet(iac003lTasklet(null))
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
    public Iac003lTasklet iac003lTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Iac003lTasklet();
    }

    /**
     * 금융감독원 OpenDart  기업공시정보 데이터수집 Step
     *
     * @return
     */
    @Bean
    @JobScope
    public Step iac016lStep(
            @Value("#{jobExecutionContext[companyCodeList]}") List<String> companyCodeList) {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_IAC003l + CmmnConst.POST_FIX_STEP)
                .tasklet(iac016lTasklet(null, null))
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
    public Iac016lTasklet iac016lTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[companyCodeList]}") List<String> companyCodeList) {
        return new Iac016lTasklet();
    }
}
