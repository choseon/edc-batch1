package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@Configuration
@ConfigurationProperties(prefix = "path")
public class FileProperty {

    private String rootPath;

    private String storePath;

    private String resourcePath;

    private String configPath;

    private String logPath;

//    private Map<String, Object> jobPath;



}
