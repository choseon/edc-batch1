package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.FileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileService {

    @Autowired
    private FileProperty fileProperty;

    private int cleanBackupBaseDt;

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

    /**
     * 수집 기준일 ex) 20211020
     */
    private String baseDt;

    /**
     * 배치 시작일시 ex) 2021-10-20 09:23:11
     */
    public String startTime;

    /**
     * 데이터파일 VO
     */
    private FileVO dataFileVO;

    /**
     * 임시파일 VO
     */
    private FileVO tempFileVO;

    /**
     * 로그파일 VO
     */
    private FileVO logFileVO;

    /**
     * 첨부파일 VO
     */
    private FileVO attachFileVO;

    /**
     * 백업파일 VO
     */
    private FileVO backupFileVO;

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

        this.jobId = jobId;

        // 파일 확장자
        String fileExtension = this.fileProperty.getDataFileExtension();
        // 데이터파일 루트경로
        String dataRootPath = this.fileProperty.getDataRootPath();

        // 데이터 디렉토리명 (테이블명)
        String dataDirName = this.fileProperty.getDataFilePrefixName() + jobId;
        // 데이터 파일명
        String dataFileName = dataDirName + "_" + this.baseDt;
        // 데이터파일VO 생성
        this.dataFileVO = new FileVO(dataRootPath, dataDirName, dataFileName, fileExtension);

        // 로그파일VO 생성
        String logDirName = this.fileProperty.getLogDirName();
        String logFileName = dataFileName + "_" + DateUtil.getCurrentTime2();
        this.logFileVO = new FileVO(dataRootPath, logDirName, logFileName, fileExtension);

        // 임시파일VO 생성
        String tempRootPath = this.dataFileVO.getFilePath();
        String tempDirName = this.fileProperty.getTempDirName();
        this.tempFileVO = new FileVO(tempRootPath, tempDirName, dataFileName, fileExtension);

        // 백업파일VO 생성
        String backupRootPath = this.dataFileVO.getFilePath();
        String backupDirName = this.fileProperty.getBackupDirName();
        this.backupFileVO = new FileVO(backupRootPath, backupDirName);

        // 백업파일 제거 기준일
        this.cleanBackupBaseDt = this.fileProperty.getCleanBackupBaseDt();

        // 첨부파일VO 생성
        String jobGroupId = jobId.substring(0, 3);
        if (this.fileProperty.getAttachDirName().containsKey(jobGroupId)) {
            String attachRootPath = this.fileProperty.getAttachRootPath();
            String attachDirName = this.fileProperty.getAttachDirName().get(jobGroupId);
            this.attachFileVO = new FileVO(attachRootPath, attachDirName);
        }
    }

    /**
     * 리소스 경로 리턴
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperty.getResourcePath();
    }

    /**
     * 첨부파일 경로 리턴
     *
     * @return
     */
    public String getAttachedFilePath() {
        return this.attachFileVO.getFilePath();
    }


    /**************************************************************************************************
     * 데이터 파일 관련
     **************************************************************************************************/

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param list 데이터리스트
     * @param <T>
     */
    public <T> void makeFile(List<T> list) throws FileNotFoundException, IllegalAccessException {
        makeFile(this.jobId, list, false);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param jobId
     * @param list  데이터리스트
     * @param <T>
     */
    public <T> void makeFile(String jobId, List<T> list) throws FileNotFoundException, IllegalAccessException {
        makeFile(jobId, list, false);
    }

    /**
     * 수집데이터파일 및 로그파일생성
     *
     * @param jobId
     * @param list   데이터리스트
     * @param append 파일이어쓰기 여부
     * @param <T>
     */
    public <T> void makeFile(String jobId, List<T> list, Boolean append) throws FileNotFoundException, IllegalAccessException {

        initFileVO(jobId);
        log.info("----------------------------------------------------------------------------");
        makeDataFile(list, this.dataFileVO, append);
        makeLogFile(list, this.logFileVO, this.dataFileVO);
        log.info("----------------------------------------------------------------------------");
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
            log.info("makeDataFile: {} listCnt is 0", this.jobId);
        } else {
            FileUtil.makeTsvFile(fileVO.getFilePath(), fileVO.getFileFullName(), list, append);
            log.info("makeDataFile: {}, listCnt: {}", fileVO.getFullFilePath(), list.size());
        }
    }

    /**************************************************************************************************
     * 로그 파일 관련
     **************************************************************************************************/

    /**
     * 성공 로그파일 생성
     *
     * @param list      데이터리스트
     * @param logFileVO 로그파일 정보가 담긴 VO
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, FileVO logFileVO, FileVO dataFileVO) throws FileNotFoundException, IllegalAccessException {
        makeLogFile(list.size(), logFileVO, dataFileVO);

    }

    /**
     * 성공 로그파일 생성
     *
     * @param listCnt    건수
     * @param logFileVO  로그파일 정보가 담긴 VO
     * @param dataFileVO 데이터파일 정보가 담긴 VO
     * @param <T>
     * @throws FileNotFoundException
     * @throws IllegalAccessException
     */
    private <T> void makeLogFile(int listCnt, FileVO logFileVO, FileVO dataFileVO) throws FileNotFoundException, IllegalAccessException {
        Log001mVO log001mVO = new Log001mVO();
        log001mVO.setParamYmd(DateUtil.getCurrentDate()); // 작업일자
        log001mVO.setStep(CmmnProperties.LOG_FILE_STEP);
        log001mVO.setTableName(dataFileVO.getFileDirName());
        log001mVO.setStartTime(this.startTime);
        log001mVO.setEndTime(DateUtil.getCurrentTime());
        log001mVO.setJobStat(CmmnProperties.LOG_JOB_STAT_SUCCEEDED);
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
     * 실패 로그파일 생성
     *
     * @param msg 에러메시지
     */
    public void makeFailLogFile(String msg) throws FileNotFoundException, IllegalAccessException {

        Log001mVO log001mVO = new Log001mVO();
        log001mVO.setParamYmd(DateUtil.getCurrentDate()); // 작업일자
        log001mVO.setStep(CmmnProperties.LOG_FILE_STEP);
        log001mVO.setTableName(this.dataFileVO.getFileDirName());
        log001mVO.setStartTime(this.startTime);
        log001mVO.setEndTime(DateUtil.getCurrentTime());
        log001mVO.setJobStat(CmmnProperties.LOG_JOB_STAT_FAIL);
        log001mVO.setErrm(msg);
        log001mVO.setTargSuccessRows(0);
        log001mVO.setBaseDt(this.baseDt);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(log001mVO);

        // 로그 파일 생성
        FileUtil.makeTsvFile(this.logFileVO.getFilePath(), this.logFileVO.getFileFullName(), arrayList);
        log.info("makeFailLogFile: {}", this.logFileVO.getFullFilePath());

    }

    /**
     * 에러로그 생성
     *
     * @param msg 에러메시지
     */
    public void makeErrorLog(String msg) throws FileNotFoundException, IllegalAccessException {
        makeErrorLog(this.jobId, msg);
    }

    /**
     * 에로로그 생성
     *
     * @param jobId
     * @param msg   에러메시지
     */
    public void makeErrorLog(String jobId, String msg) throws FileNotFoundException, IllegalAccessException {

        log.info("----------------------------------------------------------------------------");
        log.info(msg);
        this.initFileVO(jobId);
        this.makeFailLogFile(msg);
        log.info("----------------------------------------------------------------------------");
    }

    /**************************************************************************************************
     * 임시파일 관련
     **************************************************************************************************/

    /**
     * 임시파일 생성
     *
     * @param list              파일로 생성할 리스트 목록
     * @param appendingFileName 기본파일명에 추가될 임시파일명
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String appendingFileName) throws FileNotFoundException, IllegalAccessException {
        makeTempFile(list, appendingFileName, false);
    }

    /**
     * 임시파일 생성
     *
     * @param list              파일로 생성할 리스트 목록
     * @param appendingFileName 기본파일명에 추가될 임시파일명
     * @param isForce           리스트가 0건인 경우 파일 생성 여부 설정
     * @param <T>
     * @throws FileNotFoundException
     * @throws IllegalAccessException
     */
    public <T> void makeTempFile(List<T> list, String appendingFileName, Boolean isForce)
            throws FileNotFoundException, IllegalAccessException {

        if (!isForce && list.size() == 0) return;

        if (!ObjectUtils.isEmpty(appendingFileName)) {
            this.tempFileVO.setAppendingFileName(appendingFileName);
        }

        FileUtil.makeTsvFile(this.tempFileVO.getFilePath(), this.tempFileVO.getFileFullName(), list);
        log.info("makeTempFile: {}, listCnt: {} ", this.tempFileVO.getFullFilePath(), list.size());
    }


    /**
     * 임시파일 병합
     */
    public void mergeTempFile() throws IOException, IllegalAccessException {
        mergeTempFile(null);
    }

    /**
     * 임시파일 병합
     *
     * @param appendingFileName 기본파일명에 추가되는 파일명
     * @throws IOException
     * @throws IllegalAccessException
     */
    public void mergeTempFile(String appendingFileName) throws IOException, IllegalAccessException {

        if (!ObjectUtils.isEmpty(appendingFileName)) {
            this.dataFileVO.setAppendingFileName(appendingFileName);
        }

        // temp 폴더 존재여부 확인
        if (!new File(this.tempFileVO.getFilePath()).exists()) {
            log.info("mergeTempPath: {} not exists", this.tempFileVO.getFilePath());
            return;
        }

        log.info("----------------------------------------------------------------------------");
        // 임시파일 병합하여 데이터파일 생성
        int listCnt = FileUtil.mergeFile(this.tempFileVO.getFilePath(), this.dataFileVO.getFilePath(), this.dataFileVO.getFileFullName());
        log.info("makeDataFile: {}, listCnt: {} ", this.dataFileVO.getFullFilePath(), listCnt);
        // 로그파일 생성
        makeLogFile(listCnt, this.logFileVO, this.dataFileVO);
        log.info("----------------------------------------------------------------------------");

        // 임시파일 삭제
        cleanTempFile();
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
     */
    public void cleanTempFile() {
        FileUtil.deleteFile(this.tempFileVO.getFilePath());
    }

    /**
     * 임시파일 존재여부 체크
     *
     * @param suffixFileName
     * @return
     */
    public boolean isTempFileExsists(String suffixFileName) {

        this.tempFileVO.setAppendingFileName(suffixFileName);
        return new File(this.tempFileVO.getFullFilePath()).exists();
    }

    /**
     * 임시파일경로 존재여부 체크
     *
     * @return
     */
    public boolean isTempPathExsists() {

        String filePath = this.tempFileVO.getFilePath();
        return new File(filePath).exists();
    }

    /**
     * 임시파일 경로 리턴
     *
     * @return
     */
    public String getTempPath() {
        return this.tempFileVO.getFilePath();
    }

    /**
     * 임시파일VO 리턴
     *
     * @return
     */
    public FileVO getTempFileVO() {
        return this.tempFileVO;
    }


    /**************************************************************************************************
     * 백업파일 관련
     **************************************************************************************************/

    /**
     * 백업파일 제거
     *
     * @throws ParseException
     */
    public void cleanBackupFile() throws ParseException {

        String currentDate = DateUtil.getCurrentDate("yyyy/MM");
        String baseDt = DateUtil.getOffsetMonth(currentDate, this.cleanBackupBaseDt, "yyyy/MM");

        this.backupFileVO.setAppendingFilePath(baseDt);
        FileUtil.deleteFile(this.backupFileVO.getFilePath());

//        log.info("cleanBackupFile: {}", this.backupFileVO.getFilePath());
    }
}