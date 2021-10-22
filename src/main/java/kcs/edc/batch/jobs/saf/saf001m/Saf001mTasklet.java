package kcs.edc.batch.jobs.saf.saf001m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.jobs.saf.saf001m.vo.Saf001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Slf4j
@StepScope
public class Saf001mTasklet extends CmmnTask implements Tasklet, StepExecutionListener {

    private List<String> certNumList = new ArrayList<>();
    private String authKey;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
        jobProp = apiProperty.getJobProp(getJobGrpName());
        authKey = jobProp.getHeader().get("AuthKey");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", authKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        // parameter setting
        UriComponentsBuilder builder = getUriComponetsBuilder().replaceQueryParam("conditionValue", baseDt);
        uri = builder.build().toUri();

        // send API
        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        String resultJson = exchange.getBody();
        log.info("uri {}", uri);
        log.info("resultJson {}", resultJson);

        if(Objects.isNull(resultJson)) return RepeatStatus.FINISHED;

        Saf001mVO resultVO = objectMapper.readValue(resultJson, Saf001mVO.class);
        if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

        // certNum을 추출한 List를 saf001l에 넘겨준다
        for (Saf001mVO.Item item : resultVO.getResultData()) {

            resultList.add(item);
            certNumList.add(item.getCertNum());
        }
        // 파일생성
        makeFile(getCurrentJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("certNumList", certNumList);
        return null;
    }
}
