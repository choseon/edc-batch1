package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.CmmnFileVO;
import kcs.edc.batch.cmmn.vo.FileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FileService {

    private String LOG_FILE_STEP = "EXT_FILE_CREATE";

    @Autowired
    private FileProperty fileProperty;

    private CmmnFileVO fileVO;

    private String jobGroupId;

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

    /**
     * 수집 기준일 ex) 20211020
     */
    private String baseDt;

    /**
     * 배치 시작일시 ex) 20211020 09:23:11
     */
    public String startTime;

    private FileVO dataFileVO;
    private FileVO tempFileVO;
    private FileVO logFileVO;
    private FileVO attachFileVO;

    /**
     * 초기화
     *
     * @param jobGroupId
     * @param jobId
     * @param baseDt
     */
    public void init(String jobGroupId, String jobId, String baseDt) {
        this.jobGroupId = jobGroupId;
        this.jobId = jobId;
        this.baseDt = baseDt;
        this.startTime = DateUtil.getCurrentTime();

        initFileVO(this.jobId);
        log.info("FileService init() >> jobId: {}", this.jobId);
    }

    public void initFileVO(String jobId) {
        this.fileVO = new CmmnFileVO(this.fileProperty, this.jobId);

        String fileRootPath = this.fileProperty.getFileRootPath();
        String dataFilePrefixName = this.fileProperty.getDataFilePrefixName();
        String logDirName = this.fileProperty.getLogDirName();
        String tempDirName = this.fileProperty.getTempDirName();
        String dataFileExtension = this.fileProperty.getDataFileExtension();
        // 데이터 디렉토리명
        String dataDirName = dataFilePrefixName + jobId;
        // 로그파일명
        String logFileName = dataDirName + "_" + DateUtil.getCurrentTime2() + "." + dataFileExtension;

        FileVO dataFileVO = new FileVO(fileRootPath, dataDirName, null);
        FileVO logFileVO = new FileVO(fileRootPath, logDirName, logFileName);
        FileVO tempFileVO = new FileVO(fileRootPath + dataDirName, tempDirName, null);
        // 첨부파일 경로
        if(this.fileProperty.getAttachDirName().containsKey(this.jobGroupId)) {
            String attachPath = this.fileProperty.getAttachPath();
            String attachDirName = this.fileProperty.getAttachDirName().get(this.jobGroupId);
            FileVO attachFileVO = new FileVO(attachPath, attachDirName, null);
        }
    }

    /**
     * 리소스 경로
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperty.getResourcePath();
    }

    public String getRootPath() {
        return this.fileVO.getFILE_ROOT_PATH();
    }

    /**
     * 첨부파일 경로
     *
     * @return
     */
    public String getAttachedFilePath() {
        return this.fileVO.getAttachedFilePath();
    }

    public String getTempPath(String jobId) {
        initFileVO(jobId);
        return this.fileVO.getTempFilePath();
    }


    /*********************************************************************************************************
     * 파일생성
     *********************************************************************************************************/

    public <T> void makeFile2(String jobId, List<T> list, Boolean append) {

        this.dataFileVO.setSuffixFileName(this.baseDt);

        makeDataFile(list, fileVO, append);
        makeLogFile(list, fileVO);

    }

    public <T> void makeFile(List<T> list, Boolean append) {
        makeFile(this.jobId, list, append);
    }

    public <T> void makeFile(String jobId, List<T> list) {
        makeFile(jobId, list, false);
    }

    public <T> void makeFile(List<T> list) {
        makeFile(this.jobId, list, false);
    }

    public <T> void makeFile(String jobId, List<T> list, Boolean append) {
//        HiveFileVO hiveFileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
//        hiveFileVO.setDataFileName(this.baseDt);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        initFileVO(jobId);
        this.fileVO.setDataFileName(this.baseDt);

        if (list.size() == 0) {
            log.info("skip.... no data");
            makeLogFile(list, fileVO);
            return;
        }
        makeDataFile(list, fileVO, append);
        makeLogFile(list, fileVO);
    }

    /**
     * 수집데이터파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list, CmmnFileVO fileVO) {

        makeDataFile(list, fileVO, false);
    }

    private <T> void makeDataFile(List<T> list, CmmnFileVO fileVO, Boolean append) {

//        log.info("[{}] Data file make start", fileVO.getTableName());


        FileUtil.makeTsvFile(fileVO.getDataFilePath(), fileVO.getDataFileName(), list, append);

        log.info("[{}] DataFile : {}", fileVO.getDataDirName(), fileVO.getDataFilePath() + fileVO.getDataFileName());
        log.info("[{}] DataFile listCnt : {}", fileVO.getDataDirName(), list.size());
    }

    /**
     * 성공 로그파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, CmmnFileVO fileVO) {

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(fileVO.getDataDirName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Succeeded");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

        log.info("[{}] LogFile :  {}", fileVO.getDataDirName(), fileVO.getLogFilePath() + fileVO.getLogFileName());
    }

    /**
     * 실패 로그파일 생성
     *
     * @param msg
     */
    public void makeLogFile(String msg) {

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
//        fileVO.setDataFileName(this.baseDt);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        fileVO.setDataFileName(this.baseDt);

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(fileVO.getDataDirName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Fail");
        logVO.setErrm(msg);
        logVO.setTargSuccessRows(1);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

        log.info("[{}] LogFile :  {}", fileVO.getDataDirName(), fileVO.getLogFilePath() + fileVO.getLogFileName());

    }



    /**
     * 임시파일생성
     *
     * @param list
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String fileName) {

//        if (list.size() == 0) {
//            log.debug("list is null");
//            return;
//        }

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
        fileVO.setTempFileName(fileName);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
//        fileVO.setTempFileName(this.baseDt);


        String tempFilePath = fileVO.getTempFilePath();
        String tempFileName = fileVO.getTempFileName();

        FileUtil.makeTsvFile(tempFilePath, tempFileName, list);

        log.info("TempFile:  {} ", tempFilePath + tempFileName);

    }


    /**
     * 임시파일 병합 (MultiThread로 생성된 임시파일)
     */
    public void mergeTempFile(String jobId) {

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
//        fileVO.setDataFileName(this.baseDt);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        this.fileVO.setDataFileName(this.baseDt);

        FileUtil.mergeFile(this.fileVO.getTempFilePath(), this.fileVO.getDataFilePath(), this.fileVO.getDataFileName());
        FileUtil.deleteFile(this.fileVO.getTempFilePath());

        // 로그파일 생성

    }

    /**
     * 임시파일 제거
     */
    public void cleanTempFile(String jobId) {
//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);

        if (Objects.isNull(this.fileVO)) {
            initFileVO(jobId);
        }
        FileUtil.deleteFile(this.fileVO.getTempFilePath());
    }

    public boolean tempFileExsists(String suffixFileName) {
//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
//        fileVO.setTempFileName(sufixFileName);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
//        fileVO.setTempFileName(this.baseDt);
        this.fileVO.setTempFileName(suffixFileName);

        String tempFilePath = this.fileVO.getTempFilePath();
        String tempFileName = this.fileVO.getTempFileName();
        File file = new File(tempFilePath + tempFileName);

        return file.exists();

    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

}