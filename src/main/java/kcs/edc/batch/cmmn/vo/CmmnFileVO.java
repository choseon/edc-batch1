package kcs.edc.batch.cmmn.vo;

import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CmmnFileVO {

    private FileVO dataFileVO;
    private FileVO tempFileVO;
    private FileVO logFileVO;
    private FileVO attachFileVO;

    private String FILE_ROOT_PATH;

    /**
     * 테이블명 접두어
     */
    private String PRE_FIX_TABLE_NAME;
    /**
     * 로그 테이블명
     */
    private String LOG_DIR_NAME;

    /**
     * 임시파일 폴더명
     */
    private String TEMP_DIR_NAME;

    /**
     * 파일 확장자
     */
    private String FILE_EXTENSION;

    private String ATTACH_PATH;

    private String ATTACH_DIR_NAME;

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

    /**
     * 테이블명
     */
    private String dataDirName;

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

    public CmmnFileVO(FileProperty fileProp, String jobId) {
        this.jobId = jobId;
        this.FILE_ROOT_PATH = fileProp.getFileRootPath();
        this.PRE_FIX_TABLE_NAME = fileProp.getDataFilePrefixName();
        this.LOG_DIR_NAME = fileProp.getLogDirName();
        this.TEMP_DIR_NAME = fileProp.getTempDirName();
        this.FILE_EXTENSION = fileProp.getDataFileExtension();


        // 데이터 디렉토리명
        this.dataDirName = this.PRE_FIX_TABLE_NAME + this.jobId;

        // 수집데이터파일 경로
        this.dataFilePath = this.FILE_ROOT_PATH + this.dataDirName + "/";

        // 로그파일 경로
        this.logFilePath = this.FILE_ROOT_PATH + this.LOG_DIR_NAME + "/";

        // 임시파일경로
        this.tempFilePath = this.dataFilePath + this.TEMP_DIR_NAME + "/";

        // 첨부파일 경로
        String jobGroupId = jobId.substring(0,3);
        if(fileProp.getAttachDirName().containsKey(jobGroupId)) {
            this.ATTACH_PATH = fileProp.getAttachPath();
            this.ATTACH_DIR_NAME = fileProp.getAttachDirName().get(jobGroupId);
            this.attachedFilePath = this.ATTACH_PATH + this.ATTACH_DIR_NAME + "/";
        }

        // 로그파일명
        this.logFileName = this.dataDirName + "_" + DateUtil.getCurrentTime2() + "." + this.FILE_EXTENSION;
    }

    /**
     * 데이터수집 파일명
     * @param dataFileName
     */
    public void setDataFileName(String dataFileName) {
        this.dataFileName = this.dataDirName + "_" + dataFileName + "." + this.FILE_EXTENSION;
    }

    public void setTempFileName(String tempFileName) {
        this.tempFileName = this.dataDirName + "_" + tempFileName + "." + this.FILE_EXTENSION;
    }

}
