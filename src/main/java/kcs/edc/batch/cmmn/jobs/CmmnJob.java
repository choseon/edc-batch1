package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.service.ApiService;
import kcs.edc.batch.cmmn.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@StepScope
public class CmmnJob implements StepExecutionListener {

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected FileService fileService;

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집기준일

    protected List<Object> resultList = new ArrayList<>();

    protected ExecutionContext jobExecutionContext;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        if (Objects.isNull(this.baseDt)) {
            log.info("baseDt is null");
        }

        this.apiService.init(getCurrentJobId());
        this.fileService.init(getCurrentJobId(), this.baseDt);

        // step간 파라미터 넘겨주기 위해 jobExcutionContext 초기화
        // afterStep에서 넘겨줄 값 셋팅해준다
        this.jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        // 현재의 jobId를 공통으로 담아준다. FileMerge시 자동으로 jobId가 전달된다.
        this.jobExecutionContext.put("jobId", getCurrentJobId());

        return null;
    }

    /**
     * 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
     *
     * @return grpName 배치그룹명
     */
    protected String getJobGrpName() {

        String className = this.getClass().getSimpleName();

        // 클래스명 문자열을 잘라서 group name 추출 (Nav001Tasklet -> nav)
        String grpName = className.substring(0, 3).toLowerCase();

        return grpName;
    }

    /**
     * ClassName에서 JobId 추출하여 return ex) Nav001mTasklet -> nav001m
     *
     * @return jobId 배치잡ID
     */
    protected String getCurrentJobId() {

        String className = this.getClass().getSimpleName();
        String jobId = null;
        if (className.contains("Tasklet")) {
            jobId = className.replace("Tasklet", "").toLowerCase();
        } else {
            jobId = className;
        }
        return jobId;
    }

    /**
     * 배치 시작로그 출력
     */
    public void writeCmmnLogStart() {
        log.info("##########################################################################");
        log.info("START JOB :::: {} ", getCurrentJobId());
        log.info("##########################################################################");
        log.info("baseDt : {}", baseDt);
    }

    /**
     * 배치 종료로그 출력
     */
    public void writeCmmnLogEnd() {
        log.info("##########################################################################");
        log.info("END JOB :::: {}", getCurrentJobId());
        log.info("##########################################################################");
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
    public void writeCmmnLogEnd(String threadNum) {
//        log.info("##########################################################################");
        log.info("END JOB :::: {} #{}", getCurrentJobId(), threadNum);
        log.info("##########################################################################");
    }

}
