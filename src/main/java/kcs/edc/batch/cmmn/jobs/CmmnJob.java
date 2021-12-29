package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.service.ApiService;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.service.JobService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
public class CmmnJob implements StepExecutionListener {

    @Autowired
    protected JobService jobService;

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected FileService fileService;

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집기준일

    protected String startDt; // 시작일
    protected String endDt; // 종료일

    protected int period; // 수집기간

    protected List<Object> resultList = new ArrayList<>(); // 최종결과리스트
    protected ExecutionContext jobExecutionContext;

    protected String jobGroupId;
    protected String jobId;

    protected int itemCnt = 1;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        // jobExecutionContext 초기화
        // afterStep에서 파라미터를 셋팅하면 Step간 데이터가 공유된다.
        this.jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();

        // jobId, jobGroupId 셋팅
        this.jobId = getCurrentJobId();
        this.jobGroupId = getCurrentJobGroupId(this.jobId);

        // ApiService 초기화
        this.apiService.init(this.jobId);

        // JobService 초기화하고 baseDt, startDt, endDt, period를 셋팅한다.
        this.jobService.init(this.jobGroupId, this.baseDt);
        this.baseDt = this.jobService.getBaseDt();
        this.startDt = this.jobService.getStartDt();
        this.endDt = this.jobService.getEndDt();
        this.period = this.jobService.getPeriod();

        // FileService 초기화
        this.fileService.init(this.jobId, this.baseDt);

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        // 현재의 jobId를 공통으로 담아준다. FileMerge시 자동으로 jobId가 전달된다.
        this.jobExecutionContext.put("jobId", getCurrentJobId());

        return ExitStatus.COMPLETED;
    }

    protected String getJobGroupId() {
        return this.jobGroupId;
    }

    /**
     * ClassName에서 JobId 추출하여 return ex) Nav001mTasklet -> nav001m
     *
     * @return jobId 배치잡ID
     */
    protected String getCurrentJobId() {

        String className = this.getClass().getSimpleName();
        String jobId = null;
        if (className.endsWith("Tasklet")) {
            jobId = className.replace("Tasklet", "").toLowerCase();
            if (jobId.length() > 7) {
                jobId = jobId.substring(0, 7);
                log.info("jobId::::: {}", jobId);
            }
        } else {
            jobId = className;
        }
        return jobId;
    }

    protected String getCurrentJobGroupId(String jobId) {
        return jobId.substring(0, 3).toLowerCase();
    }

    /**
     * 배치 시작로그 출력
     */
    public void writeCmmnLogStart() {
        writeCmmnLogStart(this.jobId);
    }

    /**
     * 배치 시작로그 출력
     * @param jobId
     */
    public void writeCmmnLogStart(String jobId) {
        log.info("##########################################################################");
        log.info("START JOB :::: {} ", jobId);
        log.info("##########################################################################");
        log.info("baseDt: {}, startDt: {}, endDt: {}, period: {}", this.baseDt, this.startDt, this.endDt, this.period);
    }

    /**
     * 배치 종료로그 출력
     */
    public void writeCmmnLogEnd() {
        writeCmmnLogEnd(this.jobId);
    }

    /**
     * 배치 종료로그출력
     * @param jobId
     */
    public void writeCmmnLogEnd(String jobId) {
        log.info("END JOB :::: {}", jobId);
    }

    /**
     * 배치 시작로그 출력
     *
     * @param threadNum 쓰레드번호
     * @param listSize  목록건수
     */
    public void writeCmmnLogStart(String threadNum, int listSize) {
        log.info("##########################################################################");
        log.info("START JOB ::: {}, #{}, baseDt : {}, list size : {}", getCurrentJobId(), threadNum, this.baseDt, listSize);
    }

    /**
     * 배치 종료로그 출력
     *
     * @param threadNum 쓰레드번호
     */
    public void writeCmmnLogEnd(String threadNum, int listSize) {
//        log.info("##########################################################################");
        log.info("END JOB :::: {} #{}", getCurrentJobId(), threadNum);
        log.info("##########################################################################");
    }

    /**
     * 에러로그 생성
     *
     * @param msg 에러메시지
     */
    public void makeErrorLog(String msg) {
        makeErrorLog(this.jobId, msg);
    }

    /**
     * 에로로그 생성
     *
     * @param jobId
     * @param msg   에러메시지
     */
    public void makeErrorLog(String jobId, String msg) {
        try {
            this.fileService.makeErrorLog(jobId, msg);
        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

}
