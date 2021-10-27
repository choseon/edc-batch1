package kcs.edc.batch.jobs.big.issue;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.issue.vo.Big002mVO;
import kcs.edc.batch.jobs.big.issue.vo.IssueRankQueryVO;
import kcs.edc.batch.jobs.big.news.vo.Big001mVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue Ranking (이슈랭킹)
 */
@Slf4j
public class Big002mTasklet extends CmmnJob implements Tasklet {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "N";
    private List<String> newsClusterList = new ArrayList<>();

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

        writeCmmnLogStart();

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        IssueRankQueryVO queryVO = new IssueRankQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().setDate(this.until);

        Big002mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big002mVO.class);
        if(resultVO.getResult() != 0) return RepeatStatus.FINISHED;

        List<Big002mVO.TopicItem> topics = resultVO.getReturn_object().getTopics();
        for (Big002mVO.TopicItem item : topics) {

            List<String> news_cluster = item.getArtcListCn();
            this.newsClusterList.addAll(news_cluster);

            item.setArtcPblsDt(this.until);
            item.setKcsRgrsYn(this.kcsRgrsYn);
            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime());

            // topic.newsClusterToString()

            this.resultList.add(item);
        }
        // 파일생성
        this.fileService.makeFile(resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        super.afterStep(stepExecution);

        this.jobExecutionContext.put("newsClusterList", newsClusterList);
        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }
}
