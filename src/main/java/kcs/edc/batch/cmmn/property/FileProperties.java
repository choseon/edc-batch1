package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "path")
public class FileProperties {

    private String rootPath;

    private String storePath;

    private Map<String, Map<String, String>> nasStroePath;

    private String resourcePath;

    private String configPath;

    private String logPath;

}
*/


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "file.path")
public class FileProperties {

//    private Map<String, Map<String, String>> storePath;

    private String resourcePath;

    private String hiveStorePath;

    private String nasStorePath;

}
