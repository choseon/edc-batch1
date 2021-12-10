package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 파일 병합, 삭제와 관련된 공통 Tasklet
 */
@Slf4j
@StepScope
public class CmmnFileTasklet implements Tasklet {

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집기준일

    @Autowired
    private FileService fileService = new FileService();

    private String actionType;

    private List<String> jobList = new ArrayList<>();

    @Value("#{jobExecutionContext[jobId]}")
    public String jobId;

    public CmmnFileTasklet(String actionType) {
        this.actionType = actionType;
    }

    public CmmnFileTasklet(String actionType, List<String> jobList) {
        this(actionType);
        this.jobList = jobList;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        log.info("####################################################");
        log.info("START JOB :::: {} ", this.getClass().getSimpleName());
        log.info("####################################################");
        log.info("actionType : {}", this.actionType);

        if(ObjectUtils.isEmpty(this.jobId)) {
            this.jobList.add(this.jobId);
        }

        for (String jobId : this.jobList) {

            log.info(">> jobId: {}", jobId);
            this.fileService.initFileVO(jobId);

            if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE)) {
                this.fileService.mergeTempFile(jobId);
            } else if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN)) {
                this.fileService.cleanTempFile(jobId);
            }
        }

        log.info("END JOB :::: {}", this.getClass().getSimpleName());

        return RepeatStatus.FINISHED;
    }

    /*    @Autowired
    private FileService fileService = new FileService();

    private String actionType;

    @Value("#{jobExecutionContext[jobId]}")
    public String jobId;

    private List<String> jobList;

    public CmmnFileTasklet(String actionType) {
        this.actionType = actionType;
    }

    public CmmnFileTasklet(String actionType, List<String> jobList) {
        this.actionType = actionType;
        this.jobList = jobList;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        log.info("####################################################");
        log.info("START JOB :::: {} ", this.getClass().getSimpleName());
        log.info("####################################################");
        log.info("actionType : {}", this.actionType);

        if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE)) { // file merge

            log.info("jobId: {}", this.jobId);
            this.fileService.mergeTempFile(this.jobId);

        } else if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN)) { // file clean

            if(Objects.nonNull(this.jobList)) {

                for (String jobId : this.jobList) {
                    log.info("jobId: {}", jobId);
                    this.fileService.cleanTempFile(jobId);
                }
            } else {
                log.info("jobList is null");
            }
        }

        log.info("END JOB :::: {}", this.getClass().getSimpleName());

        return RepeatStatus.FINISHED;
    }*/
}
