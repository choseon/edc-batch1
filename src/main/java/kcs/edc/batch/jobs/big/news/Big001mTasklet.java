package kcs.edc.batch.jobs.big.news;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.news.code.NewNationWideComCode;
import kcs.edc.batch.jobs.big.news.vo.Big001mVO;
import kcs.edc.batch.jobs.big.news.vo.NewsQueryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
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
 * News Search(뉴스검색)
 */
@Slf4j
public class Big001mTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    @Value("#{jobExecutionContext[newsClusterList]}")
    private List<List<String>> newsClusterList;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        writeCmmnLogStart();
        log.info("KcsKeywordYn: {}, issueSrwrYn: {}", this.kcsRgrsYn, this.issueSrwrYn);

        try {
            NewsQueryVO queryVO = new NewsQueryVO();
            String accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
            queryVO.setAccess_key(accessKey);
            queryVO.getArgument().getPublished_at().setFrom(DateUtil.getFormatDate(this.startDt));
            queryVO.getArgument().getPublished_at().setUntil(DateUtil.getFormatDate(this.endDt));

            NewNationWideComCode code = new NewNationWideComCode();

            if (!ObjectUtils.isEmpty(this.newsClusterList)) {

//                queryVO.getArgument().getPublished_at().setFrom(DateUtil.getFormatDate("20220110"));
//                queryVO.getArgument().getPublished_at().setUntil(DateUtil.getFormatDate("20220111"));

                // 뉴스상세검색
                for (List<String> newsCluster : this.newsClusterList) {
                    queryVO.getArgument().setNewsIds(newsCluster);
                    log.info("newsCluster: {}", newsCluster);

                    URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                    Big001mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);

                    if (resultVO.getResult() != 0) continue;

                    List<Big001mVO.DocumentItem> documents = resultVO.getReturn_object().getDocuments();
                    for (Big001mVO.DocumentItem item : documents) {
                        item.setSrchQuesWordNm(queryVO.getArgument().getQuery());
                        item.setOxprClsfNm(code.getNewsNationName(item.getOxprNm()));
                        item.setIssueSrwrYn(this.issueSrwrYn);
                        item.setKcsRgrsYn(this.kcsRgrsYn);
                        item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                        item.setLastChngDtlDttm(DateUtil.getCurrentTime());

                        this.resultList.add(item);
                    }
                    log.info("[{}/{}] {} >> documents.size: {}",
                            this.itemCnt++, this.newsClusterList.size(), this.jobId, documents.size());
                }

            } else {

                // 뉴스 키워드 검색
                for (String keyword : this.keywordList) {
                    queryVO.getArgument().setQuery(keyword);

                    URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                    Big001mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);

                    if (resultVO.getResult() != 0) continue;

                    List<Big001mVO.DocumentItem> documents = resultVO.getReturn_object().getDocuments();
                    for (Big001mVO.DocumentItem item : documents) {
                        item.setSrchQuesWordNm(keyword);
                        item.setOxprClsfNm(code.getNewsNationName(item.getOxprNm()));
                        item.setIssueSrwrYn(this.issueSrwrYn);
                        item.setKcsRgrsYn(this.kcsRgrsYn);
                        item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                        item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                        this.resultList.add(item);
                    }
                    log.info("[{}/{}] {} >> keyword: {}, documents.size: {}",
                            this.itemCnt++, this.keywordList.size(), this.jobId, keyword, documents.size());
                }
            }

            // 파일생성
            this.fileService.makeTempFile(this.resultList, DateUtil.getCurrentTime2());

        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.toString());
        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.toString());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.toString());
        } catch (RestClientException e) {
            this.makeErrorLog(e.toString());
        } catch (ParseException e) {
            this.makeErrorLog(e.toString());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }

}
