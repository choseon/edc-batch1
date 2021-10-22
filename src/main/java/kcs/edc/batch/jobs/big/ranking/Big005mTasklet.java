package kcs.edc.batch.jobs.big.ranking;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.ranking.vo.Big005mVO;
import kcs.edc.batch.jobs.big.ranking.vo.RankingQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;
import java.util.Objects;

/**
 * Query Ranking (인기검색어)
 */
@Slf4j
public class Big005mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    private String kcsRgrsYn = "Y";
    private String issueSrwrYn = "N";

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

        RankingQueryVO queryVO = new RankingQueryVO();
        queryVO.setAccess_key(accessKey);
        queryVO.getArgument().setFrom(from);
        queryVO.getArgument().setUntil(until);

        String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
        log.info("uri {}", uri);
        log.debug("resultJson {}", resultJson);

        if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

        Big005mVO resultVO = objectMapper.readValue(resultJson, Big005mVO.class);

        if(resultVO.getResult() != 0) return RepeatStatus.FINISHED;

        List<Big005mVO.QueryItem> queries = resultVO.getReturn_object().getQueries();
        for (Big005mVO.QueryItem item : queries) {
            item.setKcsRgrsYn(kcsRgrsYn);
            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime2());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime2());

            resultList.add(item);
        }
        // 파일생성
        makeFile("big004m", resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }
}
