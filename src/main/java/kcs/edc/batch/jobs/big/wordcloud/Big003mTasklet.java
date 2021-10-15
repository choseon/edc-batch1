package kcs.edc.batch.jobs.big.wordcloud;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtils;
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

import java.util.List;
import java.util.Objects;

/**
 * WordCloud(워드클라우드)
 */
@Slf4j
public class Big003mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
        jobProp = apiProperty.getJobProp(getJobGrpName());
        accessKey = jobProp.getHeader().get("accessKey");

        from = DateUtils.getOffsetDate(DateUtils.getFormatDate(cletDt), -1, "yyyy-MM-dd");
        until = DateUtils.getOffsetDate(DateUtils.getFormatDate(cletDt), -0, "yyyy-MM-dd");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        WCQueryVO queryVO = new WCQueryVO();
        queryVO.setAccess_key(accessKey);
        queryVO.getArgument().getPublished_at().setFrom(from);
        queryVO.getArgument().getPublished_at().setUntil(until);

        uri = getUriComponetsBuilder().build().toUri();

        for (String keyword : keywordList) {
            queryVO.getArgument().setQuery(keyword);

            String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
            log.info("uri {}", uri);
            log.debug("resultJson {}", resultJson);

            if(Objects.isNull(resultJson)) continue;

            Big003mVO resultVO = objectMapper.readValue(resultJson, Big003mVO.class);

            if(resultVO.getResult() != 0) continue;

            List<Big003mVO.NodeItem> nodes = resultVO.getReturn_object().getNodes();
            for (Big003mVO.NodeItem item : nodes) {
                item.setArtcPblsDt(until);
                item.setSrchQuesWordNm(keyword);
                item.setKcsRgrsYn(kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtils.getCurrentTime2());
                item.setLastChngDtlDttm(DateUtils.getCurrentTime2());

                resultList.add(item);
            }
        }
        // 파일생성
        makeFile(getJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("keywordList", keywordList);
        jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }

}
