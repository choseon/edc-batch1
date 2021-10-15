package kcs.edc.batch.config;

import kcs.edc.batch.jobs.biz.biz001m.Biz001mTasklet;
import kcs.edc.batch.jobs.kot.kot001m.Kot001mTasklet;
import kcs.edc.batch.jobs.kot.kot002m.Kot002mTasklet;
import kcs.edc.batch.jobs.saf.saf001l.Saf001lTasklet;
import kcs.edc.batch.jobs.saf.saf001m.Saf001mTasklet;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfig {

    public static final String JOB_ID_BIZ001M = "biz001m";

    public static final String JOB_ID_KOT001M = "kot001m";
    public static final String JOB_ID_KOT002M = "kot002m";

    public static final String JOB_ID_SOM001M = "som001m";
    public static final String JOB_ID_SOM002M = "som002m";
    public static final String JOB_ID_SOM003M = "som003m";
    public static final String JOB_ID_SOM004M = "som004m";
    public static final String JOB_ID_SOM005M = "som005m";

    public static final String JOB_ID_SAF001M = "saf001m";
    public static final String JOB_ID_SAF001L = "saf001l";
    public static final String JOB_ID_SAF002L = "saf002l";
    public static final String JOB_ID_SAF003L = "saf003l";
    public static final String JOB_ID_SAF004L = "saf004l";

    public static final String JOB_ID_BIG001M = "big001m";
    public static final String JOB_ID_BIG002M = "big002m";
    public static final String JOB_ID_BIG003M = "big003m";
    public static final String JOB_ID_BIG004M = "big004m";

    public static final String JOB_ID_UCT001M = "uct001m";

    public static final String PREFIX_STEP = "step";
    public static final String PREFIX_JOB = "job";

    public static final int GRID_SIZE = 10;

    @Autowired
    Environment environment;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

//    @Scheduled(cron = "${scheduler.cron}")
//    public void scheduler() throws Exception {
//        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
//        jobLauncher.run(dailyJob(),jobParameters);
//    }

    /**
     * 일배치 JOB 설정
     *
     * @return
     */
//    @Bean
    public Job dailyJob() {

        Flow safFlow = new FlowBuilder<Flow>("safFlow")
                .start(saf001mStep(null))
                .next(saf001lStep(null, null))
                .build();

        Flow kotFlow = new FlowBuilder<Flow>("kotFlow")
                .start(kot001mStep(null))
                .next(kot002mStep(null, null))
                .build();

        return jobBuilderFactory.get("dailyJob")
                .start(safFlow)
//                .next(kotFlow)
//                .next(biz001mStep(null))
                .end()
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 Setp
     */
    @Bean
    @JobScope
    public Step biz001mStep(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return stepBuilderFactory.get("biz001mStep")
                .tasklet(biz001mTasklet(cletDt))
                .build();
    }

    /**
     * 중소기업연구원 중소벤처기업부 기업마당 Tasklet
     */
    @Bean
    @StepScope
    public Biz001mTasklet biz001mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Biz001mTasklet();
    }


    /**
     * 국가기술표준원 제품안전정보센터 Step
     */
    @Bean
    @JobScope
    public Step saf001mStep(
            @Value("#{jobParameters[cletDt]}") String cletDt) {
        return stepBuilderFactory.get("saf001mStep")
                .tasklet(saf001mTasklet(cletDt))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Tasklet
     */
    @Bean
    @StepScope
    public Saf001mTasklet saf001mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Saf001mTasklet();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Step
     */
    @Bean
    @JobScope
    public Step saf001lStep(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {
        return stepBuilderFactory.get("saf001lStep")
                .tasklet(saf001lTasklet(cletDt, certNumList))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 Tasklet
     */
    @Bean
    @StepScope
    public Saf001lTasklet saf001lTasklet(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {
        return new Saf001lTasklet();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Step
     */
    @Bean
    @JobScope
    public Step kot001mStep(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return stepBuilderFactory.get("kot001mStep")
                .tasklet(kot001mTasklet(cletDt))
                .build();
    }

    /**
     * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
     */
    @Bean
    @StepScope
    public Kot001mTasklet kot001mTasklet(@Value("#{jobParameters[cletDt]}") String cletDt) {
        return new Kot001mTasklet();
    }

    @Bean
    @JobScope
    public Step kot002mStep(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[resultList]}") List<Object> resultList) {
        return stepBuilderFactory.get("kot002mStep")
                .tasklet(kot002mTasklet(cletDt, resultList))
                .build();
    }

    @Bean
    @StepScope
    public Kot002mTasklet kot002mTasklet(
            @Value("#{jobParameters[cletDt]}") String cletDt,
            @Value("#{jobExecutionContext[resultList]}") List<Object> resultList) {
        return new Kot002mTasklet();
    }

}
