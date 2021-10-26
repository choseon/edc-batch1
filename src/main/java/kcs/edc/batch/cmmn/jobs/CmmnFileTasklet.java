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

import java.util.List;
import java.util.Objects;

@Slf4j
public class CmmnFileTasklet implements Tasklet {

    @Autowired
    private FileService fileService = new FileService();

    private String actionType;

    private String jobId;
    private List<String> jobList;

    public CmmnFileTasklet(String actionType, List<String> jobList) {
        this.actionType = actionType;
        this.jobList = jobList;
    }

    public CmmnFileTasklet(String actionType, String jobId) {
        this.actionType = actionType;
        this.jobId = jobId;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        log.info("####################################################");
        log.info("START JOB :::: {} ", this.getClass().getSimpleName());
        log.info("####################################################");
        log.info("actionType : {}", this.actionType);
        log.info("jobId: {}", this.jobId);

        if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_MERGE)) { // file merge

            this.fileService.mergeTempFile(this.jobId);

        } else if(this.actionType.equals(CmmnConst.CMMN_FILE_ACTION_TYPE_CLEAN)) { // file clean
            if(Objects.nonNull(this.jobList)) {

                for (String jobId : this.jobList) {
                    this.fileService.cleanTempFile(jobId);
                }
            } else {
                this.fileService.cleanTempFile(this.jobId);
            }
        }

        log.info("####################################################");
        log.info("END JOB :::: {}", this.getClass().getSimpleName());
        log.info("####################################################");

        return RepeatStatus.FINISHED;
    }
}
