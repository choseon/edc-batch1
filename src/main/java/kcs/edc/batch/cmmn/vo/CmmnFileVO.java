package kcs.edc.batch.cmmn.vo;

import kcs.edc.batch.cmmn.property.FileProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CmmnFileVO {

    private String ROOT_PATH;

    /**
     * 테이블명 접두어
     */
    private String PRE_FIX_TABLE_NAME;
    /**
     * 로그 테이블명
     */
    private String LOG_TABLE_NAME;

    /**
     * 임시파일 폴더명
     */
    private String TEMP_DIR_NAME;

    /**
     * 파일 확장자
     */
    private String FILE_EXTENSION;

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

    /**
     * 첨부파일 경로
     */
    private String attachedFilePath;

    /**
     * 첨부파일명
     */
    private String attachedFileName;

    public CmmnFileVO(FileProperties fileProp, String jobId) {
        this.jobId = jobId;
        this.ROOT_PATH = fileProp.getDataPath();
        this.PRE_FIX_TABLE_NAME = fileProp.getPrefixTableName();
        this.LOG_TABLE_NAME = fileProp.getLogTableName();
        this.TEMP_DIR_NAME = fileProp.getTempDirName();
        this.FILE_EXTENSION = fileProp.getFileExtension();

        // 테이블명
        this.tableName = this.PRE_FIX_TABLE_NAME + this.jobId;
        // 수집데이터파일 경로
        this.dataFilePath = this.ROOT_PATH + this.tableName + "/";

        // 로그파일 경로
        this.logFilePath = fileProp.getLogPath();

        // 임시파일경로
        this.tempFilePath = this.dataFilePath + this.TEMP_DIR_NAME + "/";

        // 로그파일명
        this.logFileName = this.tableName + "_" + DateUtil.getCurrentTime2() + "." + this.FILE_EXTENSION;

        // 첨부파일 경로
        String jobGroupId = jobId.substring(0,3);
        if(fileProp.getAttachPath().containsKey(jobGroupId)) {
            this.attachedFilePath = fileProp.getAttachPath().get(jobGroupId);
        }
    }

    /**
     *
     * @param fileProp
     * @param isPortal
     * @param jobGroupId
     * @param jobId
     */
/*    public CmmnFileVO(FileProperties.FileProp fileProp, Boolean isPortal, String jobGroupId, String jobId) {
        this.jobId = jobId;

        Map<String, String> rootPath = fileProp.getRootPath();
        this.ROOT_PATH = isPortal ? rootPath.get(jobGroupId) : rootPath.get("all");
        this.PRE_FIX_TABLE_NAME = fileProp.getPrefixTableName();
        this.LOG_TABLE_NAME = fileProp.getLogTableName();
        this.TEMP_DIR_NAME = fileProp.getTempPath();
        this.FILE_EXTENSION = fileProp.getFileExtension();


        if(isPortal) {
            // 테이블명
            this.tableName = this.PRE_FIX_TABLE_NAME + this.jobId.substring(0,3) + "_" + this.jobId.substring(3);
            // 수집데이터파일 경로
            this.dataFilePath = this.ROOT_PATH + fileProp.getDataFileDir() + "/" + this.tableName + "/";
            // 첨부파일 경로
            this.attachedFilePath = this.ROOT_PATH + fileProp.getAttachedFileDir() + "/";

        } else {
            // 테이블명
            this.tableName = this.PRE_FIX_TABLE_NAME + this.jobId;
            // 수집데이터파일 경로
            this.dataFilePath = this.ROOT_PATH + this.tableName + "/";
        }

        // 로그파일 경로
        this.logFilePath = fileProp.getLogPath();
        // 임시파일경로
        this.tempFilePath = this.dataFilePath + this.TEMP_DIR_NAME + "/";

        // 로그파일명
        this.logFileName = this.tableName + "_" + DateUtil.getCurrentTime2() + "." + this.FILE_EXTENSION;
    }*/

    /**
     * 데이터수집 파일명
     * @param dataFileName
     */
    public void setDataFileName(String dataFileName) {
        this.dataFileName = this.tableName + "_" + dataFileName + "." + this.FILE_EXTENSION;
    }

    public void setTempFileName(String tempFileName) {
        this.tempFileName = this.tableName + "_" + tempFileName + "." + this.FILE_EXTENSION;
    }

}
