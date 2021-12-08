package kcs.edc.batch.config;

import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.saf.saf001l.Saf001lTasklet;
import kcs.edc.batch.jobs.saf.saf001m.Saf001mTasklet;
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
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 국가기술표준원 제품안전정보센터 데이터수집 Batch Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SafJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;

    @Value("${scheduler.saf.isActive}")
    private Boolean isActive;

    @Value("${scheduler.saf.baseline}")
    private String baseline;

    /**
     * 국가기술표준원 제품안전정보센터 데이터수집 Batch Launcher 설정
     *
     * @throws Exception
     */
    @Scheduled(cron = "${scheduler.saf.cron}")
    public void launcher() {

        log.info(">>>>> {} launcher..... isActive: {}", this.getClass().getSimpleName().substring(0, 6), this.isActive);
        if (!this.isActive) return;

        String baseDt = DateUtil.getBaseLineDate(this.baseline);
        log.info("baseline: {}, baseDt: {}", this.baseline, baseDt);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDt", baseDt)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobLauncher.run(safJob(), jobParameters);
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
     * 국가기술표준원 제품안전정보센터 데이터수집 Batch Job 설정
     *
     * @return
     */
    @Bean
    public Job safJob() {

        return jobBuilderFactory.get("safJob")
                .start(saf001mStep(null))
                .next(saf001lStep(null, null))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 제품안전정보 수집 Step 설정
     */
    @Bean
    @JobScope
    public Step saf001mStep(
            @Value("#{jobParameters[baseDt]}") String baseDt) {
        return stepBuilderFactory.get("saf001mStep")
                .tasklet(saf001mTasklet(baseDt))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터 제품안전정보 수집 Tasklet 설정
     */
    @Bean
    @StepScope
    public Saf001mTasklet saf001mTasklet(@Value("#{jobParameters[baseDt]}") String baseDt) {
        return new Saf001mTasklet();
    }

    /**
     * 국가기술표준원 제품안전정보센터
     * 제품안전정보 파생모델목록 수집, 연관인증번호 목록 수집, 제조공장목록 수집, 이미지목록 수집
     * Step 설정
     */
    @Bean
    @JobScope
    public Step saf001lStep(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {

        return stepBuilderFactory.get("saf001lStep")
                .tasklet(saf001lTasklet(baseDt, certNumList))
                .build();
    }

    /**
     * 국가기술표준원 제품안전정보센터
     * 제품안전정보 파생모델목록 수집, 연관인증번호 목록 수집, 제조공장목록 수집, 이미지목록 수집
     * Tasklet 설정
     */
    @Bean
    @StepScope
    public Saf001lTasklet saf001lTasklet(
            @Value("#{jobParameters[baseDt]}") String baseDt,
            @Value("#{jobExecutionContext[certNumList]}") List<String> certNumList) {
        return new Saf001lTasklet();
    }

}
