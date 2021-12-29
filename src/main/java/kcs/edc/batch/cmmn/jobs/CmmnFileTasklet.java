package kcs.edc.batch.cmmn.jobs;

import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.service.JobService;
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

/**
 * 파일 병합, 삭제와 관련된 공통 Tasklet
 */
@Slf4j
@StepScope
public class CmmnFileTasklet implements Tasklet {

    @Value("#{jobParameters[baseDt]}")
    protected String baseDt; // 수집기준일

    @Autowired
    protected JobService jobService;

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

        if(ObjectUtils.isEmpty(this.jobList)) {
            if(this.actionType.equals(CmmnProperties.CMMN_FILE_ACTION_TYPE_BACKUP_CLEAN)) {
                this.jobList = this.jobService.getJobList();
            }
        }

        for (String jobId : this.jobList) {

            log.info(">> jobId: {}", jobId);
            this.fileService.initFileVO(jobId);

            if(this.actionType.equals(CmmnProperties.CMMN_FILE_ACTION_TYPE_MERGE)) { // 파일 병합
                this.fileService.mergeTempFile();

            } else if(this.actionType.equals(CmmnProperties.CMMN_FILE_ACTION_TYPE_CLEAN)) { // 파일 삭제
                this.fileService.cleanTempFile();

            } else if(this.actionType.equals(CmmnProperties.CMMN_FILE_ACTION_TYPE_BACKUP_CLEAN)) { // 백업파일 삭제
                this.fileService.cleanBackupFile();
            }
        }

        log.info("END JOB :::: {}", this.getClass().getSimpleName());

        return RepeatStatus.FINISHED;
    }

}
