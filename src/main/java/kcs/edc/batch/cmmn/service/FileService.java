package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.FileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileService {

    private String LOG_FILE_STEP = "EXT_FILE_CREATE";
    private String LOG_JOB_STAT_SUCCEEDED = "Succeeded";
    private String LOG_JOB_STAT_FAIL = "Fail";

    @Autowired
    private FileProperty fileProperty;

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
     * FileService 초기화
     *
     * @param jobId
     * @param baseDt
     */
    public void init(String jobId, String baseDt) {
        this.jobId = jobId;
        this.baseDt = baseDt;
        this.startTime = DateUtil.getCurrentTime();

        initFileVO(this.jobId);
        log.info("FileService init() >> jobId: {}", this.jobId);
    }

    /**
     * fileVO 초기화
     *
     * @param jobId
     */
    public void initFileVO(String jobId) {
//        this.fileVO = new CmmnFileVO(this.fileProperty, this.jobId);

        // 파일 확장자
        String fileExtension = this.fileProperty.getDataFileExtension();

        String fileRootPath = this.fileProperty.getRootPath();
        String dataFilePrefixName = this.fileProperty.getDataFilePrefixName();
        // 데이터 디렉토리명
        String dataDirName = dataFilePrefixName + jobId;
        // 데이터 파일명
        String dataFileName = dataDirName + "_" + this.baseDt;
        this.dataFileVO = new FileVO(fileRootPath, dataDirName, dataFileName, fileExtension);

        // 로그 디렉토리명
        String logDirName = this.fileProperty.getLogDirName();
        // 로그파일명
        //      String logFileName = dataDirName + "_" + DateUtil.getCurrentTime2();
        String logFileName = dataFileName + "_" + DateUtil.getCurrentTime2();
        this.logFileVO = new FileVO(fileRootPath, logDirName, logFileName, fileExtension);

        // 임시파일 디렉토리명
        String tempRootPath = fileRootPath + dataDirName + "/";
        String tempDirName = this.fileProperty.getTempDirName();
        this.tempFileVO = new FileVO(tempRootPath, tempDirName, dataFileName, fileExtension);

        // 첨부파일 경로
        String jobGroupId = jobId.substring(0, 3);
        if (this.fileProperty.getAttachDirName().containsKey(jobGroupId)) {
            String attachPath = this.fileProperty.getAttachRootPath();
            String attachDirName = this.fileProperty.getAttachDirName().get(jobGroupId);
            this.attachFileVO = new FileVO(attachPath, attachDirName, null, fileExtension);
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

    /**
     * 첨부파일 경로
     *
     * @return
     */
    public String getAttachedFilePath() {
        return this.attachFileVO.getFilePath();
    }

    /**
     * 현재 jobId의 임시파일 경로
     *
     * @return
     */
    public String getTempPath() {
        return getTempPath(this.jobId);
    }

    /**
     * 파라미터로 넘어온 jobId의 임시파일 경로
     *
     * @param jobId
     * @return
     */
    public String getTempPath(String jobId) {

        initFileVO(jobId);
        return this.tempFileVO.getFilePath();
    }

    /**************************************************************************************************
     * 파일생성 관련
     **************************************************************************************************/

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param list
     * @param <T>
     */
    public <T> void makeFile(List<T> list) {
        makeFile(this.jobId, list, false);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param list
     * @param append
     * @param <T>
     */
    public <T> void makeFile(List<T> list, Boolean append) {
        makeFile(this.jobId, list, append);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param jobId
     * @param list
     * @param <T>
     */
    public <T> void makeFile(String jobId, List<T> list) {
        makeFile(jobId, list, false);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param jobId
     * @param list
     * @param append
     * @param <T>
     */
    public <T> void makeFile(String jobId, List<T> list, Boolean append) {

        initFileVO(jobId);
        makeDataFile(list, this.dataFileVO, append);
        makeLogFile(list, this.logFileVO);
    }

    /**
     * 수집데이터파일 생성
     *
     * @param list
     * @param fileVO
     * @param append
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list, FileVO fileVO, Boolean append) {

        if (ObjectUtils.isEmpty(list)) {
            log.info("DataFile: listCnt is 0");
        } else {
            FileUtil.makeTsvFile(fileVO.getFilePath(), fileVO.getFileFullName(), list, append);
            log.info("DataFile: {}, listCnt: {}", fileVO.getFullFilePath(), list.size());
        }
    }

    /**
     * 성공 로그파일 생성
     *
     * @param list
     * @param fileVO
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, FileVO fileVO) {

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(fileVO.getFileDirName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat(LOG_JOB_STAT_SUCCEEDED);
        logVO.setTargSuccessRows(list.size());
        logVO.setBaseDt(this.baseDt);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(fileVO.getFilePath(), fileVO.getFileFullName(), arrayList);
        log.info("logFile: {}", fileVO.getFullFilePath());
    }

    /**
     * 실패 로그파일 생성
     *
     * @param msg
     */
    public void makeLogFile(String msg) {

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(this.logFileVO.getFileDirName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat(LOG_JOB_STAT_FAIL);
        logVO.setErrm(msg);
        logVO.setTargSuccessRows(0);
        logVO.setBaseDt(this.baseDt);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(this.logFileVO.getFilePath(), this.logFileVO.getFileFullName(), arrayList);
        log.info("logFile: {}", this.logFileVO.getFullFilePath());

    }

    /**
     * 임시파일 생성
     *
     * @param list
     * @param appendingFileName
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String appendingFileName) {

        this.tempFileVO.setAppendingFileName(appendingFileName);

        FileUtil.makeTsvFile(this.tempFileVO.getFilePath(), this.tempFileVO.getFileFullName(), list);
        log.info("TempFile: {}, listCnt: {} ", this.tempFileVO.getFullFilePath(), list.size());
    }


    /**
     * 임시파일 병합
     *
     * @param jobId
     */
    public void mergeTempFile(String jobId) {

        initFileVO(jobId);
        FileUtil.mergeFile(this.tempFileVO.getFilePath(), this.dataFileVO.getFilePath(), this.dataFileVO.getFileFullName());
        FileUtil.deleteFile(this.tempFileVO.getFilePath());
    }

    /**
     * 임시파일 제거
     *
     * @param jobId
     */
    public void cleanTempFile(String jobId) {

        initFileVO(jobId);
        FileUtil.deleteFile(this.tempFileVO.getFilePath());
    }

    public void cleanTempFile() {
        cleanTempFile(this.jobId);
    }

    /**
     * 임시파일 존재여부 체크
     *
     * @param suffixFileName
     * @return
     */
    public boolean isTempFileExsists(String suffixFileName) {

        this.tempFileVO.setAppendingFileName(suffixFileName);
        File file = new File(this.tempFileVO.getFullFilePath());

        return file.exists();
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public FileVO getTempFileVO() {
        return tempFileVO;
    }

}