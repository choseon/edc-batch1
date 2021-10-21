package kcs.edc.batch.jobs.som.som004m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.som.som001m.vo.Som001mVO;
import kcs.edc.batch.jobs.som.som004m.vo.Som004mVO;
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
 * SomeTrend 연관어(감성) 수집 Tasklet
 */
@Slf4j
public class Som004mTasklet extends CmmnJob implements Tasklet {

    @Value("#{stepExecutionContext[threadNum]}")
    protected String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    protected List<Som001mVO> partitionList;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart(this.threadNum, this.partitionList.size());

        for (Som001mVO som001mVO : this.partitionList) {

            UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
            builder.replaceQueryParam("startDate", som001mVO.getDate());
            builder.replaceQueryParam("endDate", som001mVO.getDate());
            builder.replaceQueryParam("source", som001mVO.getSource());
            builder.replaceQueryParam("keyword", som001mVO.getKeyword());
            this.uri = builder.build().toUri();

            Som004mVO resultVO = this.apiService.sendApiForEntity(this.uri, Som004mVO.class);
            if (Objects.isNull(resultVO)) continue;

            log.info("[{}] >> source :: {} | keyword :: {} | kcsKeywordYn :: {} | size :: {}",
                    getCurrentJobId(), som001mVO.getSource(), som001mVO.getKeyword(), som001mVO.getRegistYn(), resultVO.getChildList().size());

            for (Som004mVO.Item item : resultVO.getChildList()) {

                item.setDate(som001mVO.getDate());
                item.setSource(som001mVO.getSource());
                item.setRegistYn(som001mVO.getRegistYn());
                item.setKeyword(som001mVO.getKeyword());

                String polarity = item.getCategoryList().get(0).substring(0, 6) + "0";
                List<String> categoryList = this.apiService.getjobPropParam("categoryList[]");
                String sensWordClsfNm = convertPoNeNtOt(categoryList, polarity);
                item.setSensWordClsfNm(sensWordClsfNm);

                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());

                this.resultList.add(item);
            }
        }

        this.fileService.makeTempFile(this.resultList);

        this.writeCmmnLogEnd(this.threadNum);

        return RepeatStatus.FINISHED;
    }

    private String convertPoNeNtOt(List<String> categoryList, String polarity) {
        List<String> positiveCode = categoryList.subList(0, 2);
        List<String> negativeCode = categoryList.subList(2, 4);
        List<String> neutralCode = categoryList.subList(4, 5);
        if (positiveCode.contains(polarity)) {
            return "po";
        } else if (negativeCode.contains(polarity)) {
            return "ng";
        } else {
            return neutralCode.contains(polarity) ? "nt" : "ot";
        }
    }

}
