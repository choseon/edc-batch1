package kcs.edc.batch.jobs.som.som003m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtils;
import kcs.edc.batch.jobs.som.som001m.vo.Som001mVO;
import kcs.edc.batch.jobs.som.som003m.vo.Som003mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

/**
 * SomeTrend 연관어 수집 Tasklet
 */
@Slf4j
public class Som003mTasklet extends CmmnTask implements Tasklet {

    @Value("#{stepExecutionContext[threadNum]}")
    protected String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    protected List<Som001mVO> partitionList;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();
        log.info("thread Num :: {}, partitionList.size() :: {}", threadNum, partitionList.size());

        for (Som001mVO som001mVO : partitionList) {

            UriComponentsBuilder builder = getUriComponetsBuilder();
            builder.replaceQueryParam("startDate", som001mVO.getDate());
            builder.replaceQueryParam("endDate", som001mVO.getDate());
            builder.replaceQueryParam("source", som001mVO.getSource());
            builder.replaceQueryParam("keyword", som001mVO.getKeyword());
            uri = builder.build().toUri();

            Som003mVO resultVO = sendApiForEntity(uri, Som003mVO.class);
            if(Objects.isNull(resultVO)) continue;

            log.info("[{}] >> source :: {} | keyword :: {} | kcsKeywordYn :: {} | size :: {}",
                    getJobId(), som001mVO.getSource(), som001mVO.getKeyword(), som001mVO.getRegistYn(), resultVO.getChildList().size());

            for (Som003mVO.Item item : resultVO.getChildList()) {

                item.setDate(som001mVO.getDate());
                item.setSource(som001mVO.getSource());
                item.setRegistYn(som001mVO.getRegistYn());
                item.setKeyword(som001mVO.getKeyword());
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


}
