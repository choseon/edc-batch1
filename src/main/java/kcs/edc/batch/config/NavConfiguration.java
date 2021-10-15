package kcs.edc.batch.config;

import kcs.edc.batch.jobs.nav.nav003m.Nav003mTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NavConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

//    @Bean
    public Job navJob() {
        return jobBuilderFactory.get("navJob")
                .start(nav003mStep())
                .build();
    }

    @Bean
    @JobScope
    public Step nav003mStep() {
        return stepBuilderFactory.get("nav003mStep")
                .tasklet(nav003mTasklet())
                .build();
    }

    @Bean
    @StepScope
    public Tasklet nav003mTasklet() {
        return new Nav003mTasklet();
    }
}
