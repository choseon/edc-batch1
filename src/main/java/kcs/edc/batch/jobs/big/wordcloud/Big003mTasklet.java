package kcs.edc.batch.jobs.big.wordcloud;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.issue.vo.Big002mVO;
import kcs.edc.batch.jobs.big.wordcloud.vo.Big003mVO;
import kcs.edc.batch.jobs.big.wordcloud.vo.WCQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * WordCloud(워드클라우드)
 */
@Slf4j
public class Big003mTasklet extends CmmnJob implements Tasklet{

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGrpName(), "accessKey");
        this.from = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -1, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -0, "yyyy-MM-dd");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        WCQueryVO queryVO = new WCQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().getPublished_at().setFrom(this.from);
        queryVO.getArgument().getPublished_at().setUntil(this.until);

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        for (String keyword : this.keywordList) {
            queryVO.getArgument().setQuery(keyword);

            Big003mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big003mVO.class);
            if(resultVO.getResult() != 0) continue;

            List<Big003mVO.NodeItem> nodes = resultVO.getReturn_object().getNodes();
            for (Big003mVO.NodeItem item : nodes) {
                item.setArtcPblsDt(this.until);
                item.setSrchQuesWordNm(keyword);
                item.setKcsRgrsYn(this.kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());

                this.resultList.add(item);
            }
        }
        // 파일생성
        this.fileService.makeFile(resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        super.afterStep(stepExecution);

        this.jobExecutionContext.put("keywordList", keywordList);
        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }

}
