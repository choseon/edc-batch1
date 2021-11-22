package kcs.edc.batch.config;


import kcs.edc.batch.jobs.big.issue.Big002mTasklet;
import kcs.edc.batch.jobs.big.news.Big001mTasklet;
import kcs.edc.batch.jobs.big.ranking.Big005mTasklet;
import kcs.edc.batch.jobs.big.timeline.Big004mTasklet;
import kcs.edc.batch.jobs.big.wordcloud.Big003mTasklet;
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
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Value("${scheduler.big.isActive}")
    private Boolean isActive;

    /**
     * 한국언론진흥재단 빅카인드 데이터수집 Batch launcher 설정
     */
    @Scheduled(cron = "${scheduler.big.cron}")
    public void launcher() {

        log.info("BigJobConfig launcher...");
        log.info("isActive: {}", this.isActive);
        if (!this.isActive) return;

        try {
            String baseDt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDt", baseDt)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(bigJob(), jobParameters);

        } catch (JobExecutionAlreadyRunningException e) {
            log.error(e.getMessage());
        } catch (JobRestartException e) {
            log.error(e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error(e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.error(e.getMessage());
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

        // query_rank(인기검색어) -> Word Cloud(워드클라우드) -> News Search(뉴스조회)
        Flow bigFlow2 = new FlowBuilder<Flow>("bigFlow2")
                .start(big005mStep(null)) // query_rank
                .next(big003mStep(null, null, null, null)) // Word Cloud
                .next(big001mStep(null, null, null, null, null)) // News Search
                .build();

        // Issue Rank(이슈랭킹) -> News Search(뉴스상세조회)
        Flow bigFlow3 = new FlowBuilder<Flow>("bigFlow3")
                .start(big002mStep(null)) // Issue Rank
                .next(big001mStep(null, null, null, null, null)) // News Search
                .build();

        return jobBuilderFactory.get("bigJob")
                .start(bigFlow1)
                .next(bigFlow2)
                .next(bigFlow3)
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
            @Value("#{jobExecutionContext[newsClusterList]}") List<Object> newsClusterList) {

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
            @Value("#{jobExecutionContext[newsClusterList]}") List<Object> newsClusterList) {
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
}
