package kcs.edc.batch.jobs.saf.saf001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.jobs.saf.saf001m.vo.Saf001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Slf4j
@StepScope
public class Saf001mTasklet extends CmmnJob implements Tasklet {

    private List<String> certNumList = new ArrayList<>();
    private String authKey;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);
        this.authKey = this.apiService.getJobPropHeader(getJobGrpName(), "AuthKey");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", authKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        // parameter setting
        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("conditionValue", baseDt);
        URI uri = builder.build().toUri();

        // send API
        Saf001mVO resultVO = this.apiService.sendApiExchange(uri, HttpMethod.GET, entity, Saf001mVO.class);
        if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

        // certNum을 추출한 List를 saf001l에 넘겨준다
        for (Saf001mVO.Item item : resultVO.getResultData()) {

            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime());

            log.info("certUid: {}, certNum : {}", item.getCertUid(), item.getCertNum());

            resultList.add(item);
            certNumList.add(item.getCertNum());
        }
        // 파일생성
        this.fileService.makeFile(resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("certNumList", certNumList);
        return ExitStatus.COMPLETED;
    }
}
