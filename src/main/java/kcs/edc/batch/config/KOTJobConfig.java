package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.jobs.kot.kot001m.Kot001mTasklet;
import kcs.edc.batch.jobs.kot.kot002m.Kot002mTasklet;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KOTJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

//    @Scheduled(cron = "${scheduler.cron.kot}")
    public void launcher() throws Exception {
        log.info("KotJobConfig launcher...");

        String cletDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cletDt", cletDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(kotJob(), jobParameters);
    }

//    @Bean
    public Job kotJob() {

        return jobBuilderFactory.get(JobConstant.JOB_GRP_ID_KOT + JobConstant.PREFIX_JOB)
                .start(kot001mStep(null))
                .next(kot002mStep(null, null))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Step
     */
    @Bean
    @JobScope
    public Step kot001mStep(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return stepBuilderFactory.get(JobConstant.JOB_ID_KOT001M + JobConstant.PREFIX_STEP)
                .tasklet(kot001mTasklet(cletDt))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
     */
    @Bean
    @StepScope
    public Kot001mTasklet kot001mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Kot001mTasklet();
    }

    @Bean
    @JobScope
    public Step kot002mStep(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[resultList]}") List<Object> resultList) {
        return stepBuilderFactory.get(JobConstant.JOB_ID_KOT002M + JobConstant.PREFIX_STEP)
                .tasklet(kot002mTasklet(cletDt, resultList))
                .build();
    }

    @Bean
    @StepScope
    public Kot002mTasklet kot002mTasklet(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[resultList]}") List<Object> resultList) {
        return new Kot002mTasklet();
    }
}
