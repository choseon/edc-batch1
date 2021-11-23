package kcs.edc.batch.cmmn.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileProperty {

    /**
     * 리소스파일 경로
     */
    private String resourcePath;

    /**
     * 파일 루트경로
     */
    private String rootPath;

    /**
     * 첨부파일 루트경로
     */
    private String attachRootPath;

    /**
     * 로그파일 디렉토리명
     */
    private String logDirName;

    /**
     * 임시파일 디렉토리명
     */
    private String tempDirName;

    /**
     * 데이터파일 테이블명 접두어(ht_)
     */
    private String dataFilePrefixName;

    /**
     * 파일 확장자
     */
    private String dataFileExtension;

    /**
     * 첨부파일 디렉토리명
     */
    private Map<String, String> attachDirName;

}
