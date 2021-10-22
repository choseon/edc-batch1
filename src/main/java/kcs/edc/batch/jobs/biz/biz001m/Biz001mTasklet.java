package kcs.edc.batch.jobs.biz.biz001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.jobs.biz.biz001m.vo.Biz001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.net.URI;
import java.util.List;

@Slf4j
@StepScope
public class Biz001mTasklet extends CmmnJob implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

        Biz001mVO resultVO = this.apiService.sendApiForEntity(uri, Biz001mVO.class);

        // 예외 처리

        List<Biz001mVO.Item> jsonArray = resultVO.getJsonArray();
        for (Biz001mVO.Item item : jsonArray) {

            resultList.add(item);
        }

        // 파일 생성
        this.fileService.makeFile(this.resultList);
        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
