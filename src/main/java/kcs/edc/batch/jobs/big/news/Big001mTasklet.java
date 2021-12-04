package kcs.edc.batch.jobs.big.news;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.big.news.code.NewNationWideComCode;
import kcs.edc.batch.jobs.big.news.vo.Big001mVO;
import kcs.edc.batch.jobs.big.news.vo.NewsQueryVO;
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
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * News Search(뉴스검색)
 */
@Slf4j
public class Big001mTasklet extends CmmnJob implements Tasklet, StepExecutionListener {

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    @Value("#{jobExecutionContext[newsClusterList]}")
    private List<List<String>> newsClusterList;

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGroupId(), "accessKey");
        this.from = DateUtil.getOffsetDate(this.baseDt, 0, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(this.baseDt, 1, "yyyy-MM-dd");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        writeCmmnLogStart();
        log.info("from: {}, until: {}", this.from, this.until);

        NewsQueryVO queryVO = new NewsQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().getPublished_at().setFrom(this.from);
        queryVO.getArgument().getPublished_at().setUntil(this.until);

        NewNationWideComCode code = new NewNationWideComCode();

        if (!ObjectUtils.isEmpty(this.newsClusterList)) {

            // 뉴스상세검색
            for (List<String> nesClusters : this.newsClusterList) {
                queryVO.getArgument().setNewsIds(nesClusters);

                URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                Big001mVO resultVO = null;
                try {
                    resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);
                } catch (JsonProcessingException e) {
                    this.processError(e.getMessage());
                    return null;
                } catch (RestClientException e) {
                    this.processError(e.getMessage());
                    return null;
                }

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
                log.info("{} >> newsClusterList.size : {}, documents.size: {}, KcsKeywordYn : {}",
                        getCurrentJobId(), nesClusters.size(), documents.size(), this.kcsRgrsYn);

            }
        } else {

            // 뉴스 키워드 검색
            for (String keyword : this.keywordList) {
                queryVO.getArgument().setQuery(keyword);

                URI uri = this.apiService.getUriComponetsBuilder().build().toUri();
                Big001mVO resultVO = null;
                try {
                    resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);
                } catch (JsonProcessingException e) {
                    this.processError(e.getMessage());
                    return null;
                } catch (RestClientException e) {
                    this.processError(e.getMessage());
                    return null;
                }
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
                log.info("{} >> keyword : {}, documents.size: {}, KcsKeywordYn : {}",
                        getCurrentJobId(), keyword, documents.size(), this.kcsRgrsYn);
            }
        }

        // 파일생성
        this.fileService.makeFile(this.resultList, true);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

}
