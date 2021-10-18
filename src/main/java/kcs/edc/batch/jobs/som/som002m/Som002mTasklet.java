package kcs.edc.batch.jobs.som.som002m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtils;
import kcs.edc.batch.jobs.som.som001m.vo.Som001mVO;
import kcs.edc.batch.jobs.som.som002m.vo.Som002mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * SomeTrend 문서 목록 수집 Tasklet
 */
@Slf4j
public class Som002mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    @Value("#{stepExecutionContext[threadNum]}")
    protected String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    protected List<Som001mVO> partitionList;

    @Override
    public void beforeStep(StepExecution stepExecution) {

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart(threadNum, partitionList.size());

        for (Som001mVO som001mVO : partitionList) {

            UriComponentsBuilder builder = getUriComponetsBuilder();
            builder.replaceQueryParam("startDate", som001mVO.getDate());
            builder.replaceQueryParam("endDate", som001mVO.getDate());
            builder.replaceQueryParam("source", som001mVO.getSource());

            List<String> keywordsExcFilterList = jobProp.getParam().get("keywordsExcFilterList[]");
            for (String filter : keywordsExcFilterList) {
                String keyword = "(" + som001mVO.getKeyword() + ")&&~(" + filter + ")";
                builder.queryParam("keyword", keyword);
            }
            builder.replaceQueryParam("keywordsExcFilterList[]", "");
            uri = builder.build().toUri();

            Som002mVO resultVO = sendApiForEntity(uri, Som002mVO.class);
            if(Objects.isNull(resultVO)) continue;

            log.info("[{} #{}] >> source :: {} | keyword :: {} | kcsKeywordYn :: {} | size :: {}",
                    getJobId(), threadNum, som001mVO.getSource(), som001mVO.getKeyword(), som001mVO.getRegistYn(), resultVO.getDocumentList().size());

            for (Som002mVO.Item item : resultVO.getDocumentList()) {
                item.setDate(som001mVO.getDate());
                item.setSource(som001mVO.getSource());

                Date documnetDate = new SimpleDateFormat("yyyyMMddHHMMSS").parse(item.getDocumentDate());
                String targetDate = new SimpleDateFormat("yyyy-MM-dd HH:MM:SS").format(documnetDate);
                item.setDocumentDate(targetDate);

                item.setRegistYn(som001mVO.getRegistYn());
                item.setFrstRgsrDtlDttm(DateUtils.getCurrentTime());
                item.setLastChngDtlDttm(DateUtils.getCurrentTime());

                resultList.add(item);
            }
        }

        makeTempFile(getJobId(), resultList);

        log.info("End thread Num : {}", threadNum);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        jobExecutionContext.put("jobId", getJobId());
        return ExitStatus.COMPLETED;
    }
}
