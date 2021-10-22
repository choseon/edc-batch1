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
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private String currentJobId;

    @Scheduled(cron = "${scheduler.cron.som}")
    public void launcher() {
        log.info("SomConfiguration launcher...");

        try {
            String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDt", baseDt)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(somJob(), jobParameters);

        } catch (JobExecutionAlreadyRunningException e) {
            e.printStackTrace();
        } catch (JobRestartException e) {
            e.printStackTrace();
        } catch (JobInstanceAlreadyCompleteException e) {
            e.printStackTrace();
        } catch (JobParametersInvalidException e) {
            e.printStackTrace();
        }
    }

    @Bean
    public Job somJob() {

        return jobBuilderFactory.get(JobConstant.JOB_GRP_ID_SOM + JobConstant.POST_FIX_JOB)
                .start(som001mFlow())
                .next(som002mFlow())
                .next(som003mFlow())
                .next(som004mFlow())
                .next(som005mFlow())
                .end()
                .build();
    }


    /**************************************************************************************************
     * Som001m 관련 설정
     **************************************************************************************************/

    @Bean
    @JobScope
    public Flow som001mFlow() {
        return new FlowBuilder<Flow>(JobConstant.JOB_ID_SOM001M + JobConstant.POST_FIX_FLOW)
                .start(som001mStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step som001mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM001M + JobConstant.POST_FIX_STEP)
                .tasklet(som001mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som001mTasklet som001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Som001mTasklet();
    }

    /**************************************************************************************************
     * Som002m 관련 설정
     **************************************************************************************************/

    @Bean
    @JobScope
    public Flow som002mFlow() {
        currentJobId = JobConstant.JOB_ID_SOM002M;
        return new FlowBuilder<Flow>(JobConstant.JOB_ID_SOM002M + JobConstant.POST_FIX_FLOW)
                .start(som002mPartitionStep(null, null))// som002mPatitionStep 실행
                .on("COMPLETED") // 성공이면
                .to(somFileMergeStep()) // fileMergeStep 실행
                .build();
    }

    @Bean
    @JobScope
    public Step som002mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM002M + JobConstant.POST_FIX_PARTITION_STEP)
                .partitioner("som002mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som002mStep())
                .build();
    }

    @Bean
    public Step som002mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM002M + JobConstant.POST_FIX_STEP)
                .tasklet(som002mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som002mTasklet som002mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Som002mTasklet();
    }

    /**************************************************************************************************
     * Som003m 관련 설정
     **************************************************************************************************/

    @Bean
    @JobScope
    public Flow som003mFlow() {
        currentJobId = JobConstant.JOB_ID_SOM003M;
        return new FlowBuilder<Flow>(JobConstant.JOB_ID_SOM003M + JobConstant.POST_FIX_FLOW)
                .start(som003mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep())
                .build();
    }

    /**
     * @param baseDt
     * @param list
     * @return
     */
    @Bean
    @JobScope
    public Step som003mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM003M + JobConstant.POST_FIX_PARTITION_STEP)
                .partitioner("som003mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som003mStep())
                .build();
    }

    @Bean
    public Step som003mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM003M + JobConstant.POST_FIX_STEP)
                .tasklet(som003mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som003mTasklet som003mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Som003mTasklet();
    }

    /**************************************************************************************************
     * Som004m 관련 설정
     **************************************************************************************************/

    @Bean
    @JobScope
    public Flow som004mFlow() {
        currentJobId = JobConstant.JOB_ID_SOM004M;
        return new FlowBuilder<Flow>(JobConstant.JOB_ID_SOM004M + JobConstant.POST_FIX_FLOW)
                .start(som004mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep())
                .build();


    }

    @Bean
    @JobScope
    public Step som004mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM004M + JobConstant.POST_FIX_PARTITION_STEP)
                .partitioner("som004mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som004mStep())
                .build();
    }

    @Bean
    public Step som004mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM004M + JobConstant.POST_FIX_STEP)
                .tasklet(som004mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som004mTasklet som004mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Som004mTasklet();
    }

    /**************************************************************************************************
     * Som005m 관련 설정
     **************************************************************************************************/

    @Bean
    @JobScope
    public Flow som005mFlow() {
        currentJobId = JobConstant.JOB_ID_SOM005M;
        return new FlowBuilder<Flow>(JobConstant.JOB_ID_SOM005M + JobConstant.POST_FIX_FLOW)
                .start(som005mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep())
                .build();
    }

    @Bean
    @JobScope
    public Step som005mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM005M + JobConstant.POST_FIX_PARTITION_STEP)
                .partitioner("som005mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som005mStep())
                .build();
    }

    @Bean
    public Step som005mStep() {
        return stepBuilderFactory.get(JobConstant.JOB_ID_SOM005M + JobConstant.POST_FIX_STEP)
                .tasklet(som005mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Som005mTasklet som005mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Som005mTasklet();
    }


    /**************************************************************************************************
     * 공통 Step
     **************************************************************************************************/

    @Bean
//    @JobScope
    public Step somFileMergeStep() {


//        if(Objects.isNull(currentJobId)) return null;
        return stepBuilderFactory.get(JobConstant.JOB_GRP_ID_SOM + JobConstant.POST_FIX_FILE_STEP)
                .tasklet(new CmmnMergeFile(currentJobId))
                .build();
    }

//    @Bean
//    @StepScope
//    public CmmnMergeFile somMergeFileTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
//        return new CmmnMergeFile(currentJobId);
//    }

/*    @Bean
    @StepScope
    public CmmnMergeFile somMergeFileTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {

        List<String> mergeJobList = new ArrayList<>();
        mergeJobList.add(JobConstant.JOB_ID_SOM002M);
        mergeJobList.add(JobConstant.JOB_ID_SOM003M);
        mergeJobList.add(JobConstant.JOB_ID_SOM004M);
        mergeJobList.add(JobConstant.JOB_ID_SOM005M);

        return new CmmnMergeFile(mergeJobList);
    }*/

    @Bean
    public TaskExecutor somExecutor() {
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
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> resultList) {
        return new CmmnPartitioner();
    }

}
