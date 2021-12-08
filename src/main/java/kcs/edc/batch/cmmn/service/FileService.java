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
import java.io.FileNotFoundException;
import java.io.IOException;
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
        log.debug("FileService init() >> jobId: {}", this.jobId);
    }

    /**
     * fileVO 초기화
     *
     * @param jobId
     */
    public void initFileVO(String jobId) {

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
    public <T> void makeFile(List<T> list) throws FileNotFoundException, IllegalAccessException {
        makeFile(this.jobId, list, false);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param list
     * @param append
     * @param <T>
     */
    public <T> void makeFile(List<T> list, Boolean append) throws FileNotFoundException, IllegalAccessException {
        makeFile(this.jobId, list, append);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param jobId
     * @param list
     * @param <T>
     */
    public <T> void makeFile(String jobId, List<T> list) throws FileNotFoundException, IllegalAccessException {
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
    public <T> void makeFile(String jobId, List<T> list, Boolean append) throws FileNotFoundException, IllegalAccessException {

        initFileVO(jobId);
        makeDataFile(list, this.dataFileVO, append);
        makeLogFile(list, this.logFileVO, this.dataFileVO);
    }

    /**
     * 수집데이터파일 생성
     *
     * @param list
     * @param fileVO
     * @param append
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list, FileVO fileVO, Boolean append) throws FileNotFoundException, IllegalAccessException {

        if (list.size() == 0) {
            log.info("makeDataFile: listCnt is 0");
        } else {
            FileUtil.makeTsvFile(fileVO.getFilePath(), fileVO.getFileFullName(), list, append);
            log.info("makeDataFile: {}, listCnt: {}", fileVO.getFullFilePath(), list.size());
        }
    }

    private <T> void makeLogFile(int listCnt, FileVO logFileVO, FileVO dataFileVO) throws FileNotFoundException, IllegalAccessException {
        Log001mVO log001mVO = new Log001mVO();
        log001mVO.setParamYmd(DateUtil.getCurrentDate()); // 작업일자
        log001mVO.setStep(this.LOG_FILE_STEP);
        log001mVO.setTableName(dataFileVO.getFileDirName());
        log001mVO.setStartTime(this.startTime);
        log001mVO.setEndTime(DateUtil.getCurrentTime());
        log001mVO.setJobStat(this.LOG_JOB_STAT_SUCCEEDED);
        log001mVO.setTargSuccessRows(listCnt);
        log001mVO.setBaseDt(this.baseDt);

        // 데이터 파일용량
        File file = new File(dataFileVO.getFullFilePath());
        if (file.exists()) {
            log001mVO.setBytes(file.length());
        }

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(log001mVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(logFileVO.getFilePath(), logFileVO.getFileFullName(), arrayList);
        log.info("makeLogFile: {}", logFileVO.getFullFilePath());
    }

    /**
     * 성공 로그파일 생성
     *
     * @param list
     * @param logFileVO
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, FileVO logFileVO, FileVO dataFileVO) throws FileNotFoundException, IllegalAccessException {
        makeLogFile(list.size(), logFileVO, dataFileVO);

    }

    /**
     * 실패 로그파일 생성
     *
     * @param msg
     */
    public void makeLogFile(String msg) throws FileNotFoundException, IllegalAccessException {

        Log001mVO log001mVO = new Log001mVO();
        log001mVO.setParamYmd(DateUtil.getCurrentDate()); // 작업일자
        log001mVO.setStep(this.LOG_FILE_STEP);
        log001mVO.setTableName(this.dataFileVO.getFileDirName());
        log001mVO.setStartTime(this.startTime);
        log001mVO.setEndTime(DateUtil.getCurrentTime());
        log001mVO.setJobStat(this.LOG_JOB_STAT_FAIL);
        log001mVO.setErrm(msg);
        log001mVO.setTargSuccessRows(0);
        log001mVO.setBaseDt(this.baseDt);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(log001mVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(this.logFileVO.getFilePath(), this.logFileVO.getFileFullName(), arrayList);
        log.info("makeLogFile: {}", this.logFileVO.getFullFilePath());

    }

    /**
     * 임시파일 생성
     *
     * @param list
     * @param appendingFileName
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String appendingFileName) throws FileNotFoundException, IllegalAccessException {
        makeTempFile(this.jobId, list, appendingFileName, false);
    }

    public <T> void makeTempFile(List<T> list, String appendingFileName, Boolean isForce) throws FileNotFoundException, IllegalAccessException {
        makeTempFile(this.jobId, list, appendingFileName, isForce);
    }

    public <T> void makeTempFile(String jobId, List<T> list, String appendingFileName) throws FileNotFoundException, IllegalAccessException {
        makeTempFile(jobId, list, appendingFileName, false);
    }

    public <T> void makeTempFile(String jobId, List<T> list, String appendingFileName, Boolean isForce) throws FileNotFoundException, IllegalAccessException {

        if(!isForce && list.size() == 0) return;
        this.initFileVO(jobId);
        this.tempFileVO.setAppendingFileName(appendingFileName);

        FileUtil.makeTsvFile(this.tempFileVO.getFilePath(), this.tempFileVO.getFileFullName(), list);
        log.info("makeTempFile: {}, listCnt: {} ", this.tempFileVO.getFullFilePath(), list.size());
    }


    /**
     * 임시파일 병합
     *
     * @param jobId
     */
    public void mergeTempFile(String jobId) throws IOException, IllegalAccessException {
        mergeTempFile(jobId, null);
    }

    public void mergeTempFile(String jobId, String appendingFileName) throws IOException, IllegalAccessException {

        initFileVO(jobId);
        if(!ObjectUtils.isEmpty(appendingFileName)) {
            this.dataFileVO.setAppendingFileName(appendingFileName);
        }

        // temp 폴더 존재여부 확인
        if(!new File(this.tempFileVO.getFilePath()).exists()) {
            log.info("tempPath: {} is not exists", this.tempFileVO.getFilePath());
        } else {
            // 임시파일 병합하여 데이터파일 생성
            int listCnt = FileUtil.mergeFile(this.tempFileVO.getFilePath(), this.dataFileVO.getFilePath(), this.dataFileVO.getFileFullName());
            log.info("makeDataFile: {}, listCnt: {} ", this.dataFileVO.getFullFilePath(), listCnt);
            // 로그파일 생성
            makeLogFile(listCnt, this.logFileVO, this.dataFileVO);
            // 임시파일 삭제
            cleanTempFile(jobId);
        }
    }

    /**
     * 임시파일 갯수 조회
     *
     * @return
     */
    public int getTempFileCnt() {

        String filePath = this.tempFileVO.getFilePath();
        int fileCnt = FileUtil.getFileCnt(filePath);

        return fileCnt;
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