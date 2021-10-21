package kcs.edc.batch.jobs.big.timeline;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.big.timeline.vo.Big004mVO;
import kcs.edc.batch.jobs.big.timeline.vo.TimelineQueryVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * News TimeLine (뉴스 타임라인)
 */
@Slf4j
public class Big004mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    private List<String> kcsKeywordList;
    private String kcsRgrsYn = "Y";
    private String issueSrwrYn = "N";


    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
        jobProp = apiProperty.getJobProp(getJobGrpName());
        accessKey = jobProp.getHeader().get("accessKey");

        from = DateUtil.getOffsetDate(DateUtil.getFormatDate(cletDt), -1, "yyyy-MM-dd");
        until = DateUtil.getOffsetDate(DateUtil.getFormatDate(cletDt), -0, "yyyy-MM-dd");

        try {
            String resourcePath = fileProperty.getResourcePath();
            String filePath = resourcePath + JobConstant.RESOURCE_FILE_NAME_SOM_KCS_KEWORD;
            kcsKeywordList = FileUtil.readTextFile(filePath);
            log.info("kcsKeywordList.size() {}", kcsKeywordList.size());

        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        uri = getUriComponetsBuilder().build().toUri();

        TimelineQueryVO queryVO = new TimelineQueryVO();
        queryVO.setAccess_key(accessKey);
        queryVO.getArgument().getPublished_at().setFrom(from);
        queryVO.getArgument().getPublished_at().setUntil(until);
        log.info("from {} until {}", from, until);

        for(String keyword : kcsKeywordList) {
            queryVO.getArgument().setQuery(keyword);

            String resultJson = restTemplate.postForObject(uri, queryVO, String.class);
            log.info("uri {}", uri);
            log.debug("resultJson {}", resultJson);

            if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

            Big004mVO resultVO = objectMapper.readValue(resultJson, Big004mVO.class);

            if(resultVO.getResult() != 0) continue;

            List<Big004mVO.TimeLineItem> time_line = resultVO.getReturn_object().getTime_line();
            for (Big004mVO.TimeLineItem item : time_line) {
                item.setSrchQuesWordNm(keyword);
                item.setKcsRgrsYn(kcsRgrsYn);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime2());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime2());

                resultList.add(item);
            }
        }
        // 파일생성
        makeFile(getCurrentJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        jobExecutionContext.put("keywordList", kcsKeywordList);
        jobExecutionContext.put("kcsRgrsYn", kcsRgrsYn);
        jobExecutionContext.put("issueSrwrYn", issueSrwrYn);

        return ExitStatus.COMPLETED;
    }
}
