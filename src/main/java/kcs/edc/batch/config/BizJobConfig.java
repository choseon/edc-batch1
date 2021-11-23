package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.biz.biz001m.Biz001mTasklet;
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

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 데이터수집 Batch Launcher 설정
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.biz.cron}")
    public void launcher() throws Exception {

        log.info("BIZJobConfig launcher...");
        log.info("isActive: {}", this.isActive);
        if (!this.isActive) return;

        // 수집기준일 : D-1
        String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(bizJob(), jobParameters);
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
