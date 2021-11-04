package kcs.edc.batch.cmmn.service;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.property.FileProperties;
import kcs.edc.batch.cmmn.property.JobProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.vo.CmmnFileVO;
import kcs.edc.batch.cmmn.vo.HiveFileVO;
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
    private FileProperties fileProperties;

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

    /**
     * 초기화
     * @param jobGroupId
     * @param jobId
     * @param baseDt
     */
    public void init(String jobGroupId, String jobId, String baseDt) {
        this.jobGroupId = jobGroupId;
        this.jobId = jobId;
        this.baseDt = baseDt;
        this.startTime = DateUtil.getCurrentTime();

        init(this.jobId);
    }

    public void init(String jobId) {
        this.jobId = jobId;

        FileProperties.FileProp fileProp = this.fileProperties.getFileProp(getJobGrpName(this.jobId));

        if(Objects.isNull(fileProp)) {
            throw new NullPointerException("fileProperty is null");
        }

        // 포털여부
        Boolean isPortalJobGroup = this.fileProperties.isPortalJobGroup(this.jobGroupId);
        this.fileVO = new CmmnFileVO(fileProp, isPortalJobGroup, this.jobGroupId, this.jobId);
    }

    /**
     * 리소스 경로
     *
     * @return
     */
    public String getResourcePath() {
        return this.fileProperties.getResourcePath();
    }

    public String getRootPath() {
        return this.fileVO.getROOT_PATH();
    }



    /**
     * 파일저장 경로
     * @return
     */
//    public String getStorePath() {
//
//        String jobGrpName = getJobGrpName(this.jobId);
//        if(jobGrpName.equals(CmmnConst.JOB_GRP_ID_KOT) || jobGrpName.equals(CmmnConst.JOB_GRP_ID_OPD)) {
//            return this.fileProperties.getTiberoStorePath().get(jobGrpName);
//        } else {
//            return this.fileProperties.getHiveStorePath();
//        }
//    }

    /**
     * jobId로 jobGrpNm 추출
     * 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
     *
     * @return grpName 배치그룹명
     */
    protected String getJobGrpName(String jobId) {

        String grpName = null;

        if(jobId.startsWith("iac")) {
            grpName = CmmnConst.JOB_GRP_ID_OPD;
        } else if(jobId.startsWith("pit")) {
            grpName = CmmnConst.JOB_GRP_ID_KOT;
        } else {
            // 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
            grpName = jobId.substring(0, 3).toLowerCase();
        }
        return grpName;
    }



    /*****************************************************************************************************************
     * 파일생성
     *****************************************************************************************************************/

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
        init(jobId);
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

        log.info("[{}] Data file make start", fileVO.getTableName());
        log.info("[{}] count : {}", fileVO.getTableName(), list.size());

        FileUtil.makeTsvFile(fileVO.getDataFilePath(), fileVO.getDataFileName(), list, append);

        log.info("[{}] completed : {}", fileVO.getTableName(), fileVO.getDataFilePath() + fileVO.getDataFileName());
    }

    /**
     * 로그파일 생성
     *
     * @param list
     * @param <T>
     */
    private <T> void makeLogFile(List<T> list, CmmnFileVO fileVO) {

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
        FileUtil.makeTsvFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

        log.info("[{}] completed :  {}", fileVO.getTableName(), fileVO.getLogFilePath() + fileVO.getLogFileName());
    }

    private void makeLogFile(String msg) {

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
//        fileVO.setDataFileName(this.baseDt);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
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
        FileUtil.makeTsvFile(fileVO.getLogFilePath(), fileVO.getLogFileName(), arrayList);

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

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
//        fileVO.setTempFileName(fileName);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        fileVO.setTempFileName(this.baseDt);


        String tempFilePath = fileVO.getTempFilePath();
        String tempFileName = fileVO.getTempFileName();

        FileUtil.makeTsvFile(tempFilePath, tempFileName, list);

        log.info("TempFile Completed :  {} ", tempFilePath + tempFileName);

    }


    /**
     * 임시파일 병합 (MultiThread로 생성된 임시파일)
     */
    public void mergeTempFile(String jobId) {

//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
//        fileVO.setDataFileName(this.baseDt);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        fileVO.setDataFileName(this.baseDt);

        FileUtil.mergeFile(fileVO.getTempFilePath(), fileVO.getDataFilePath(), fileVO.getDataFileName());
        FileUtil.deleteFile(fileVO.getTempFilePath());

        // 로그파일 생성

    }

    /**
     * 임시파일 제거
     */
    public void cleanTempFile(String jobId) {
//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), jobId);
//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        FileUtil.deleteFile(fileVO.getTempFilePath());
    }

    public boolean tempFileExsists(String sufixFileName) {
//        HiveFileVO fileVO = new HiveFileVO(this.fileProperties.getHiveStorePath(), this.jobId);
//        fileVO.setTempFileName(sufixFileName);

//        CmmnFileVO fileVO = new CmmnFileVO(this.fileProp, jobId);
        fileVO.setTempFileName(this.baseDt);

        String tempFilePath = fileVO.getTempFilePath();
        String tempFileName = fileVO.getTempFileName();
        File file = new File(tempFilePath + tempFileName);

        return file.exists();

    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

}