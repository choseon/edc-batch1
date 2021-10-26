package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnFileTasklet;
import kcs.edc.batch.cmmn.jobs.CmmnMergeFile;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mPartitioner;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
import java.util.ArrayList;
import java.util.List;

/**
 * UN Comtrade 수출데이터 수집 Job Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UctJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    private int GRID_SIZE = 10;
    private int POOL_SIZE = 10;

    /**
     * UN Comtrade job launcher (월배치)
     * 매월 1일 전년도, 전전년도 2년치 데이터 수집하여
     * 15일 이전에 내부 hlo1db에서 데이터 조회되도록 스케쥴링함.
     */
    @Scheduled(cron = "${scheduler.cron.uct}")
    public void launcher() {
        log.info("UctConfiguration launcher...");

        // 수집기준일 : 오늘
        String baseDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 수집기준년도 : 전년도, 전전년도 2년치
        String baseYear = LocalDateTime.now().minusYears(2).format(DateTimeFormatter.ofPattern("yyyy"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addString("baseYear", baseYear)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(uctJob(), jobParameters);
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

    /**
     * UN Comtrade Job 설정
     *
     * @return
     */
    @Bean
    public Job uctJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_UCT + CmmnConst.POST_FIX_JOB)
                .start(uctFileCleanStep())
                .next(uct001mPartitionStep())
                .next(uctFileMergeStep())
                .build();
    }

    /**
     * uct001m Partition Step
     * Multi Thread를 위한 partitionr, executor 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step uct001mPartitionStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_UCT001M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("uct001mStep", new Uct001mPartitioner())
                .gridSize(GRID_SIZE)
                .taskExecutor(uctExecutor())
                .step(uct001mStep())
                .build();
    }

    /**
     * uct001m Step
     *
     * @return
     */
    @Bean
    public Step uct001mStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_UCT001M + CmmnConst.POST_FIX_STEP)
                .tasklet(uct001mTasklet(null, null, null, null))
                .build();
    }

    /**
     * uct001m Tasklet
     * partitioner를 통해 threadNum, partitionList를 넘겨받아 multi thread 실행
     *
     * @param baseDt
     * @param threadNum
     * @param partitionList
     * @return
     */
    @Bean
    @StepScope
    public Uct001mTasklet uct001mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobParameters[baseYear]}") String baseYear,
            @Value("#{stepExecutionContext[threadNum]}") String threadNum,
            @Value("#{stepExecutionContext[partitionList]}") List<String> partitionList) {

        return new Uct001mTasklet();
    }

    /**
     * File Merge Step
     * Multi Thread 실행시 저장된 Temp File 을 Merge하기 위한 Step
     *
     * @return
     */
    @Bean
    public Step uctFileMergeStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_UCT001M + CmmnConst.POST_FIX_FILE_MERGE_STEP)
//                .tasklet(new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE, CmmnConst.JOB_ID_UCT001M))
//                .tasklet(cmmnMergeFile(null))
                .tasklet(uctFileMergeTasklet())
                .build();
    }

    /**
     * File Merge Tasklet
     * @return
     */
    @Bean
    public CmmnFileTasklet uctFileMergeTasklet() {
        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE, CmmnConst.JOB_ID_UCT001M);
    }


    /**
     * File Clean Step
     * @return
     */
    @Bean
    public Step uctFileCleanStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_UCT001M + CmmnConst.POST_FIX_FILE_CLEAN_STEP)
//                .tasklet(new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN, CmmnConst.JOB_ID_UCT001M))
//                .tasklet(cmmnMergeFile(null))
                .tasklet(uctFileCleanTasklet())
                .build();
    }

    /**
     * File Clean Tasklet
     * @return
     */
    @Bean
    public CmmnFileTasklet uctFileCleanTasklet() {
        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN, CmmnConst.JOB_ID_UCT001M);
    }

    @Bean
    @StepScope
    public CmmnMergeFile cmmnMergeFile(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new CmmnMergeFile(CmmnConst.JOB_ID_UCT001M);

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
