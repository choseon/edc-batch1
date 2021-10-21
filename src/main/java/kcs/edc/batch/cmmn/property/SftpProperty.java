package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@Configuration
@ConfigurationProperties(prefix = "sftp")
public class SftpProperty {

    private Map<String, JobProp> jobs = new HashMap<>();

    @Getter
    @Setter
    public static class JobProp {
        private String host;
        private int port;
        private String user;
        private String password;
        private String remoteFilePath;
        private String remoteFileName;
        private String downloadFilePath;
    }

    public SftpProperty.JobProp getCurrentJobProp(String jobName) {
        return this.jobs.get(jobName);
    }


}
