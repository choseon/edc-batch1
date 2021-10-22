package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.jobs.nav.nav003m.Nav003mTasklet;
import kcs.edc.batch.jobs.nav.nav004m.Nav004mTasklet;
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
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NavJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    //    @Scheduled(cron = "${scheduler.cron.kot}")
    public void launcher() throws Exception {
        log.info("NAVJobConfig launcher...");

        // D-4일 기준
        String baseDt = LocalDateTime.now().minusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        baseDt = "20210711";

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(navJob(), jobParameters);
    }

    @Bean
    public Job navJob() {
        return jobBuilderFactory.get(JobConstant.JOB_GRP_ID_NAV + JobConstant.POST_FIX_JOB)
                .start(nav003mStep())
                .next(nav004mStep())
                .build();
    }

    @Bean
    @JobScope
    public Step nav003mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_NAV003M + JobConstant.POST_FIX_STEP)
                .tasklet(nav003mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet nav003mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Nav003mTasklet();
    }

    @Bean
    @JobScope
    public Step nav004mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_NAV004M + JobConstant.POST_FIX_STEP)
                .tasklet(nav004mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet nav004mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Nav004mTasklet();
    }
}
