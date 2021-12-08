package kcs.edc.batch.jobs.big.cmmn;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import lombok.SneakyThrows;
import org.springframework.batch.core.StepExecution;

public class BigCmmnJob extends CmmnJob {

    private String kcsRgrsYn = "Y";
    private String issueSrwrYn = "N";

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
        this.from = DateUtil.getOffsetDate(this.baseDt, 0, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(this.baseDt, 1, "yyyy-MM-dd");
    }
}
