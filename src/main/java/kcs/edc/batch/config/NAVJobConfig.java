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
public class NAVJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    //    @Scheduled(cron = "${scheduler.cron.kot}")
    public void launcher() throws Exception {
        log.info("NAVJobConfig launcher...");

        String cletDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cletDt", cletDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(navJob(), jobParameters);
    }

    @Bean
    public Job navJob() {
        return jobBuilderFactory.get("navJob")
                .start(nav003mStep())
                .next(nav004mStep())
                .build();
    }

    @Bean
//    @JobScope
    public Step nav003mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_NAV003M + JobConstant.PREFIX_STEP)
                .tasklet(nav003mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet nav003mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Nav003mTasklet();
    }

    @Bean
//    @JobScope
    public Step nav004mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_NAV004M + JobConstant.PREFIX_STEP)
                .tasklet(nav004mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet nav004mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Nav004mTasklet();
    }
}
