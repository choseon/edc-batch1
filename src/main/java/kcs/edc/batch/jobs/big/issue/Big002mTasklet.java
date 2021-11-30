package kcs.edc.batch.jobs.big.issue;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.issue.vo.Big002mVO;
import kcs.edc.batch.jobs.big.issue.vo.IssueRankQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Issue Ranking (이슈랭킹)
 */
@Slf4j
public class Big002mTasklet extends CmmnJob implements Tasklet {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "Y";
    private List<List<String>> newsClusterList = new ArrayList<>();

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
        this.from = DateUtil.getOffsetDate(this.baseDt, 0, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(this.baseDt, 1, "yyyy-MM-dd");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        this.jobExecutionContext.put("newsClusterList", this.newsClusterList);
        this.jobExecutionContext.put("kcsRgrsYn", this.kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", this.issueSrwrYn);

        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        IssueRankQueryVO queryVO = new IssueRankQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().setDate(this.until); // ex) 2021-11-24

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
        Big002mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big002mVO.class);
        if(resultVO.getResult() != 0) {
            log.info("resultVO.getResult(): {}", resultVO.getReason());
            return null;
        }

        List<Big002mVO.TopicItem> topics = resultVO.getReturn_object().getTopics();
        for (Big002mVO.TopicItem item : topics) {

            List<String> newsCluster = item.getNews_cluster();
            this.newsClusterList.add(newsCluster);

//            item.setNews_cluster(convertNesClusterListToString(newsCluster));
            item.setArtcPblsDt(this.baseDt); // 20211124
            item.setKcsRgrsYn(this.kcsRgrsYn);
            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime());
            this.resultList.add(item);
        }
        log.info("{} >> topics.size : {}, KcsKeywordYn : {}", getCurrentJobId(), topics.size(), this.kcsRgrsYn);

        // 파일생성
        this.fileService.makeFile(this.resultList, true);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
