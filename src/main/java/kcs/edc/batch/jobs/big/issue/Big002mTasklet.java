package kcs.edc.batch.jobs.big.issue;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.issue.vo.Big002mVO;
import kcs.edc.batch.jobs.big.issue.vo.IssueRankQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue Ranking (이슈랭킹)
 */
@Slf4j
public class Big002mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "N";
    private List<String> newsClusterList = new ArrayList<>();

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
        jobProp = apiProperty.getJobProp(getJobGrpName());
        accessKey = jobProp.getHeader().get("accessKey");

        from = DateUtil.getOffsetDate(DateUtil.getFormatDate(baseDt), -1, "yyyy-MM-dd");
        until = DateUtil.getOffsetDate(DateUtil.getFormatDate(baseDt), -0, "yyyy-MM-dd");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        uri = getUriComponetsBuilder().build().toUri();

        IssueRankQueryVO queryVO = new IssueRankQueryVO();
        queryVO.setAccess_key(accessKey);
        queryVO.getArgument().setDate(until);

        String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
        log.info("uri {}", uri);
        log.debug("resultJson {}", resultJson);

        if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

        Big002mVO resultVO = objectMapper.readValue(resultJson, Big002mVO.class);

        if(resultVO.getResult() != 0) return RepeatStatus.FINISHED;

        List<Big002mVO.TopicItem> topics = resultVO.getReturn_object().getTopics();
        for (Big002mVO.TopicItem item : topics) {

            List<String> news_cluster = item.getArtcListCn();
            newsClusterList.addAll(news_cluster);

            item.setArtcPblsDt(until);
            item.setKcsRgrsYn(kcsRgrsYn);
            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime2());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime2());

            // topic.newsClusterToString()

            resultList.add(item);
        }
        // 파일생성
        makeFile(getCurrentJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        jobExecutionContext.put("newsClusterList", newsClusterList);
        jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }
}
