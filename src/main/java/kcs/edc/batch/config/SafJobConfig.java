package kcs.edc.batch.config;

import kcs.edc.batch.jobs.saf.saf001l.Saf001lTasklet;
import kcs.edc.batch.jobs.saf.saf001m.Saf001mTasklet;
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
 *
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SafJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    /**
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.cron.saf}")
    public void launcher() throws Exception {

        String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(safJob(), jobParameters);
    }

    /**
     *
     * @return
     */
    @Bean
    public Job safJob() {

        return jobBuilderFactory.get("safJob")
                .start(saf001mStep(null))
                .next(saf001lStep(null, null))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Step
     */
    @Bean
    @JobScope
    public Step saf001mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("saf001mStep")
                .tasklet(saf001mTasklet(baseDt))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Tasklet
     */
    @Bean
    @StepScope
    public Saf001mTasklet saf001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Saf001mTasklet();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Step
     */
    @Bean
    @JobScope
    public Step saf001lStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {

        return stepBuilderFactory.get("saf001lStep")
                .tasklet(saf001lTasklet(baseDt, certNumList))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Tasklet
     */
    @Bean
    @StepScope
    public Saf001lTasklet saf001lTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {
        return new Saf001lTasklet();
    }

}
