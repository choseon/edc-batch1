package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnFileTasklet;
import kcs.edc.batch.cmmn.jobs.CmmnPartitioner;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 바이브컴퍼티 썸트랜드 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SomJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    private int GRID_SIZE = 20;
    private int POOL_SIZE = 20;

    private String currentJobId;

    @Value("${scheduler.jobs.som.isActive}")
    private Boolean isActive;

    //    @Scheduled(cron = "${scheduler.jobs.som.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(somJob(), jobParameters);
        } catch (JobExecutionAlreadyRunningException e) {
            log.info(e.getMessage());
        } catch (JobRestartException e) {
            log.info(e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info(e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * 바이브컴퍼티 썸트랜드 데이터수집 Batch Job 설정
     *
     * @return
     */
    @Bean
    public Job somJob() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return null;

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_SOM + CmmnConst.POST_FIX_JOB)
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

        return new FlowBuilder<Flow>(CmmnConst.JOB_ID_SOM001M + CmmnConst.POST_FIX_FLOW)
                .start(somFileCleanStep(null))
                .next(som001mStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step som001mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM001M + CmmnConst.POST_FIX_STEP)
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

        return new FlowBuilder<Flow>(CmmnConst.JOB_ID_SOM002M + CmmnConst.POST_FIX_FLOW)
                .start(som002mPartitionStep(null, null))// som002mPatitionStep 실행
                .on("COMPLETED") // 성공이면
                .to(somFileMergeStep(null)) // fileMergeStep 실행
                .build();
    }

    @Bean
    @JobScope
    public Step som002mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM002M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("som002mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som002mStep())
                .build();
    }

    @Bean
    public Step som002mStep() {

        this.currentJobId = CmmnConst.JOB_ID_SOM002M;
        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM002M + CmmnConst.POST_FIX_STEP)
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

        return new FlowBuilder<Flow>(CmmnConst.JOB_ID_SOM003M + CmmnConst.POST_FIX_FLOW)
                .start(som003mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep(null))
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

        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM003M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("som003mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som003mStep())
                .build();
    }

    @Bean
    public Step som003mStep() {

        this.currentJobId = CmmnConst.JOB_ID_SOM003M;
        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM003M + CmmnConst.POST_FIX_STEP)
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

        return new FlowBuilder<Flow>(CmmnConst.JOB_ID_SOM004M + CmmnConst.POST_FIX_FLOW)
                .start(som004mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step som004mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM004M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("som004mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som004mStep())
                .build();
    }

    @Bean
    public Step som004mStep() {

        this.currentJobId = CmmnConst.JOB_ID_SOM004M;
        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM004M + CmmnConst.POST_FIX_STEP)
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

        return new FlowBuilder<Flow>(CmmnConst.JOB_ID_SOM005M + CmmnConst.POST_FIX_FLOW)
                .start(som005mPartitionStep(null, null))
                .on("COMPLETED")
                .to(somFileMergeStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step som005mPartitionStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM005M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("som005mStep", cmmnPartitioner(baseDt, list)) // partitioning
                .gridSize(GRID_SIZE) // partitioning size
                .taskExecutor(somExecutor()) // multi thread
                .step(som005mStep())
                .build();
    }

    @Bean
    public Step som005mStep() {

        this.currentJobId = CmmnConst.JOB_ID_SOM005M;
        return stepBuilderFactory.get(CmmnConst.JOB_ID_SOM005M + CmmnConst.POST_FIX_STEP)
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
    @JobScope
    public Step somFileMergeStep(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        return stepBuilderFactory.get(CmmnConst.JOB_GRP_ID_SOM + CmmnConst.POST_FIX_FILE_MERGE_STEP)
                .tasklet(somFileMergeTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public CmmnFileTasklet somFileMergeTasklet(@Value("#{jobExecutionContext[jobId]}") String jobId) {
        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE);
    }

    @Bean
    @JobScope
    public Step somFileCleanStep(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        return stepBuilderFactory.get(CmmnConst.JOB_GRP_ID_SOM + CmmnConst.POST_FIX_FILE_CLEAN_STEP)
                .tasklet(somFileCleanTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public CmmnFileTasklet somFileCleanTasklet(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        List<String> list = new ArrayList<>();
        list.add(CmmnConst.JOB_ID_SOM002M);
        list.add(CmmnConst.JOB_ID_SOM003M);
        list.add(CmmnConst.JOB_ID_SOM004M);
        list.add(CmmnConst.JOB_ID_SOM005M);

        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN, list);
    }

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
