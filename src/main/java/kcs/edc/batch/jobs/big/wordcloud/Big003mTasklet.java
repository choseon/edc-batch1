package kcs.edc.batch.jobs.big.wordcloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.wordcloud.vo.Big003mVO;
import kcs.edc.batch.jobs.big.wordcloud.vo.WCQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

/**
 * WordCloud(워드클라우드)
 */
@Slf4j
public class Big003mTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        this.jobExecutionContext.put("keywordList", keywordList);
        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();
        log.info("KcsKeywordYn: {}, issueSrwrYn: {}", this.kcsRgrsYn, this.issueSrwrYn);

        try {
            WCQueryVO queryVO = new WCQueryVO();
            String accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
            queryVO.setAccess_key(accessKey);
            queryVO.getArgument().getPublished_at().setFrom(DateUtil.getFormatDate(this.startDt));
            queryVO.getArgument().getPublished_at().setUntil(DateUtil.getFormatDate(this.endDt));

            if(ObjectUtils.isEmpty(this.keywordList)) {
                throw new NullPointerException("keywordList is null");
            }

            for (String keyword : this.keywordList) {
                queryVO.getArgument().setQuery(keyword);

                URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                Big003mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big003mVO.class);

                if (resultVO.getResult() != 0) {
                    log.info("resultVO.getResult(): {}", resultVO.getResult());
                    continue;
                }

                List<Big003mVO.NodeItem> nodes = resultVO.getReturn_object().getNodes();
                for (Big003mVO.NodeItem item : nodes) {
                    item.setArtcPblsDt(this.endDt);
                    item.setSrchQuesWordNm(keyword);
                    item.setKcsRgrsYn(this.kcsRgrsYn);
                    item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                    item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                    this.resultList.add(item);

                    log.info("[{}] {} >> keyword: {}, date: {}, name: {}",
                            this.itemCnt++, this.jobId, keyword, this.endDt, item.getName());
                }
            }

            // 파일생성
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
        } catch (NullPointerException e) {
            this.makeErrorLog(e.toString());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}
