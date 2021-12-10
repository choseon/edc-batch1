package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.jobs.CmmnFileTasklet;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.uct.uct001m.Uct001mMergeTasklet;
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

    @Value("${scheduler.jobs.uct.baseline}")
    private String baseline;

    /**
     * UN Comtrade Batch launcher (월배치)
     * 매월 1일 전년도, 전전년도 2년치 데이터 수집하여
     * 15일 이전에 내부 hlo1db에서 데이터 조회되도록 스케쥴링 필요.
     */
    @Scheduled(cron = "${scheduler.jobs.uct.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        String baseDt = DateUtil.getCurrentDate("yyyyMM");

//        String baseYear = null;
        int day = Integer.parseInt(DateUtil.getCurrentDate("dd"));
//        if (day < 7) {
//            baseYear = DateUtil.getBaseLineDate("Y-1");
//
//        } else {
//            baseYear = DateUtil.getBaseLineDate("Y-2");
//        }
//        if(day > 7) {
//            this.baseline = "Y-2";
//        }
        this.baseline = (day < 7) ? "Y-1" : "Y-2";
        String baseYear = DateUtil.getBaseLineDate(this.baseline);

        log.info(">>>>> baseline: {}, baseDt: {}, baseYear: {}", this.baseline, baseDt, baseYear);





        // 수집기준일 : 월배치 이므로 수집기준일은 금월로 설정한다
//        String baseDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 수집기준년도 : 전년도, 전전년도 2년치
//        String baseYear = null;
//        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd"));
//        if(day.equals("1")) {
//            baseYear = LocalDateTime.now().minusYears(1).format(DateTimeFormatter.ofPattern("yyyy"));
//        } else {
//            baseYear = LocalDateTime.now().minusYears(2).format(DateTimeFormatter.ofPattern("yyyy"));
//        }
//        int day = Integer.getInteger(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd")));
//        if (day > 0 || day < 7) {
//            baseYear = LocalDateTime.now().minusYears(1).format(DateTimeFormatter.ofPattern("yyyy"));
//        } else {
//            baseYear = LocalDateTime.now().minusYears(2).format(DateTimeFormatter.ofPattern("yyyy"));
//        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addString("baseYear", baseYear)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(uctJob(), jobParameters);
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
//                .start(uctFileCleanStep(null)) // temp 파일 삭제
                .start(uct001mStep())
//                .start(uct001mPartitionStep(null))
//                .next(uctFileMergeStep(null, null))
//                .start(uct001mStep())
//                .on("COMPLETED")
//                .to(uctFileMergeStep(null, null))
                .on("*")
                .end()
//                .from(uct001mPartitionStep(null))
//                .on("*")
//                .to(uctFileMergeStep(null))
//                .next(uctFileMergeStep(null, null)) // temp 파일 병합
                .end()
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
            @Value("#{jobParameters[baseYear]}") String baseYear,
            @Value("#{stepExecutionContext[threadNum]}") String threadNum,
            @Value("#{stepExecutionContext[partitionList]}") List<String> partitionList) {

        return new Uct001mTasklet();
    }

    /**************************************************************************************************
     * 공통 Step
     **************************************************************************************************/

    /**
     * MultiThread로 생성된 파일을 병합하기 위한 FileMergeStep 설정
     *
     * @param jobId
     * @return
     */
    @Bean
    @JobScope
    public Step uctFileMergeStep(
            @Value("#{jobExecutionContext[jobId]}") String jobId,
            @Value("#{jobExecutionContext[baseYearList]}") List<String> baseYearList) {

        return stepBuilderFactory.get(CmmnConst.JOB_GRP_ID_UCT + CmmnConst.POST_FIX_FILE_MERGE_STEP)
                .tasklet(uctFileMergeTasklet(null, null))
                .build();
    }

    /**
     * MultiThread로 생성된 파일을 병합하기 위한 FileMergeTasklet 설정
     *
     * @param jobId
     * @return
     */
    @Bean
    @StepScope
    public Uct001mMergeTasklet uctFileMergeTasklet(
            @Value("#{jobExecutionContext[jobId]}") String jobId,
            @Value("#{jobExecutionContext[baseYearList]}") List<String> baseYearList) {
//        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE);
        return new Uct001mMergeTasklet();
    }

    /**
     * Temp폴더 삭제를 위한 FileCleanStep 설정
     *
     * @param jobId
     * @return
     */
    @Bean
    @JobScope
    public Step uctFileCleanStep(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        return stepBuilderFactory.get(CmmnConst.JOB_GRP_ID_UCT + CmmnConst.POST_FIX_FILE_CLEAN_STEP)
                .tasklet(uctFileCleanTasklet(null))
                .build();
    }

    /**
     * Temp폴더 삭제를 위한 FileCleanTasklet 설정
     *
     * @param jobId
     * @return
     */
    @Bean
    @StepScope
    public CmmnFileTasklet uctFileCleanTasklet(@Value("#{jobExecutionContext[jobId]}") String jobId) {

        ArrayList<String> list = new ArrayList<>();
        list.add(CmmnConst.JOB_ID_UCT001M);
        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN, list);
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
