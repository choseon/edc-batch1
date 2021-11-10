package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.jobs.kot.pit811m.Pit811mTasklet;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KotJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Scheduled(cron = "${scheduler.cron.kot}")
    public void launcher() throws Exception {
        log.info("KotJobConfig launcher...");

        String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(kotJob(), jobParameters);
    }

    @Bean
    public Job kotJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_KOT + CmmnConst.POST_FIX_JOB)
                .start(kot001mStep(null))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Step
     */
    @Bean
    @JobScope
    public Step kot001mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get(CmmnConst.JOB_ID_PIT811M + CmmnConst.POST_FIX_STEP)
                .tasklet(kot001mTasklet(baseDt))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
     */
    @Bean
    @StepScope
    public Pit811mTasklet kot001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Pit811mTasklet();
    }
}
