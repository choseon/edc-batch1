package kcs.edc.batch.jobs.kot.kot002m;

import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot001mVO;
import kcs.edc.batch.jobs.kot.kot002m.vo.Kot002mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@StepScope
public class Kot002mTasklet extends CmmnTask implements Tasklet {

    @Value("#{jobExecutionContext[resultList]}")
    List<Kot001mVO.Item> resultList;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        writeCmmnLogStart();

        List<Kot002mVO> items = new ArrayList<>();
        List<String> keywordList = new ArrayList<>();

        for (Kot001mVO.Item item : resultList) {
            String[] split = item.getKwrd().split(",");
            keywordList.addAll(Arrays.asList(split));
            for (String s : keywordList) {
                Kot002mVO vo = new Kot002mVO();
                vo.setBbstxSn(item.getBbstxSn());
                vo.setKwrd(s);
                vo.setNewsWrtDt(item.getNewsWrtDt());
                vo.setCletDT(item.getCletDT());
                items.add(vo);
            }
        }

        makeFile(getJobId(), items);

        writeCmmnLogEnd();
        return RepeatStatus.FINISHED;
    }
}
