package kcs.edc.batch.jobs.big.timeline;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.big.timeline.vo.Big004mVO;
import kcs.edc.batch.jobs.big.timeline.vo.TimelineQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * News TimeLine (뉴스 타임라인)
 */
@Slf4j
public class Big004mTasklet extends CmmnJob implements Tasklet, StepExecutionListener {

    private List<String> kcsKeywordList;
    private String kcsRgrsYn = "Y";
    private String issueSrwrYn = "N";

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
        this.from = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -1, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -0, "yyyy-MM-dd");

        try {
            String resourcePath = this.fileService.getResourcePath();
            String filePath = resourcePath + CmmnConst.RESOURCE_FILE_NAME_KCS_KEYWORD;
            this.kcsKeywordList = FileUtil.readTextFile(filePath);
            log.info("kcsKeywordList.size() {}", kcsKeywordList.size());

        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("keywordList", kcsKeywordList);
        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        TimelineQueryVO queryVO = new TimelineQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().getPublished_at().setFrom(this.from);
        queryVO.getArgument().getPublished_at().setUntil(this.until);

        for(String keyword : this.kcsKeywordList) {
            queryVO.getArgument().setQuery(keyword);

            Big004mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big004mVO.class);
            if(resultVO.getResult() != 0) continue;

            List<Big004mVO.TimeLineItem> time_line = resultVO.getReturn_object().getTime_line();
            for (Big004mVO.TimeLineItem item : time_line) {
                item.setSrchQuesWordNm(keyword);
                item.setKcsRgrsYn(this.kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                this.resultList.add(item);

                log.info("{} >> keyword : {}, KcsKeywordYn : {}", getCurrentJobId(), keyword, this.kcsRgrsYn);
            }
        }
        // 파일생성
        this.fileService.makeFile(this.resultList, true);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
