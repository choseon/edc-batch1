package kcs.edc.batch.jobs.big.ranking;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
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
import org.springframework.web.client.RestClientException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Ranking (인기검색어)
 */
@Slf4j
public class Big005mTasklet extends CmmnJob implements Tasklet, StepExecutionListener {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "N";

    private String from;
    private String until;
    private String accessKey;

    private List<String> keywordList = new ArrayList<>();

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

        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);
        this.jobExecutionContext.put("keywordList", keywordList);

        return ExitStatus.COMPLETED;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();
        log.info("from: {}, until: {}, KcsKeywordYn: {}, issueSrwrYn: {}", this.from, this.until, this.kcsRgrsYn, this.issueSrwrYn);

        try {
            URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

            RankingQueryVO queryVO = new RankingQueryVO();
            queryVO.setAccess_key(this.accessKey);
            queryVO.getArgument().setFrom(this.from);
            queryVO.getArgument().setUntil(this.until);

            Big005mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big005mVO.class);

            List<Big005mVO.QueryItem> queries = resultVO.getReturn_object().getQueries();
            for (Big005mVO.QueryItem item : queries) {
                item.setKcsRgrsYn(this.kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());

                this.resultList.add(item);
                this.keywordList.add(item.getQuery());

                log.info("[{}/{}] {} >> query : {}, count: {}",
                        this.itemCnt++, queries.size(), this.jobId, item.getQuery(), item.getCount());
            }

            // 파일생성
//            this.fileService.makeFile(CmmnConst.JOB_ID_BIG004M, resultList, true);
            this.fileService.makeTempFile(CmmnConst.JOB_ID_BIG004M, this.resultList, DateUtil.getCurrentTime2());

        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
            return null;
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
            return null;
        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}
