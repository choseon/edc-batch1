package kcs.edc.batch.jobs.big.news;

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

import java.net.URI;
import java.util.List;

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
    private List<String> newsClusterList;

    private String from;
    private String until;
    private String accessKey;

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.accessKey = this.apiService.getJobPropHeader(getJobGrpName(), "accessKey");
        this.from = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -1, "yyyy-MM-dd");
        this.until = DateUtil.getOffsetDate(DateUtil.getFormatDate(this.baseDt), -0, "yyyy-MM-dd");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        NewsQueryVO queryVO = new NewsQueryVO();
        queryVO.setAccess_key(this.accessKey);
        queryVO.getArgument().getPublished_at().setFrom(this.from);
        queryVO.getArgument().getPublished_at().setUntil(this.until);

        NewNationWideComCode code = new NewNationWideComCode();

        if(this.newsClusterList != null) { // 뉴스상세검색

            queryVO.getArgument().setNewsIds(this.newsClusterList);

            Big001mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);

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

        } else if(this.keywordList != null) { // 뉴스 키워드 검색

            for (String keyword : this.keywordList) {
                queryVO.getArgument().setQuery(keyword);

                Big001mVO resultVO = this.apiService.sendApiPostForObject(uri, queryVO, Big001mVO.class);
                if(resultVO.getResult() != 0) continue;

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
            }
        }

        // 파일생성
        this.fileService.makeFile(this.resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

}
