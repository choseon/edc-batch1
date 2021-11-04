package kcs.edc.batch.jobs.kot.kot002m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.jobs.kot.kot001m.vo.Pit811mVO;
import kcs.edc.batch.jobs.kot.kot002m.vo.Kot002mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class Kot002mTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[resultList]}")
    List<Pit811mVO.Item> resultList;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        List<Kot002mVO> items = new ArrayList<>();
        List<String> keywordList = new ArrayList<>();

        for (Pit811mVO.Item item : resultList) {
            String[] split = item.getKwrd().split(",");
            keywordList.addAll(Arrays.asList(split));

            for (String keyword : keywordList) {
                Kot002mVO vo = new Kot002mVO();
                vo.setBbstxSn(item.getBbstxSn());
                vo.setKwrd(keyword);
                vo.setNewsWrtDt(item.getNewsWrtDt());
                vo.setCletFileCtrnDttm(item.getCletFileCtrnDt());
                items.add(vo);
            }
        }

        this.fileService.makeFile(items);

        this.writeCmmnLogEnd();
        return RepeatStatus.FINISHED;
    }
}
