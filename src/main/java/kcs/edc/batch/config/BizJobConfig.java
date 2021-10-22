package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.JobConstant;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 중소기업연구원 중소벤처기업부 기업마당 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BizJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

//    @Scheduled(cron = "${scheduler.cron.biz}")
    public void launcher() throws Exception {
        log.info("BIZJobConfig launcher...");

        String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

//        jobLauncher.run(bizJob(), jobParameters);
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 Job
     * @return
     */
    @Bean
    public Job bizJob() {

        return jobBuilderFactory.get(JobConstant.JOB_GRP_ID_BIZ + JobConstant.POST_FIX_JOB)
                .start(biz001mStep(null))
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 Setp
     */
    @Bean
    @JobScope
    public Step biz001mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get(JobConstant.JOB_ID_BIZ001M + JobConstant.POST_FIX_STEP)
                .tasklet(biz001mTasklet(baseDt))
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 Tasklet
     */
    @Bean
    @StepScope
    public Biz001mTasklet biz001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Biz001mTasklet();
    }

}