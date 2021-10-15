package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnMergeFile;
import kcs.edc.batch.cmmn.jobs.CmmnPartitioner;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.jobs.som.som001m.Som001mTasklet;
import kcs.edc.batch.jobs.som.som002m.Som002mTasklet;
import kcs.edc.batch.jobs.som.som003m.Som003mTasklet;
import kcs.edc.batch.jobs.som.som004m.Som004mTasklet;
import kcs.edc.batch.jobs.som.som005m.Som005mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SomJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    private int GRID_SIZE = 10;
    private int POOL_SIZE = 10;

/*    @Scheduled(cron = "${scheduler.cron.som}")
    public void launcher() throws Exception {
        log.info("SomConfiguration launcher...");

        String cletDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cletDt", cletDt)
                .addLong("time",System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(somJob(), jobParameters);
    }*/

//    @Bean
    public Job somJob() {

        return jobBuilderFactory.get("somJob")
                .start(som001mStep(null))
                .next(som002mStepManager(null, null))
                .next(somFileMergeStep(null))
                .next(som003mStepManager(null, null))
                .next(somFileMergeStep(null))
                .next(som004mStepManager(null, null))
                .next(somFileMergeStep(null))
                .next(som005mStepManager(null, null))
                .next(somFileMergeStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step som001mStep(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM001M + JobConfig.PREFIX_STEP)
                .tasklet(som001mTasklet(cletDt))
                .build();
    }

    @Bean
    @StepScope
    public Som001mTasklet som001mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Som001mTasklet();
    }

    @Bean
    @JobScope
    public Step som002mStepManager(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM002M + "Manager")
                .partitioner("som002mStep", cmmnPartitioner(cletDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(executor()) // multi thread
                .step(som002mStep())
                .build();
    }

    @Bean
    public Step som002mStep() {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM002M + JobConfig.PREFIX_STEP)
                .tasklet(som002mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som002mTasklet som002mTasklet(
            @Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Som002mTasklet();
    }

    @Bean
    @JobScope
    public Step som003mStepManager(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM003M + "Manager")
                .partitioner("som003mStep", cmmnPartitioner(cletDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(executor()) // multi thread
                .step(som003mStep())
                .build();
    }

    @Bean
    public Step som003mStep() {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM003M + JobConfig.PREFIX_STEP)
                .tasklet(som003mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som003mTasklet som003mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Som003mTasklet();
    }

    @Bean
    @JobScope
    public Step som004mStepManager(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM004M + "Manager")
                .partitioner("som004mStep", cmmnPartitioner(cletDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(executor()) // multi thread
                .step(som004mStep())
                .build();
    }

    @Bean
    public Step som004mStep() {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM004M + JobConfig.PREFIX_STEP)
                .tasklet(som004mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som004mTasklet som004mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Som004mTasklet();
    }

    @Bean
    @JobScope
    public Step som005mStepManager(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM005M + "Manager")
                .partitioner("som005mStep", cmmnPartitioner(cletDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(executor()) // multi thread
                .step(som005mStep())
                .build();
    }

    @Bean
    public Step som005mStep() {
        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM005M + JobConfig.PREFIX_STEP)
                .tasklet(som005mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som005mTasklet som005mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Som005mTasklet();
    }

    @Bean
    @JobScope
    public Step somFileMergeStep(
            @Value("#{jobParameters[cletDt]}") String cletDt) {

        return stepBuilderFactory.get(JobConfig.JOB_ID_SOM005M + JobConfig.PREFIX_STEP)
                .tasklet(somMergeFileTasklet(cletDt))
                .build();
    }

    @Bean
    @StepScope
    public CmmnMergeFile somMergeFileTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {

        List<String> mergeJobList = new ArrayList<>();
        mergeJobList.add(JobConstant.JOB_ID_SOM002M);
        mergeJobList.add(JobConstant.JOB_ID_SOM003M);
        mergeJobList.add(JobConstant.JOB_ID_SOM004M);
        mergeJobList.add(JobConstant.JOB_ID_SOM005M);

        return new CmmnMergeFile(mergeJobList);
    }

    @Bean
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setThreadNamePrefix("multi-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }

    @Bean
    @JobScope
    public CmmnPartitioner cmmnPartitioner(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[list]}") List<Object> resultList) {
        return new CmmnPartitioner();
    }

}
