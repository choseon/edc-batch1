package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnMergeFile;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mPartitioner;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UCTJobConfig {


    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    private int GRID_SIZE = 10;
    private int POOL_SIZE = 10;

    //    @Scheduled(cron = "${scheduler.cron.uct}")
    public void launcher() {
        log.info("UctConfiguration launcher...");

        String cletDt = DateUtil.getToday("yyyyMMdd"); // 수집일
        String baseDt = DateUtil.getOffsetDate(cletDt, -1, "yyyyMMdd"); // 기준일

//        String cletDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cletDt", cletDt)
                .addString("baseDt", baseDt)
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

    @Bean
    public Job uctJob() {

        return jobBuilderFactory.get(JobConstant.JOB_GRP_ID_UCT + JobConstant.PREFIX_JOB)
                .start(uct001mPartitionStep())
                .next(uctFileMergeStep())
                .build();
    }

    @Bean
    public Step uct001mPartitionStep() {

        return stepBuilderFactory.get(JobConstant.JOB_ID_UCT001M + JobConstant.PREFIX_PARTITION_STEP)
                .partitioner("uct001mStep", new Uct001mPartitioner())
                .gridSize(GRID_SIZE)
                .taskExecutor(uctExecutor())
                .step(uct001mStep())
                .build();
    }

    @Bean
    public Step uct001mStep() {

        return stepBuilderFactory.get(JobConstant.JOB_ID_UCT001M + JobConstant.PREFIX_STEP)
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
    public Step uctFileMergeStep() {

        return stepBuilderFactory.get(JobConstant.JOB_ID_UCT001M + JobConstant.PREFIX_FILE_STEP)
                .tasklet(new CmmnMergeFile(JobConstant.JOB_ID_UCT001M))
                .build();
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
