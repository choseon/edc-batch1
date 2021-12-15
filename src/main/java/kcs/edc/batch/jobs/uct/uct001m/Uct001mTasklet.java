package kcs.edc.batch.jobs.uct.uct001m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.uct.uct001m.vo.Uct001mVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.*;

/**
 * UN Comtrade HS6 단위코드 기준의 각국 수출데이터 수집
 */
@Slf4j
public class Uct001mTasklet extends CmmnJob implements Tasklet {

    @Value("#{stepExecutionContext[threadNum]}")
    private String threadNum;

    @Value("#{stepExecutionContext[partitionList]}")
    private List<String> partitionList;

    @Value("#{jobParameters[ps]}")
    private String ps;

    private int totalFileCnt;


    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);

        this.baseDt = DateUtil.getCurrentDate("yyyyMM");
        if(ObjectUtils.isEmpty(this.ps)) {
            String baseLine = this.schedulerService.getBaseLine();
            this.endDt = DateUtil.getBaseLineDate(baseLine).substring(0,4);
            this.startDt = DateUtil.getOffsetYear(this.endDt, (this.period - 1) * -1);
        } else {
            this.period = 1;
            this.startDt = this.ps;
            this.endDt = this.ps;
        }

        // baseDt를 초기화하므로 fileService도 초기화 해줘야한다.
        this.fileService.init(this.jobId, this.baseDt);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        if (ObjectUtils.isEmpty(this.threadNum)) {
            this.writeCmmnLogStart();
        } else {
            this.writeCmmnLogStart(this.threadNum, this.partitionList.size());
        }

        try {

            // apiService에  Custom RestTemplate Setting
            this.apiService.setRestTemplate(getRestTemplate());

            // 신고국가 목록
            List<String> rList = ObjectUtils.isEmpty(this.partitionList) ? getAreaList() : this.partitionList;
            // 파트너국가 목록
            List<String> pList = getAreaList();
            // 년도 목록
            List<String> psList = new ArrayList<>();

            // 생성되어야할 총파일갯수
            this.totalFileCnt = rList.size() * (pList.size() - 1);

            int resultCnt = 0;
            Boolean success = true;

            for (int i = 0; i < this.period; i++) {

                String ps = DateUtil.getOffsetYear(this.startDt, i);
                log.info(">>> START CAll API >>> ps: {}", ps);

                psList.add(ps);
                this.fileService.initFileVO();
                this.fileService.getTempFileVO().setAppendingFilePath(ps); // temp파일 path 추가

                if (this.fileService.isTempPathExsists() && this.fileService.getTempFileCnt() == this.totalFileCnt) {
                    log.info("completed: {}, fileCnt: {}", this.fileService.getTempFileVO().getFilePath(), this.fileService.getTempFileCnt());
                } else {
                    resultCnt = callApi(rList, pList, ps);
                    if (resultCnt == -1) {
                        success = false;
                        break;
                    }
                }
                log.info(">>> END CALL API >>> ps: {}", ps);
            }

            if (!success) return null;
            for (String ps : psList) {

                this.fileService.initFileVO();
                this.fileService.getTempFileVO().setAppendingFilePath(ps); // temp파일 path 추가

                // 최종검증
                // 이미 생성된 파일은 바로 넘어가기 때문에 정상적으로 파일이 생성된 상태라면 짧은시간 소요됨.
                log.info(">>> START VERIFY >>> ps: {}", ps);
                callApi(rList, pList, ps);
                log.info(">>> END VERIFY >>> ps: {}", ps);

                // 리눅스 명령으로 1차 파일병합
                runMergeScriptFile(ps);

                // 리눅스에서 병합된 파일을 2차 파일병합
//                this.fileService.mergeTempFile(ps + "_" + DateUtil.getCurrentDate("yyyyMM"));
                this.fileService.mergeTempFile(ps);
            }

        } catch (FileNotFoundException e) {
            this.makeErrorLog(e.getMessage());
        } catch (JsonProcessingException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } catch (ParseException e) {
            this.makeErrorLog(e.getMessage());
        } catch (InterruptedException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            if (ObjectUtils.isEmpty(this.threadNum)) {
                this.writeCmmnLogEnd();
            } else {
                this.writeCmmnLogEnd(this.threadNum, this.partitionList.size());
            }
        }

        return RepeatStatus.FINISHED;
    }


    /**
     * API 호출
     *
     * @param reportList  신고국가
     * @param partnerList 파트너국가
     * @param ps          년도
     * @return
     */
    private int callApi(List<String> reportList, List<String> partnerList, String ps) throws FileNotFoundException, IllegalAccessException, JsonProcessingException {

        int index = 0;
        int resultCnt = 0;
        for (String r : reportList) { // 신고국가
            for (String p : partnerList) { // 파트너국가

                if (r.equals(p)) continue;

                if (++index % 1000 == 0) {
                    log.info("currentCnt: {}, totalCnt: {}]", index, this.totalFileCnt);
                }

                String suffixFileName = ps + "_" + r + "_" + p;
                boolean tempFileExsists = this.fileService.isTempFileExsists(suffixFileName);
                if (tempFileExsists) {
                    log.info("[{}/{}] tempFileExsists: {}", index, this.totalFileCnt, suffixFileName);
                    continue;
                }

                int exceptionCnt = 0;
                while (true) { // exception이 많이 발생하기 때문에 exception이 발생한 경우 결과 나올때까지 무한루프 돌린다.

                    try {

                        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                        builder.replaceQueryParam("r", r);
                        builder.replaceQueryParam("p", p);
                        builder.replaceQueryParam("ps", ps);
                        URI uri = builder.build().encode().toUri();

                        Uct001mVO resultVO = this.apiService.sendApiForEntity(uri, Uct001mVO.class);

                        if (Objects.isNull(resultVO)) continue;
                        if (resultVO.getValidation() == null) continue;
                        if (!"Ok".equals(resultVO.getValidation().getStatus().get("name"))) continue;
                        if ("0".equals(resultVO.getValidation().getCount().get("value"))) continue;

                        List<Uct001mVO.Item> dataset = resultVO.getDataset();
                        for (Uct001mVO.Item item : dataset) {

                            // 결과값 체크
                            if (item.getYr().equals("0") || item.getRtCode().equals("0") ||
                                    item.getPtTitle().equals("0") || item.getPtCode().equals("0")) {
                                throw new RestClientException("result is '0'");
                            }

                            item.setCletFileCrtnDt(this.baseDt);
                            this.resultList.add(item);
                        }

                        // temp파일 생성 후 리스트 초기화
                        this.fileService.makeTempFile(this.resultList, suffixFileName, true);
                        this.resultList.clear();

                        break; // 무한루프 종료

                    } catch (RestClientException e) {

                        if (e.getMessage().contains("UnknownHostException")) {
                            this.makeErrorLog(e.getMessage());
                            return -1;
                        }
                        if (e.getMessage().contains("500")) {
                            log.debug("ps {}, r {}, p {},  >> {}", ps, r, p, e.getMessage());
                        } else {
                            log.info("ps {}, r {}, p {},  >> {}", ps, r, p, e.getMessage());
                            if (exceptionCnt++ >= 20) {
                                resultCnt++;
                                break;
                            }
                        }

                    }
                }
            }
        }
        return resultCnt;
    }

    /**
     * RestTemplate Custom settings
     *
     * @return
     */
    private RestTemplate getRestTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectTimeout(20000);
        httpRequestFactory.setReadTimeout(600000);
        HttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(20)
                .build();
        httpRequestFactory.setHttpClient(httpClient);
        RestTemplate template = new RestTemplate(httpRequestFactory);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        template.getMessageConverters().add(converter);

        return template;
    }

    /**
     * 파트너국가 목록 조회
     * 리소스파일을 조회하여 ISO 3166-1 국가 리스트 반환
     *
     * @return
     */
    public List<String> getAreaList() throws FileNotFoundException {

        List<String> pList = new ArrayList<>();
        JsonArray jsonArray = null;

        String resourcePath = this.fileService.getResourcePath();
        String filePath = resourcePath + CmmnConst.RESOURCE_FILE_NAME_UCT_AREA;
        jsonArray = FileUtil.readJsonFile(filePath, "results");

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String id = jsonObject.get("id").getAsString();
            if (id.equals("all")) continue;
            pList.add(id);
        }

        return pList;
    }

    /**
     * filePattern 형식을 조회하여 filePath로 병합하고
     * filePattern에 해당하는 파일들은 삭제하는 스크립트 생성하여 실행
     *
     * @param ps
     * @throws IOException
     */
    public void runMergeScriptFile(String ps) throws IOException, InterruptedException {

        // 스크립트 소스 생성
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 1; i < 10; i++) {

            String prefixFileName = ps + "_" + i;
            this.fileService.getTempFileVO().setAppendingFileName(prefixFileName);

            // /hdata/ht_uct001m/temp/2021/ht_uct001m_2020_1
            String filePattern = this.fileService.getTempFileVO().getFilePath() + this.fileService.getTempFileVO().getFileName();

            // /hdata/ht_uct001m/2021/ht_uct001m_2020_1.txt
            String filePath = this.fileService.getTempFileVO().getFullFilePath();

            // 리눅스 파일병합 명령어
            // ls /hdata/ht_uct001m/temp/2021/ht_uct001m_2020_1* | xargs cat > /hdata/ht_uct001m/2021/ht_uct001m_2020_1.txt
            String commandline = String.format("ls %s*_* | xargs cat > %s", filePattern, filePath);
            stringBuffer.append(commandline);
            stringBuffer.append("\n");

            // 리눅스 파일삭제 명령어
            // rm -rf /hdata/ht_uct001m/temp/2021/ht_uct001m_2020_1*
            commandline = String.format("rm -rf %s*_*", filePattern);
            stringBuffer.append(commandline);
            stringBuffer.append("\n");
        }

        // 스크립트 파일 생성
        String scriptPath = this.fileService.getResourcePath() + CmmnConst.RESOURCE_FILE_NAME_UCT_SCRIPT;
        FileUtil.makeFile(scriptPath, stringBuffer.toString());
        log.info("makeScriptFile: {}", scriptPath);
        log.info("script: {}", stringBuffer.toString());

        // 스크립트 파일 실행
        Process process = Runtime.getRuntime().exec(scriptPath);
        int waitFor = process.waitFor();
        if (waitFor == 0) {
            log.info("Success! runScriptFile: {}", scriptPath);
        }
    }

}
