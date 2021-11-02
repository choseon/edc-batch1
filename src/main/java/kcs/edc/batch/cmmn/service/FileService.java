package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.FileProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.HiveFileVO;
import kcs.edc.batch.cmmn.vo.Log001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileService {

    private String LOG_FILE_STEP = "EXT_FILE_CREATE";

    @Autowired
    protected FileProperties fileProperties;

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
    private static String startTime;

    /**
     * 초기화
     *
     * @param jobId  배치잡ID
     * @param baseDt 수집기준일
     */
    public void init(String jobId, String baseDt) {
        this.jobId = jobId;
        this.baseDt = baseDt;
        this.startTime = DateUtil.getCurrentTime();
    }

    /**
     * 리소스 경로
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperties.getResourcePath();
    }

    public <T> void makeFile(String jobId, List<T> list) {
        HiveFileVO hiveFileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
        hiveFileVO.setDataFileName(this.baseDt);

        if (list.size() == 0) {
            log.info("skip.... no data");
            makeLogFile(list, hiveFileVO);
            return;
        }
        makeDataFile(list, hiveFileVO);
        makeLogFile(list, hiveFileVO);
    }

    /**
     * 파일생성 : 데이터파일과 로그파일을 생성한다
     *
     * @param list
     * @param <T>
     */
    public <T> void makeFile(List<T> list) {
        makeFile(this.jobId, list);
    }

    /**
     * 수집데이터파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list, HiveFileVO fileVO) {

        log.info("[{}] Data file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

        FileUtil.makeFile(fileVO.getDataFilePath(), fileVO.getDataFileName(), list);

        log.info("[{}] completed : {}", fileVO.getTableName(), fileVO.getDataFilePath() + fileVO.getDataFileName());
    }

    /**
     * 로그파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, HiveFileVO fileVO) {

        log.info("[{}] Log file make start", fileVO.getTableName());

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Succeeded");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

        log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFilePath() + fileVO.getLogFileName());
    }

    private void makeLogFile(String msg) {

        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
        fileVO.setDataFileName(this.baseDt);

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep(this.LOG_FILE_STEP);
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(this.startTime);
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Fail");
        logVO.setErrm(msg);
        logVO.setTargSuccessRows(1);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        // 로그 파일 생성
        FileUtil.makeFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

        log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFilePath() + fileVO.getLogFileName());

    }

    /**
     * 임시파일생성
     *
     * @param list
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String fileName) {

//        if (list.size() == 0) {
//            log.info("list is null");
//            return;
//        }

        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
        fileVO.setTempFileName(fileName);

        String tempFilePath = fileVO.getTempFilePath();
        String tempFileName = fileVO.getTempFileName();

        FileUtil.makeFile(tempFilePath, tempFileName, list);

        log.info("TempFile Completed :  {} ", tempFilePath + tempFileName);

    }


    /**
     * 임시파일 병합 (MultiThread로 생성된 임시파일)
     */
    public void mergeTempFile(String jobId) {

        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
        fileVO.setDataFileName(this.baseDt);

        FileUtil.mergeFile(fileVO.getTempFilePath(), fileVO.getDataFilePath(), fileVO.getDataFileName());
        FileUtil.deleteFile(fileVO.getTempFilePath());

        // 로그파일 생성

    }

    /**
     * 임시파일 제거
     */
    public void cleanTempFile(String jobId) {
        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
        FileUtil.deleteFile(fileVO.getTempFilePath());
    }

    public boolean tempFileExsists(String sufixFileName) {
        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
        fileVO.setTempFileName(sufixFileName);
        String tempFilePath = fileVO.getTempFilePath();
        String tempFileName = fileVO.getTempFileName();
        File file = new File(tempFilePath + tempFileName);

        return file.exists();

    }

}