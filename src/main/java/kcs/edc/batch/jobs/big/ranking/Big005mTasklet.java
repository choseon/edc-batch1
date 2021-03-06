package kcs.edc.batch.jobs.big.ranking;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.ranking.vo.Big005mVO;
import kcs.edc.batch.jobs.big.ranking.vo.RankingQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.client.RestClientException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Query Ranking (인기검색어)
 */
@Slf4j
public class Big005mTasklet extends CmmnJob implements Tasklet {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "N";

    private List<String> keywordList = new ArrayList<>();

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
        log.info("KcsKeywordYn: {}, issueSrwrYn: {}", this.kcsRgrsYn, this.issueSrwrYn);

        try {
            URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

            RankingQueryVO queryVO = new RankingQueryVO();
            String accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
            queryVO.setAccess_key(accessKey);
            queryVO.getArgument().setFrom(DateUtil.getFormatDate(this.startDt));
            queryVO.getArgument().setUntil(DateUtil.getFormatDate(this.endDt));

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
            this.fileService.initFileVO(CmmnProperties.JOB_ID_BIG004M);
            this.fileService.makeTempFile(this.resultList, DateUtil.getCurrentTime2());

        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.toString());
        } catch (RestClientException e) {
            this.makeErrorLog(e.toString());
        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.toString());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.toString());
        } catch (ParseException e) {
            this.makeErrorLog(e.toString());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}
