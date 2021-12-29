package kcs.edc.batch.jobs.big.timeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.big.timeline.vo.Big004mVO;
import kcs.edc.batch.jobs.big.timeline.vo.TimelineQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

/**
 * News TimeLine (뉴스 타임라인)
 */
@Slf4j
public class Big004mTasklet extends CmmnJob implements Tasklet {

    private List<String> kcsKeywordList;
    private String kcsRgrsYn = "Y";
    private String issueSrwrYn = "N";

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("keywordList", kcsKeywordList);
        this.jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();
        log.info("KcsKeywordYn: {}, issueSrwrYn: {}", this.kcsRgrsYn, this.issueSrwrYn);

        try {
            String resourcePath = this.fileService.getResourcePath();
            String filePath = resourcePath + CmmnProperties.RESOURCE_FILE_NAME_KCS_KEYWORD;
            this.kcsKeywordList = FileUtil.readTextFile(filePath);

            TimelineQueryVO queryVO = new TimelineQueryVO();
            String accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
            queryVO.setAccess_key(accessKey);
            queryVO.getArgument().getPublished_at().setFrom(DateUtil.getFormatDate(this.startDt));
            queryVO.getArgument().getPublished_at().setUntil(DateUtil.getFormatDate(this.endDt));

            for (String keyword : this.kcsKeywordList) {
                queryVO.getArgument().setQuery(keyword);

                URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

                Big004mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big004mVO.class);


                if (resultVO.getResult() != 0) {
                    log.info("[{}/{}] keyword: {}, resultVO.getResult(): {}", this.itemCnt, this.kcsKeywordList.size(), keyword, resultVO.getResult());
                } else {

                    List<Big004mVO.TimeLineItem> time_line = resultVO.getReturn_object().getTime_line();
                    for (Big004mVO.TimeLineItem item : time_line) {
                        item.setSrchQuesWordNm(keyword);
                        item.setKcsRgrsYn(this.kcsRgrsYn);
                        item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                        item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                        this.resultList.add(item);

                        log.info("[{}/{}] {} >> keyword: {}, date: {}, hits: {}",
                                this.itemCnt, this.kcsKeywordList.size(), this.jobId, keyword, item.getArtcPblsDt(), item.getSrchDocGcnt());
                    }
                }
                this.itemCnt++;
            }

            // 파일생성
            this.fileService.makeTempFile(this.resultList, DateUtil.getCurrentTime2());

        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } catch (ParseException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }
        return RepeatStatus.FINISHED;
    }
}
