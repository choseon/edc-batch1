package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
@StepScope
public class Kot001mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        URI uri = null;
        try {
            // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않음
            uri = new URI(getUriComponetsBuilder().build(true).toUriString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Kot001mVO resultVO = sendApiForEntity(uri, Kot001mVO.class);
        if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

        // 결과리스트에서 데이터 가공
        for (Kot001mVO.Item item : resultVO.getItems()) {
            String htmlPath = item.getBbstxSn() + ".html";
            item.setNewsBdt(htmlPath);

            resultList.add(item);
        }

        // kot001m 파일 생성
        makeFile(getJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return ExitStatus.COMPLETED;
    }
}
