package kcs.edc.batch.config;


import kcs.edc.batch.jobs.big.issue.Big002mTasklet;
import kcs.edc.batch.jobs.big.news.Big001mTasklet;
import kcs.edc.batch.jobs.big.ranking.Big005mTasklet;
import kcs.edc.batch.jobs.big.timeline.Big004mTasklet;
import kcs.edc.batch.jobs.big.wordcloud.Big003mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BIGJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final JobLauncher jobLauncher;

    //    @Scheduled(cron = "*/5 * * * * ?") // 5초마다
    public void scheduler() throws Exception {
//        JobParameters jobParameters = new JobParametersBuilder().addString("baseDt", LocalDateTime.now().toString()).toJobParameters();
//        jobLauncher.run(bigJob(),jobParameters);
    }

//    @Bean
    public Job bigJob() {

        // News TimeLine -> Word Cloud -> News Search
        Flow bigFlow1 = new FlowBuilder<Flow>("bigFlow1")
                .start(big004mStep(null)) // News Timeline
                .next(big003mStep(null, null, null, null)) // Word Cloud
                .next(big001mStep(null, null, null, null,null)) // News Search
                .build();

        // query_rank(인기검색어) -> Word Cloud -> News Search
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

    @Bean
    @JobScope
    public Step big001mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn,
            @Value("#{jobExecutionContext[newsClusterList]}") List<Object> newsClusterList) {

        return stepBuilderFactory.get("big001mStep")
                .tasklet(big001mTasklet(null, null, null, null))
                .build();
    }

    @Bean
    @StepScope
    public Big001mTasklet big001mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[keywordList]}") List<Object> keywordList,
            @Value("#{jobExecutionContext[kcsRgrsYn]}") List<Object> kcsRgrsYn,
            @Value("#{jobExecutionContext[issueSrwrYn]}") List<Object> issueSrwrYn) {
        return new Big001mTasklet();
    }

    @Bean
    @JobScope
    public Step big002mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt) {

        return stepBuilderFactory.get("big002mStep")
                .tasklet(big002mTasklet(null))
                .build();
    }

    @Bean
    @StepScope
    public Big002mTasklet big002mTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big002mTasklet();
    }

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
     * 한국언론진흥재단 빅카인드 Tasklet
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
     * 한국언론진흥재단 빅카인드 Step
     */
    @Bean
    @JobScope
    public Step big004mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("big004mStep")
                .tasklet(big004mTasklet(baseDt))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 Tasklet
     */
    @Bean
    @StepScope
    public Big004mTasklet big004mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big004mTasklet();
    }

    /**
     * 한국언론진흥재단 빅카인드 Step
     */
    @Bean
    @JobScope
    public Step big005mStep(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("big005mStep")
                .tasklet(big005mTasklet(baseDt))
                .build();
    }

    /**
     * 한국언론진흥재단 빅카인드 Tasklet
     */
    @Bean
    @StepScope
    public Big005mTasklet big005mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Big005mTasklet();
    }
}
