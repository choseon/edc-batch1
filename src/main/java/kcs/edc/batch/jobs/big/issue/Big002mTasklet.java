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
    private String issueSrwrYn = "N";
    private List<String> newsClusterList = new ArrayList<>();

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
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        IssueRankQueryVO queryVO = new IssueRankQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().setDate(this.until);

        Big002mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big002mVO.class);
        if(resultVO.getResult() != 0) return RepeatStatus.FINISHED;

        List<Big002mVO.TopicItem> topics = resultVO.getReturn_object().getTopics();
        for (Big002mVO.TopicItem item : topics) {

            List<String> newsCluster = item.getNewsCluster();
            this.newsClusterList.addAll(newsCluster);

            item.setArtcListCn(convertNesClusterListToString(newsCluster));
            item.setArtcPblsDt(this.baseDt);
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

    public String convertNesClusterListToString(List<String> newsClusterList) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = newsClusterList.iterator();

        while (iterator.hasNext()) {
            String str = (String) iterator.next();
            sb.append("\"").append(str).append("\"").append(",");
        }

        String clusterStr = sb.toString();
        return clusterStr.substring(0, clusterStr.length() - 1) + "]";
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        this.jobExecutionContext.put("newsClusterList", this.newsClusterList);
        this.jobExecutionContext.put("kcsRgrsYn", this.kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", this.issueSrwrYn);

        return super.afterStep(stepExecution);
    }
}
