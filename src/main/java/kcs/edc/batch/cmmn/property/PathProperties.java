package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
//@ConfigurationProperties(prefix = "file")
public class PathProperties {

    private Map<String, Map<String, String>> storePath;

    private String resourcePath;

    private String configPath;
}
