package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.FileProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.HiveFileVO;
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
     * 초기화
     *
     * @param jobId  배치잡ID
     * @param baseDt 수집기준일
     */
    public void init(String jobId, String baseDt) {
        this.jobId = jobId;
        this.baseDt = baseDt;
    }

    public void init(String jobId) {
        this.jobId = jobId;
    }

    /**
     * 리소스 경로
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperties.getResourcePath();
    }

    /**
     * 파일생성 : 데이터파일과 로그파일을 생성한다
     *
     * @param list
     * @param <T>
     */
    public <T> void makeFile(List<T> list) {

        HiveFileVO hiveFileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
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
     * 수집데이터파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeDataFile(List<T> list, HiveFileVO fileVO) {

        log.info("[{}] Data file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

        try {

            FileUtil.makeFile(fileVO.getDataFilePath(), fileVO.getDataFileName(), list);

            log.info("[{}] completed : {}", fileVO.getTableName(), fileVO.getDataFilePath() + fileVO.getDataFileName());

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
    private <T> void makeLogFile(List<T> list, HiveFileVO fileVO) {

        log.info("[{}] Log file make start", fileVO.getTableName());

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep("EXT_FILE_CREATE");
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(DateUtil.getCurrentTime());
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Succeeded");
        logVO.setTargSuccessRows(list.size());

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        try {

            // 로그 파일 생성
            FileUtil.makeFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

            log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFilePath() + fileVO.getLogFileName());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void makeLogFile(String msg) {

        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
        fileVO.setDataFileName(this.baseDt);

        Log001mVO logVO = new Log001mVO();
        logVO.setParamYmd(this.baseDt);
        logVO.setStep("EXT_FILE_CREATE");
        logVO.setTableName(fileVO.getTableName());
        logVO.setStartTime(DateUtil.getCurrentTime());
        logVO.setEndTime(DateUtil.getCurrentTime());
        logVO.setJobStat("Fail");
        logVO.setErrm(msg);
        logVO.setTargSuccessRows(0);

        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(logVO);

        try {

            // 로그 파일 생성
            FileUtil.makeFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

            log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFilePath() + fileVO.getLogFileName());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 임시파일생성
     *
     * @param list
     * @param <T>
     */
    public <T> void makeTempFile(List<T> list, String fileName) {

        if (list.size() == 0) {
            log.info("list is null");
            return;
        }


//        log.info("[{}] Temp file make start", getTableName());
//        log.info("[{}] count : {}", getTableName(), list.size());

        try {

            // temp 파일이 존재하면 모두 삭제
//            FileUtil.deleteFiles(filePath);
//            FileUtil.makeFile(getTempFilePath(), getTempFileName(fileName), list);

            HiveFileVO hiveFileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
            hiveFileVO.setTempFileName(fileName);

            String tempFilePath = hiveFileVO.getTempFilePath();
            String tempFileName = hiveFileVO.getTempFileName();

            FileUtil.makeFile(tempFilePath, tempFileName, list);


//            log.info("[{}] completed :  {} ", getTableName(), getTempFileFullPath());
            log.info("TempFile Completed :  {} ", tempFilePath + tempFileName);

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }


    /**
     * 임시파일 병합 (MultiThread로 생성된 임시파일)
     */
    public void mergeTempFile(String jobId) {

        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
        fileVO.setDataFileName(this.baseDt);
        log.info("fileVO.getTempFilePath() : {}", fileVO.getTempFilePath());
        FileUtil.mergeFile(fileVO.getTempFilePath(), fileVO.getDataFilePath(), fileVO.getDataFileName());
        FileUtil.deleteFile(fileVO.getTempFilePath());
    }

    /**
     * 임시파일 제거
     */
    public void cleanTempFile(String jobId) {
        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
        FileUtil.deleteFile(fileVO.getTempFilePath());
    }

}