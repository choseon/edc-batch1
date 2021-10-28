package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
public class Kot001mTasklet extends CmmnJob implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("search4", this.baseDt);

        // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않는 설정으로 build한다.
        URI uri = builder.build(true).toUri();

        Kot001mVO resultVO = this.apiService.sendApiForEntity(uri, Kot001mVO.class);
        if (Objects.isNull(resultVO)) return RepeatStatus.FINISHED;
        if (!resultVO.getResultCode().equals("00")) return RepeatStatus.FINISHED; // NODATA_ERROR

        // 결과리스트에서 데이터 가공
        for (Kot001mVO.Item item : resultVO.getItems()) {
            String htmlPath = item.getBbstxSn() + ".html";
            item.setNewsBdt(htmlPath);
            item.setCletFileCtrnDttm(this.baseDt);

            this.resultList.add(item);
        }

        // kot001m 파일 생성
        this.fileService.makeFile(this.resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return ExitStatus.COMPLETED;
    }
}
