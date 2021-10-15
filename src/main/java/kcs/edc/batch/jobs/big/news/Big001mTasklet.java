package kcs.edc.batch.jobs.big.news;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtils;
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

import java.util.List;
import java.util.Objects;

/**
 * News Search(뉴스검색)
 */
@Slf4j
public class Big001mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    @Value("#{jobExecutionContext[keywordList]}")
    private List<String> keywordList;

    @Value("#{jobExecutionContext[kcsRgrsYn]}")
    private String kcsRgrsYn;

    @Value("#{jobExecutionContext[issueSrwrYn]}")
    private String issueSrwrYn;

    @Value("#{jobExecutionContext[newsClusterList]}")
    private List<String> newsClusterList;

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

        uri = getUriComponetsBuilder().build().toUri();

        NewsQueryVO queryVO = new NewsQueryVO();
        queryVO.setAccess_key(accessKey);
        queryVO.getArgument().getPublished_at().setFrom(from);
        queryVO.getArgument().getPublished_at().setUntil(until);

        NewNationWideComCode code = new NewNationWideComCode();

        if(newsClusterList != null) { // 뉴스상세검색

            queryVO.getArgument().setNewsIds(newsClusterList);

            String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
            log.info("uri {}", uri);
            log.debug("resultJson {}", resultJson);

            if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

            Big001mVO resultVO = objectMapper.readValue(resultJson, Big001mVO.class);

            List<Big001mVO.DocumentItem> documents = resultVO.getReturn_object().getDocuments();
            for (Big001mVO.DocumentItem item : documents) {
                item.setSrchQuesWordNm(queryVO.getArgument().getQuery());
                item.setOxprClsfNm(code.getNewsNationName(item.getOxprNm()));
                item.setIssueSrwrYn(issueSrwrYn);
                item.setKcsRgrsYn(kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtils.getCurrentTime2());
                item.setLastChngDtlDttm(DateUtils.getCurrentTime2());

                resultList.add(item);
            }

        } else if(keywordList != null) { // 뉴스 키워드 검색

            for (String keyword : keywordList) {
                queryVO.getArgument().setQuery(keyword);

                String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
                log.info("uri {}", uri);
                log.info("resultJson {}", resultJson);

                if(resultJson == null) continue;

                Big001mVO resultVO = objectMapper.readValue(resultJson, Big001mVO.class);

                if(resultVO.getResult() != 0) continue;

                List<Big001mVO.DocumentItem> documents = resultVO.getReturn_object().getDocuments();
                for (Big001mVO.DocumentItem item : documents) {
                    item.setSrchQuesWordNm(keyword);
                    item.setOxprClsfNm(code.getNewsNationName(item.getOxprNm()));
                    item.setIssueSrwrYn(issueSrwrYn);
                    item.setKcsRgrsYn(kcsRgrsYn);
                    item.setFrstRgsrDtlDttm(DateUtils.getCurrentTime2());
                    item.setLastChngDtlDttm(DateUtils.getCurrentTime2());

                    resultList.add(item);
                }
            }
        }
        // 파일생성
        makeFile(getJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }
}
