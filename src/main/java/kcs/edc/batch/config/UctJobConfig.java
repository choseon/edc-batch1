package kcs.edc.batch.config;

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

import java.util.List;

/**
 * UN Comtrade 수출데이터 수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UctJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    // multiThread 갯수를 application.yml에 설정
    // API 호출시 1시간에 10,000번의 limit가 걸려 있기 때문에 적절하게 Thread 갯수 조정필요.
    // 현재 3개로 설정했을때 limt 넘지 않으므로 limit 넘는 경우가 있다면 갯수를 줄여야함.
    @Value("${uct.gridSize}")
    private int GRID_SIZE;

    @Value("${uct.gridSize}")
    private int POOL_SIZE; // gridSize와 동일하게 적용

    @Value("${scheduler.jobs.uct.isActive}")
    private Boolean isActive;

    /**
     * UN Comtrade Batch launcher (월배치)
     * 매월 1일 전년도, 전전년도 2년치 데이터 수집하여
     * 15일 이전에 내부 hlo1db에서 데이터 조회되도록 스케쥴링 필요.
     */
    @Scheduled(cron = "${scheduler.jobs.uct.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            this.jobLauncher.run(uctJob(), jobParameters);
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
     * UN Comtrade Batch Job 설정한다.
     * Partitioning과 MultiThread를 실행한다.
     * MultiThread로 생성된 Temp파일을 정리 및 병합하기 위해
     * PartitionStep 전후에 FileCleanStep과 FileMergeStep을 실행한다.
     *
     * @return
     */
    @Bean
    public Job uctJob() {

        return jobBuilderFactory.get(CmmnConst.JOB_GRP_ID_UCT + CmmnConst.POST_FIX_JOB)
                .start(uct001mStep())
                .build();
    }


    /**
     * uct001m Partition Step
     * MultiThread를 위한 partitionr, executor 설정
     *
     * @return
     */
    @Bean
    @JobScope
    public Step uct001mPartitionStep(@Value("#{jobExecutionContext[list]}") List<Object> list) {

        return stepBuilderFactory.get(CmmnConst.JOB_ID_UCT001M + CmmnConst.POST_FIX_PARTITION_STEP)
                .partitioner("uctPartitioner", uct001mPartitioner(null))
                .gridSize(GRID_SIZE)
                .taskExecutor(uctExecutor())
                .step(uct001mStep())
                .build();
    }

    @Bean
    @StepScope
    public Uct001mPartitioner uct001mPartitioner(@Value("#{jobExecutionContext[list]}") List<Object> list) {
        return new Uct001mPartitioner();
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
            @Value("#{jobParameters[ps]}") String ps,
            @Value("#{stepExecutionContext[threadNum]}") String threadNum,
            @Value("#{stepExecutionContext[partitionList]}") List<String> partitionList) {

        return new Uct001mTasklet();
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
