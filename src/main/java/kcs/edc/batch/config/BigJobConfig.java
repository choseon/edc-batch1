package kcs.edc.batch.config;


import kcs.edc.batch.cmmn.jobs.CmmnFileTasklet;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.jobs.big.issue.Big002mTasklet;
import kcs.edc.batch.jobs.big.news.Big001mTasklet;
import kcs.edc.batch.jobs.big.ranking.Big005mTasklet;
import kcs.edc.batch.jobs.big.timeline.Big004mTasklet;
import kcs.edc.batch.jobs.big.wordcloud.Big003mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
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
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

/**
 * 한국언론진흥재단 빅카인드 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BigJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${scheduler.jobs.big.isActive}")
    private Boolean isActive;

    @Scheduled(cron = "${scheduler.jobs.big.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        try {
            this.jobLauncher.run(bigJob(), new JobParameters());
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
     * 한국언론진흥재단 빅카인드 데이터수집 Batch Job 설정
     *
     * @return
     */
    @Bean
    public Job bigJob() {

        // News TimeLine(뉴스타임라인) -> Word Cloud(워드클라우드) -> News Search(뉴스조회)
        Flow bigFlow1 = new FlowBuilder<Flow>("bigFlow1")
                .start(big004mStep(null)) // News Timeline
                .next(big003mStep(null, null, null, null)) // Word Cloud
                .next(big001mStep(null, null, null, null, null)) // News Search
                .build();

        // Issue Rank(이슈랭킹) -> News Search(뉴스상세조회)
        Flow bigFlow2 = new FlowBuilder<Flow>("bigFlow2")
                .start(big002mStep(null)) // Issue Rank
                .next(big001mStep(null, null, null, null, null)) // News Search
                .build();

        // query_rank(인기검색어) -> Word Cloud(워드클라우드) -> News Search(뉴스조회)
        Flow bigFlow3 = new FlowBuilder<Flow>("bigFlow3")
                .start(big005mStep(null)) // query_rank
                .next(big003mStep(null, null, null, null)) // Word Cloud
                .next(big001mStep(null, null, null, null, null)) // News Search
                .build();

        return jobBuilderFactory.get("bigJob")
                .start(bigFlow1)
                .next(bigFlow2)
                .next(bigFlow3)
                .next(bigFileMergeStep()) // 임시파일병합
                .end()
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스검색 수집 Step 설정
     *
     * @param baseDt
     * @param keywordList
     * @param kcsRgrsYn
     * @param issueSrwrYn
     * @param newsClusterList
     * @return
     */
    @Bean
    @JobScope
    public Step big001mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn,
            @Value("#{jobExecutionContext[newsClusterList]}") List<List<Object>> newsClusterList) {

        return stepBuilderFactory.get("big001mStep")
                .tasklet(big001mTasklet(null, null, null, null, null))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스검색 수집 Tasklet 설정
     *
     * @param baseDt
     * @param keywordList
     * @param kcsRgrsYn
     * @param issueSrwrYn
     * @return
     */
    @Bean
    @StepScope
    public Big001mTasklet big001mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn,
            @Value("#{jobExecutionContext[newsClusterList]}") List<List<Object>> newsClusterList) {
        return new Big001mTasklet();
    }

    /**
     * 한국언론진흥재단 빅카인드 이슈랭킹 수집 Step 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @JobScope
    public Step big002mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt) {

        return stepBuilderFactory.get("big002mStep")
                .tasklet(big002mTasklet(null))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 이슈랭킹 수집 Tasklet 설정
     *
     * @param baseDt
     * @return
     */
    @Bean
    @StepScope
    public Big002mTasklet big002mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big002mTasklet();
    }

    /**
     * 한국언론진흥재단 빅카인드 워드클라우드 수집 Step 설정
     *
     * @param baseDt
     * @param keywordList
     * @param kcsRgrsYn
     * @param issueSrwrYn
     * @return
     */
    @Bean
    @JobScope
    public Step big003mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn) {
        return stepBuilderFactory.get("big003mStep")
                .tasklet(big003mTasklet(null, null, null, null))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 워드클라우드 수집 Tasklet 설정
     */
    @Bean
    @StepScope
    public Big003mTasklet big003mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn) {
        return new Big003mTasklet();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스타임라인 수집 Step 설정
     */
    @Bean
    @JobScope
    public Step big004mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("big004mStep")
                .tasklet(big004mTasklet(baseDt))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스타임라인 수집 Tasklet 설정
     */
    @Bean
    @StepScope
    public Big004mTasklet big004mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big004mTasklet();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스타임라인 수집 Step 설정
     */
    @Bean
    @JobScope
    public Step big005mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("big005mStep")
                .tasklet(big005mTasklet(baseDt))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 뉴스타임라인 수집  Tasklet 설정
     */
    @Bean
    @StepScope
    public Big005mTasklet big005mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big005mTasklet();
    }


    /**
     * 한국언론진흥재단 빅카인드 파일 병합 Step 설정
     * @return
     */
    @Bean
    @JobScope
    public Step bigFileMergeStep() {

        return stepBuilderFactory.get(CmmnConst.JOB_GRP_ID_BIG + CmmnConst.POST_FIX_FILE_MERGE_STEP)
                .tasklet(bigFileMergeTasklet(null))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 파일병합 Tasklet 설정
     * @return
     */
    @Bean
    @StepScope
    public CmmnFileTasklet bigFileMergeTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        List<String> jobList = new ArrayList<>();
        jobList.add(CmmnConst.JOB_ID_BIG001M);
        jobList.add(CmmnConst.JOB_ID_BIG003M);
        jobList.add(CmmnConst.JOB_ID_BIG004M);
        return new CmmnFileTasklet(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE, jobList);
    }
}
