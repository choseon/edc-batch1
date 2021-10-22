package kcs.edc.batch.cmmn.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CmmnFileVO {

    /**
     * 로그 테이블명
     */
    private String LOG_TABLE_NAME = "ht_log001m";

    /**
     * 테이블명 패턴 : ht_[jobId] ex) ht_nav003m
     */
    private String TABLE_NAME_PATTERN = "ht_%s";

    /**
     * 파일명 패턴 : ht_[jobId]_[baseDt].txt ex) ht_nav003m_20211020.txt
     */
    private String FILE_NAME_PATTERN = "ht_%s_%s.txt";

    /**
     * 임시파일명 패턴 : ht_[jobId]_[baseDt]_[ThreadNum].txt ex) ht_nav003m_20211020_1.txt
     */
    private String TEMP_FILE_NAME_PATTERN = "ht_%s_%s_%s.txt";

    /**
     * 경로 패턴 : [저장경로]/[테이블명]/ ex) /hdata/ht_nav003m/
     */
    private String PATH_PATTERN = "%s/%s/";

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

    /**
     * 수집 기준일 ex) 20211020
     */
    private String baseDt;

    /**
     * 테이블명
     */
    private String tableName;

    /**
     * 수집데이터파일 경로
     */
    private String dataFilePath;

    /**
     * 수집파일명
     */
    private String dataFileName;

    /**
     * 첨부파일 경로
     */
    private String resourcePath;

    /**
     * 첨부파일명
     */
    private String resourceName;

    /**
     * 임시파일경로
     */
    private String tempFilePath;

    /**
     * 임시파일명
     */
    private String tempFileName;

    /**
     * 로그파일경로
     */
    private String logFilePath;

    /**
     * 로그파일명
     */
    private String logFileName;

    public CmmnFileVO(String jobId) {
        this.jobId = jobId;
        this.tableName = String.format(this.TABLE_NAME_PATTERN, this.jobId);
    }
}
