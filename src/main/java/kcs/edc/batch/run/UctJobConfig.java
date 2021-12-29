package kcs.edc.batch.run;

import kcs.edc.batch.cmmn.property.CmmnProperties;
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

    /**
     * 시간단축을 위해 multiThread 적용하여 초기데이터 수집결과
     * 1. api 호출이 충돌되어 엄청난 exception이 발생
     * 2. 데이터가 존재함에도 0건으로 수집되는 경우가 빈번하게 발생
     * 3. 1시간에 10,000번의 limit가 넘는 경우 발생
     *
     * multiThread를 적용하지 않은 경우 시간은 오래 걸려도 안정적으로 수집됨.
     *
     * 현재 multiThread 10 -> 5 -> 3 -> 1개로 설정 (application.yml)
     * 월배치로 매월 1일 전년도, 전전년도 2년치 변경적재 배치
     * 중단없이 수집하기 위해 [prod.monthly] 경로로 일배치와 분리함
     */
    @Value("${uct.gridSize}")
    private int GRID_SIZE;

    @Value("${uct.gridSize}")
    private int POOL_SIZE; // gridSize와 동일하게 적용

    @Value("${job.info.uct.isActive}")
    private Boolean isActive;

    /**
     * UN Comtrade Batch launcher 설정
     */
    @Scheduled(cron = "${job.info.uct.cron}")
    public void launcher() {

        if (!this.isActive) return;
        log.info(">>>>> {} launcher..... ", this.getClass().getSimpleName().substring(0, 6));

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

        return jobBuilderFactory.get(CmmnProperties.JOB_GRP_ID_UCT + CmmnProperties.POST_FIX_JOB)
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

        return stepBuilderFactory.get(CmmnProperties.JOB_ID_UCT001M + CmmnProperties.POST_FIX_PARTITION_STEP)
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

        return stepBuilderFactory.get(CmmnProperties.JOB_ID_UCT001M + CmmnProperties.POST_FIX_STEP)
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
