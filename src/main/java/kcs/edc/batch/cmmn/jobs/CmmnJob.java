package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.service.ApiService;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.service.SchedulerService;
import kcs.edc.batch.cmmn.util.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
public class CmmnJob implements StepExecutionListener {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected FileService fileService;

    @Autowired
    protected SchedulerService schedulerService;

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집기준일

    protected String startDt; // 시작일
    protected String endDt; // 종료일

    //    @Value("#{jobParameters[period]}")
    protected int period = 1;

    protected List<Object> resultList = new ArrayList<>(); // 최종결과리스트
    protected ExecutionContext jobExecutionContext;

    protected String jobGroupId;
    protected String jobId;

    protected int itemCnt = 1;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        // step간 파라미터 넘겨주기 위해 jobExcutionContext 초기화
        // afterStep에서 넘겨줄 값 셋팅해준다
        this.jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();

        this.jobId = getCurrentJobId();
        this.apiService.init(this.jobId);
        this.fileService.init(this.jobId, this.baseDt);

        this.jobGroupId = getCurrentJobGroupId(this.jobId);
        this.schedulerService.init(this.jobGroupId);
        this.period = this.schedulerService.getPeriod();

        if (ObjectUtils.isEmpty(this.baseDt)) {

            String baseLine = this.schedulerService.getBaseLine();
            this.baseDt = DateUtil.getBaseLineDate(baseLine);

        } else {
            if(this.jobId.equals(CmmnConst.JOB_ID_UCT001M)) {
                this.period = 1;
            }
        }

        this.endDt = this.baseDt;

        if (baseDt.length() == 4) {
            this.startDt = DateUtil.getOffsetYear(this.baseDt, (this.period - 1) * -1);
        } else {
            this.startDt = DateUtil.getOffsetDate(this.baseDt, (this.period - 1) * -1);
        }
        log.debug(">> baseDt: {}, startDt: {}, endDt: {}, period: {}", this.baseDt, this.startDt, this.endDt, this.period);


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
        writeCmmnLogStart(getCurrentJobId());
    }

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
        writeCmmnLogEnd(getCurrentJobId());
    }

    public void writeCmmnLogEnd(String jobId) {
//        log.info("##########################################################################");
        log.info("END JOB :::: {}", jobId);
//        log.info("##########################################################################");
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

    public void makeErrorLog(String msg) {
        makeErrorLog(this.jobId, msg);
    }

    public void makeErrorLog(String jobId, String msg) {

        try {
            log.info(msg);
            this.fileService.initFileVO(jobId);
            this.fileService.makeLogFile(msg);
        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IllegalAccessException e) {
            log.info(e.getMessage());
        }
    }

}
