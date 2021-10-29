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

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        // header setting
        HttpHeaders headers = new HttpHeaders();
        headers.set("AuthKey", this.apiService.getJobPropHeader(getJobGrpName(), "AuthKey"));
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        // parameter setting
        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("conditionValue", this.baseDt);
        URI uri = builder.build().toUri();

        // send API
        Saf001mVO resultVO = this.apiService.sendApiExchange(uri, HttpMethod.GET, entity, Saf001mVO.class);
        if(Objects.isNull(resultVO)) return RepeatStatus.FINISHED;


        for (Saf001mVO.Item item : resultVO.getResultData()) {

            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime());
            this.resultList.add(item);

            log.info("certUid: {}, certNum : {}", item.getCertUid(), item.getCertNum());

            // certNum을 추출한 certNumList를 saf001l에 넘겨준다
            this.certNumList.add(item.getCertNum());
        }

        // 파일생성
        this.fileService.makeFile(this.resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        // certNum을 추출한 certNumList를 saf001l에 넘겨준다
        this.jobExecutionContext.put("certNumList", this.certNumList);

        return ExitStatus.COMPLETED;
    }
}
