package kcs.edc.batch;

import kcs.edc.batch.cmmn.property.ApiProperty;
import kcs.edc.batch.cmmn.property.FileProperty;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing // 배치 기능 활성화
@EnableScheduling // 스케쥴링 기능 활성화
@EnableConfigurationProperties({ApiProperty.class, FileProperty.class})
public class EdcBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdcBatchApplication.class, args);
    }

}
