package kcs.edc.batch.jobs.som.som001m;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.som.som001m.vo.KCSFrequencyVO;
import kcs.edc.batch.jobs.som.som001m.vo.Som001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SomeTrend 키워드 빈도 수집 Tasklet
 */
@Slf4j
public class Som001mTasklet extends CmmnJob implements Tasklet {

    private final String SOM_SOURCES[] = {"news", "twitter", "blog"};

    private final String KCS_KEYWORD_Y = "Y";
    private final String KCS_KEYWORD_N = "N";

    private int index = 0;

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        this.jobExecutionContext.put("list", this.resultList);
        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        List<Som001mVO> resultVO = new ArrayList<>();
        for (String source : this.SOM_SOURCES) {

            // 썸트랜드 키워드 목록 조회
            resultVO = getSomtrendKeywordList(source);
            if(Objects.nonNull(resultVO)) {
                this.resultList.addAll(resultVO);
            }

            // 관세청 키워드 목록 조회
            resultVO = getKCSKeywordList(source);
            if(Objects.nonNull(resultVO)) {
                this.resultList.addAll(resultVO);
            }
        }

        if(this.resultList.size() == 0) {
            log.info("resultList.size(): {}", resultList.size());
        } else {
            // 파일 생성
            this.fileService.makeFile(this.resultList);
        }

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }


    /**
     * 썸트랜드 키워드 목록 조회하여 결과값 리턴
     *
     * @param source
     * @return
     */
    private List<Som001mVO> getSomtrendKeywordList(String source) {

        List<Som001mVO> resultList = new ArrayList<>();
        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("source", source);
        builder.replaceQueryParam("startDate", this.baseDt);
        builder.replaceQueryParam("endDate", this.baseDt);
        URI uri = builder.build().toUri();

        Som001mVO[] resultVO = new Som001mVO[0];
        try {
            resultVO = this.apiService.sendApiForEntity(uri, Som001mVO[].class);
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
        }
        if (Objects.isNull(resultVO)) return null;

        for (Som001mVO item : resultVO) {
            item.setDate(this.baseDt);
            item.setSource(source);
            item.setRegistYn(this.KCS_KEYWORD_N);
            item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
            item.setLastChngDtlDttm(DateUtil.getCurrentTime());
            resultList.add(item);

            log.info("[{} {}] >> source :: {} | keyword :: {} | kcsKeywordYn :: {}",
                    getCurrentJobId(), index++, source, item.getKeyword(), this.KCS_KEYWORD_N);
        }
        return resultList;
    }

    /**
     * 관세청키워드 목록 조회하여 결과값 리턴
     * kcs_keyword_for_somtrend 파일을 조회하여 관세청키워드 목록을 추출 후 API 호출하여 결과 수집
     *
     * @param source
     * @return
     */
    private List<Som001mVO> getKCSKeywordList(String source) {

        List<String> keywordList = new ArrayList<>();
        try {
            // kcs_keyword_for_somtrend.txt list
//            String resourcePath = fileProperty.getResourcePath();
            String resourcePath = this.fileService.getResourcePath();
            String filePath = resourcePath + CmmnProperties.RESOURCE_FILE_NAME_SOM_KCS_KEWORD;
            keywordList = FileUtil.readTextFile(filePath);
            if (keywordList.size() == 0) {
                log.info("filePath {} is null ", filePath);
                return null;
            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        List<Som001mVO> resultList = new ArrayList<>();
        for (String keywordLine : keywordList) {

            UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
            builder.replaceQueryParam("command", "GetKeywordTransitions");
            builder.replaceQueryParam("source", source);
            builder.replaceQueryParam("startDate", this.baseDt);
            builder.replaceQueryParam("endDate", this.baseDt);

            String[] split = keywordLine.split("\\|"); // split
            for (String keyword : split) {
                builder.queryParam("keyword[]", keyword);
            }
            URI uri = builder.build().toUri();

            KCSFrequencyVO resultVO = null;
            try {
                resultVO = this.apiService.sendApiForEntity(uri, KCSFrequencyVO.class);
            } catch (JsonProcessingException e) {
                log.info(e.getMessage());
                this.makeErrorLog(e.toString());
                return null;
            } catch (RestClientException e) {
                log.info(e.getMessage());
                this.makeErrorLog(e.toString());
                return null;
            }
            if (Objects.isNull(resultVO)) continue;

            for (String keyword : split) {
                Object o = resultVO.getKeywordDocumentCount().get(keyword);
                if (Objects.isNull(o)) continue;

                int frequency = (Integer) resultVO.getKeywordDocumentCount().get(keyword);
                if (frequency < 1) continue;

                Som001mVO item = new Som001mVO();
                item.setDate(this.baseDt);
                item.setSource(source);
                item.setKeyword(keyword);
                item.setFrequency(frequency);
                item.setRegistYn(this.KCS_KEYWORD_Y);
                item.setFrstRgsrDtlDttm(DateUtil.getCurrentTime());
                item.setLastChngDtlDttm(DateUtil.getCurrentTime());
                resultList.add(item);

                log.info("[{} {}] >> source :: {} | keyword :: {} | kcsKeywordYn :: {}",
                        getCurrentJobId(), index++, source, keyword, this.KCS_KEYWORD_Y);
            }
        }
        return resultList;
    }
}
