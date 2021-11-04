package kcs.edc.batch.cmmn.vo;

import kcs.edc.batch.cmmn.util.DateUtil;

public class PortalFileVO {

    private String rootPath;

    /**
     * 테이블명 접두어
     */
    private String PRE_FIX_TABLE_NAME = "tb_bdp_";
    /**
     * 로그 테이블명
     */
    private String LOG_TABLE_NAME = "ht_log001m";

    /**
     * 임시파일 폴더명
     */
    private String TEMP_FILE_PATH = "temp";

    /**
     * 파일 확장자
     */
    private String FILE_EXTENSION = "txt";

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

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



    public PortalFileVO(String rootPath, String jobId) {
        this.rootPath = rootPath;
        this.jobId = jobId;
        // 테이블명
        this.tableName = this.PRE_FIX_TABLE_NAME + getTiberoTableName(this.jobId);
        // 수집데이터파일 경로
        this.dataFilePath = this.rootPath + this.tableName + "/";
        // 로그파일 경로
        this.logFilePath = this.rootPath + this.LOG_TABLE_NAME + "/";
        // 임시파일경로
        this.tempFilePath = this.dataFilePath + this.TEMP_FILE_PATH + "/";

        // 로그파일명
        this.logFileName = this.tableName + "_" + DateUtil.getCurrentTime2() + "." + this.FILE_EXTENSION;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = this.tableName + "_" + dataFileName + "." + this.FILE_EXTENSION;
    }

    public void setTempFileName(String tempFileName) {
        this.tempFileName = this.tableName + "_" + tempFileName + "." + this.FILE_EXTENSION;
    }

    public String getTiberoTableName(String jobId) {
        return jobId.substring(0,3) + "_" + jobId.substring(4);
    }
}
