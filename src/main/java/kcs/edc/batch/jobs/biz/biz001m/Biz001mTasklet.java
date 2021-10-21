package kcs.edc.batch.jobs.biz.biz001m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.jobs.biz.biz001m.vo.Biz001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

@Slf4j
@StepScope
public class Biz001mTasklet extends CmmnTask implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        uri = getUriComponetsBuilder().build().toUri();

        // send api
//        ResponseEntity<String> forEntity = restTemplate.getForEntity(uri, String.class);
//        Biz001mVO resultVO = objectMapper.readValue(forEntity.getBody(), Biz001mVO.class);

        Biz001mVO resultVO = sendApiForEntity(uri, Biz001mVO.class);

        // 예외 처리

        List<Biz001mVO.Item> jsonArray = resultVO.getJsonArray();
        for (Biz001mVO.Item item : jsonArray) {

            resultList.add(item);
        }

        // 파일 생성
        makeFile(getCurrentJobId(), resultList);
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
