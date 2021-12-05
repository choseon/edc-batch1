package kcs.edc.batch.jobs.biz.biz001m;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.jobs.biz.biz001m.vo.Biz001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.client.RestClientException;

import java.io.FileNotFoundException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@StepScope
public class Biz001mTasklet extends CmmnJob implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        try {
            // Uri 설정
            URI uri = this.apiService.getUriComponetsBuilder().build().toUri();

            // Api 호풀
            Biz001mVO resultVO = this.apiService.sendApiForEntity(uri, Biz001mVO.class);

            // json 결과를 list에 저장
            List<Biz001mVO.Item> jsonArray = resultVO.getJsonArray();
            for (Biz001mVO.Item item : jsonArray) {

                // frst_regr_dtl_dttm, last_chng_dtl_dttm을 D-1로 셋팅 (1차 소스 그대로 적용)
                String yesterday = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                item.setFrstRgsrDtlDttm(yesterday);
                item.setLastChngDtlDttm(yesterday);

                this.resultList.add(item);
            }

            // 파일 생성
            this.fileService.makeFile(this.resultList);

        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(e.getMessage());
        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}
