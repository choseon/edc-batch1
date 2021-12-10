package kcs.edc.batch.jobs.big.issue;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.client.RestClientException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Issue Ranking (이슈랭킹)
 */
@Slf4j
public class Big002mTasklet extends CmmnJob implements Tasklet {

    private String kcsRgrsYn = "N";
    private String issueSrwrYn = "Y";
    private List<List<String>> newsClusterList = new ArrayList<>();

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        this.jobExecutionContext.put("newsClusterList", this.newsClusterList);
        this.jobExecutionContext.put("kcsRgrsYn", this.kcsRgrsYn);
        this.jobExecutionContext.put("issueSrwrYn", this.issueSrwrYn);

        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();
        log.info("KcsKeywordYn: {}, issueSrwrYn: {}", this.kcsRgrsYn, this.issueSrwrYn);

        try {

            for (int i = 0; i < this.period; i++) {
                IssueRankQueryVO queryVO = new IssueRankQueryVO();
                String accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
                queryVO.setAccess_key(accessKey);

                String date = DateUtil.getOffsetDate(this.startDt, i, "yyyy-MM-dd");
                queryVO.getArgument().setDate(date); // ex) 2021-11-24

                URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                Big002mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big002mVO.class);

                List<Big002mVO.TopicItem> topics = resultVO.getReturn_object().getTopics();
                if(topics == null) {
                    this.makeErrorLog("topics is null");
                }

                for (Big002mVO.TopicItem item : topics) {

                    List<String> newsCluster = item.getNews_cluster();
                    this.newsClusterList.add(newsCluster);

//            item.setNews_cluster(convertNesClusterListToString(newsCluster));
                    item.setArtcPblsDt(date); // 20211124
                    item.setKcsRgrsYn(this.kcsRgrsYn);
                    item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                    item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                    this.resultList.add(item);
                    log.info("[{}/{}] {} >> newsCluster.size: {}",
                            this.itemCnt++, topics.size(), this.jobId, newsCluster.size());
                }
            }

            // 파일생성
            this.fileService.makeFile(this.resultList, true);

        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
        } catch (ParseException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}
