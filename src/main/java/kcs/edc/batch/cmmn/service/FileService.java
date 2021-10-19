package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.ApiProperty;
import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
public class FileService {

    @Autowired
    protected FileProperty fileProperty;

    @Autowired
    protected ApiProperty apiProperty;

    private String PREFIX_TABLE_NAME = "ht_";
    private String LOG_TABLE_NAME = "ht_log001m";

    private String rootPath;

    private String jobId;

    private String cletDt;
    private ApiProperty.JobProp jobProp;


    /**
     * 테이블명 조회 ex) nav001m -> ht_nav001m
     *
     * @return tableName 테이블명
     */
    public String getTableName() {
        return PREFIX_TABLE_NAME + jobId;
    }

    /**
     * 수집파일 경로 조회 return ex) /hdata/ht_nav001m/
     *
     * @return dataFilePath 수집파일경로
     */
    public String getDataFilePath() {
        return rootPath + getTableName() + "/";
    }

    /**
     * 수집파일명 조회 ex)ht_nav001m_20210826090407.txt
     *
     * @return dataFileName 수집파일명
     */
    public String getDataFileName() {
        return getTableName() + "_" + cletDt + ".txt";
    }

    public String getDataFileFullPath() {
        return getDataFilePath() + getDataFileName();
    }

    /**
     * 로그파일 경로 조회 ex) /hdata/ht_log001m/
     *
     * @return logFilePath 로그파일경로
     */
    public String getLogFilePath() {
        return rootPath + LOG_TABLE_NAME + "/";
    }

    /**
     * 로그파일명 조회 ex)ht_nav001m_20210826090407.txt
     *
     * @return logFileName 로그파일명
     */
    public String getLogFileName() {
        return getTableName() + "_" + DateUtil.getCurrentTime2() + ".txt";
    }

    /**
     * 로그파일 전체 경로 조회
     *
     * @return logFileFullPath 로그파일 전체경로
     */
    public String getLogFileFullPath() {
        return getLogFilePath() + getLogFileName();
    }

    /**
     * 임시파일 경로 조회
     *
     * @return tempFilePath 임시파일경로
     */
    public String getTempFilePath() {
        return getDataFilePath() + "temp/";
    }

    /**
     * 임시파일명 조회
     *
     * @return tempFileName 임시파일명
     */
    public String getTempFileName() {
        return getTableName() + "_" + DateUtil.getCurrentTime2() + ".txt";
    }

    /**
     * 임시파일 전체경로 조회
     *
     * @return tempFileFullPath 임시파일 전체경로
     */
    public String getTempFileFullPath() {
        return getTempFilePath() + getTempFileName();
    }
}
