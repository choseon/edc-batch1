package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.FileProperty;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.FileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileService {

    @Autowired
    protected FileProperty fileProperty;

    /**
     * 로그 테이블명
     */
    private String LOG_TABLE_NAME = "ht_log001m";

    /**
     * 테이블명 패턴 : ht_[jobId] ex) ht_nav003m
     */
    private String PATTERN_TABLE_NAME = "ht_%s";

    /**
     * 파일명 패턴 : ht_[jobId]_[cletDt].txt ex) ht_nav003m_20211020.txt
     */
    private String PATTERN_FILE_NAME = "ht_%s_%s.txt";

    /**
     * 경로 패턴 : [저장경로]/[테이블명]/ ex) /hdata/ht_nav003m/
     */
    private String PATTERN_PATH = "%s/%s/";

    /**
     * 배치잡ID ex) nav003m
     */
    private String jobId;

    /**
     * 수집 기준일 ex) 20211020
     */
    private String cletDt;


    /**
     * 초기화
     *
     * @param jobId  배치잡ID
     * @param cletDt 수집기준일
     */
    public void init(String jobId, String cletDt) {
        this.jobId = jobId;
        this.cletDt = cletDt;
    }

    /**
     * 테이블명 조회 ex) nav001m -> ht_nav001m
     *
     * @return tableName 테이블명
     */
    public String getTableName() {

//        return this.PREFIX_TABLE_NAME + this.jobId;
        return String.format(this.PATTERN_TABLE_NAME, this.jobId);
    }

    /**
     * 수집파일 경로 조회 return ex) /hdata/ht_nav001m/
     *
     * @return dataFilePath 수집파일경로
     */
    public String getDataFilePath() {

//        return this.fileProperty.getStorePath() + getTableName() + "/";
//        return String.format(this.fileProperty.getStorePath() + "%s/", getTableName());
        return String.format(this.PATTERN_PATH, this.fileProperty.getStorePath(), getTableName());

    }

    /**
     * 수집파일명 조회 ex)ht_nav001m_20210826090407.txt
     *
     * @return dataFileName 수집파일명
     */
    public String getDataFileName() {

//        return getTableName() + "_" + this.cletDt + "." + this.FILE_EXTENSION;
        return String.format(this.PATTERN_FILE_NAME, this.jobId, this.cletDt);
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

//        return this.fileProperty.getStorePath() + LOG_TABLE_NAME + "/";
        return String.format(this.PATTERN_PATH, this.fileProperty.getStorePath(), this.LOG_TABLE_NAME);
    }

    /**
     * 로그파일명 조회 ex)ht_nav001m_20210826090407.txt
     *
     * @return logFileName 로그파일명
     */
    public String getLogFileName() {
//        return getTableName() + "_" + DateUtil.getCurrentTime2() + "." + this.FILE_EXTENSION;
        return String.format(this.PATTERN_FILE_NAME, this.jobId, DateUtil.getCurrentTime2());
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

//        return getTableName() + "_" + DateUtil.getCurrentTime2() + ".txt";
        return String.format(this.PATTERN_FILE_NAME, this.jobId, DateUtil.getCurrentTime2());
    }

    /**
     * 임시파일 전체경로 조회
     *
     * @return tempFileFullPath 임시파일 전체경로
     */
    public String getTempFileFullPath() {
        return getTempFilePath() + getTempFileName();
    }

    /**
     * 리소스 경로
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperty.getResourcePath();
    }

    public <T> void makeFile(List<T> list) {
        if (list.size() == 0) {
            log.info("[{}] Datafile file make start", getTableName());
            log.info("skip.... no data");
            makeLogFile(list);
            return;
        }
        makeDataFile(list);
        makeLogFile(list);
    }

    /**
     * 임시파일생성
     *
     * @param list
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list) {

        if (list.size() == 0) {
            log.info("list is null");
            return;
        }

        log.info("[{}] Temp file make start", getTableName());
        log.info("[{}] count : {}", getTableName(), list.size());

        try {

            // temp 파일이 존재하면 모두 삭제
//            FileUtil.deleteFiles(filePath);

            FileUtil.makeFile(getTempFilePath(), getTempFileName(), list);

            log.info("[{}] completed :  {} ", getTableName(), getTempFileFullPath());

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * 수집데이터파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list) {

        log.info("[{}] Data file make start", getTableName());
        log.info("[{}] count : {}", getTableName(), list.size());

        try {

            FileUtil.makeFile(getDataFilePath(), getDataFileName(), list);

            log.info("[{}] completed : {}", getTableName(), getDataFileFullPath());

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * 로그파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list) {

        log.info("[{}] Log file make start", getTableName());

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.cletDt);
        logVO.setTableName(getTableName());
        logVO.setStartTime(DateUtil.getCurrentTime());
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Success");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        try {

            // 로그 파일 생성
            FileUtil.makeFile(getLogFilePath(), getLogFileName(), arrayList);

            log.info("[{}] completed :  {}", getTableName(), getLogFileFullPath());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCletDt() {
        return cletDt;
    }

    public void setCletDt(String cletDt) {
        this.cletDt = cletDt;
    }
}