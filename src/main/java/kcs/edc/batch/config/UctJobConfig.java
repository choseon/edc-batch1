package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnMergeFile;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mPartitioner;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mTasklet;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UctJobConfig {


    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    private int GRID_SIZE = 10;
    private int POOL_SIZE = 10;

    //    @Scheduled(cron = "${scheduler.cron.uct}")
    public void launcher() throws Exception {
        log.info("UctConfiguration launcher...");

        String cletDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cletDt", cletDt)
                .addLong("time",System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(uctJob(), jobParameters);
    }

//    @Bean
    public Job uctJob() {

        return jobBuilderFactory.get("uctJob")
                .start(uct001mStepManager(null))
//                .next(cmmnMergeFileStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step uct001mStepManager(
            @Value("#{jobParameters[cletDt]}") String cletDt) {

        return stepBuilderFactory.get(JobConfig.JOB_ID_UCT001M + "Manager")
                .partitioner("uct002mStep", uct001mPartitioner(cletDt))
                .gridSize(GRID_SIZE)
                .taskExecutor(uctExecutor())
                .step(uct001mStep())
                .build();
    }

    @Bean
    @JobScope
    public Uct001mPartitioner uct001mPartitioner(
            @Value("#{jobParameters[cletDt]}") String cletDt) {

        return new Uct001mPartitioner();
    }

    @Bean
//    @JobScope
    public Step uct001mStep() {

        return stepBuilderFactory.get(JobConfig.JOB_ID_UCT001M + JobConfig.PREFIX_STEP)
                .tasklet(uct001mTasklet(null, null, null))
                .build();
    }

    @Bean
    @StepScope
    public Uct001mTasklet uct001mTasklet(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{stepExecutionContext[threadNum]}") String threadNum,
            @Value("#{stepExecutionContext[partitionList]}") List<String> partitionList) {

        return new Uct001mTasklet();
    }

    @Bean
    @JobScope
    public Step uctFileMergeStep(
            @Value("#{jobParameters[cletDt]}") String cletDt) {

        return stepBuilderFactory.get("uctFileMergeStep")
                .tasklet(uctFileMergeTasklet(cletDt))
                .build();
    }

    @Bean
    @StepScope
    public CmmnMergeFile uctFileMergeTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {

        return new CmmnMergeFile(JobConstant.JOB_ID_UCT001M);
    }

    @Bean
    public TaskExecutor uctExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setThreadNamePrefix("multi-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }
}
