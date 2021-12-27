package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * application.yml 설정에서 Sftp 정보를 자동으로 매핑하는 클래스
 */
@Getter
@Setter
@Component
@Configuration
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    private Map<String, SftpProp> jobs = new HashMap<>();

    @Getter
    @Setter
    public static class SftpProp {

        private String host;
        private int port;
        private String user;
        private String password;
        private String remoteFilePath;
        private String remoteFileName;
        private String downloadFilePath;
    }

    public SftpProp getCurrentJobProp(String jobName) {
        return this.jobs.get(jobName);
    }

}
