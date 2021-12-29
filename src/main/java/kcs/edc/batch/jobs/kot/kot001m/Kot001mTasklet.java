package kcs.edc.batch.jobs.kot.kot001m;

import com.fasterxml.jackson.core.JsonProcessingException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnProperties;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot001mVO;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot002mVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
public class Kot001mTasklet extends CmmnJob implements Tasklet {

    private String encoding = "UTF-8";

    private String attachedFilePath; // 첨부파일 경로

    private String scriptPath; // script 경로

    private String scriptFileName; // script file name

    @Value("${kot.allowUrls}")
    private List<String> allowUrls; // 이미지다운로드시 방화벽 허용된 url 목록

    @Value("${kot.changImgUrlPath}")
    private String changImgUrlPath; // 변경될 url path

    @Value("${kot.htmlDBPath}")
    private String htmlDBPath; // db에 저장될 html 경로

    private List<String> changeImgUrlPathList = new ArrayList<>(); // 변경할 src url 목록
    private List<Kot002mVO> newsKeywordList = new ArrayList<>(); // 뉴스 키워드 목록

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.scriptPath = this.fileService.getResourcePath();
        this.scriptFileName = CmmnProperties.RESOURCE_FILE_NAME_KOT_SCRIPT;
        this.attachedFilePath = this.fileService.getAttachedFilePath();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        try {

            // 기준일부터 period 기간동안 api 호출
            for (int i = 0; i < this.period; i++) {

                String searchDate = DateUtil.getOffsetDate(this.startDt, i);
                log.info(">>> searchDate: {}", searchDate);

                UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
                builder.replaceQueryParam("search4", searchDate);

                // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않는 설정으로 build한다.
                URI uri = builder.build(true).toUri();

                Kot001mVO resultVO = this.apiService.sendApiForEntity(uri, Kot001mVO.class);

                if (!resultVO.getResultCode().equals("00")) {
                    log.info("resultVO: {}", resultVO.getResultMsg());
                    continue;
                }

                // 결과리스트 데이터 가공
                for (Kot001mVO.Item item : resultVO.getItems()) {

                    item.setResultCode(resultVO.getResultCode());
                    item.setResultMsg(resultVO.getResultMsg());
                    item.setNumOfRows(resultVO.getNumOfRows());
                    item.setPageNo(resultVO.getPageNo());
                    item.setTotalCount(resultVO.getTotalCount());

                    String source = item.getNewsBdt(); // html소스

                    // html DB 셋팅경로 : BA201/kotra/html/bbstxSn_뉴스번호.html
                    String htmlFileName = "bbstxSn_" + item.getBbstxSn() + ".html";
                    String htmlPath = this.htmlDBPath + htmlFileName;
                    item.setNewsBdt(htmlPath); // htmlPath 셋팅

                    // 시간 포맷 변환하여 셋팅 (yyyy-MM-dd HH:mm:ss -> yyyyMMddHHmmss)
                    item.setNewsWrtDt(DateUtil.convertDateFormat(item.getNewsWrtDt()));
                    item.setRegDt(DateUtil.convertDateFormat(item.getRegDt()));
                    item.setUpdDt(DateUtil.convertDateFormat(item.getUpdDt()));

                    item.setCletFileCrtnDt(DateUtil.getCurrentDate());
                    this.resultList.add(item);

                    if (Objects.nonNull(item.getKwrd())) {
                        List<Kot002mVO> keywordList = getKeywordList(item);
                        this.newsKeywordList.addAll(keywordList);
                    }

                    // html 실제파일경로 : /app_nas/anl_data/BA201/kotra/html/bbstxSn_뉴스번호.html
                    String htmlFilePath = this.attachedFilePath + "html/" + htmlFileName;
                    makeHtmlFile(htmlFilePath, source);
                }
            }

        } catch (JsonProcessingException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        } catch (RestClientException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        } catch (ParseException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        }

        // kot001m 파일생성
        try {
            this.fileService.makeFile(CmmnProperties.JOB_ID_KOT001M, this.resultList);
        } catch (FileNotFoundException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT001M, e.getMessage());
        }

        // kot002m 파일생성
        try {
            this.fileService.makeFile(CmmnProperties.JOB_ID_KOT002M, this.newsKeywordList);
        } catch (FileNotFoundException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(CmmnProperties.JOB_ID_KOT002M, e.getMessage());
        }

        // 이미지파일 다운로드 실행
        try {
            execImageDownload();
        } catch (IOException e) {
            log.info(e.getMessage());
        } catch (InterruptedException e) {
            log.info(e.getMessage());
        }

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    /**
     * 키워드 리스트 추출
     *
     * @param item
     * @return ","를 잘라서 리스트로 리턴
     */
    public List<Kot002mVO> getKeywordList(Kot001mVO.Item item) {

        List<Kot002mVO> resultList = new ArrayList<>();
        String[] split = item.getKwrd().split(",");
        List<String> keywordList = Arrays.asList(split);

        for (String keyword : keywordList) {
            Kot002mVO vo = new Kot002mVO();
            vo.setBbstxSn(item.getBbstxSn());
            vo.setKwrd(keyword);
            vo.setNewsWrtDt(item.getNewsWrtDt());
            vo.setCletFileCrtnDt(item.getCletFileCrtnDt());
            resultList.add(vo);
        }
        return resultList;
    }

    /**
     * html 파일 생성
     *
     * @param htmlPath html 파일경로
     * @param input    내용
     */
    private void makeHtmlFile(String htmlPath, String input) throws IOException {

        String source = replaceImageUrlPath(input);
        FileUtil.makeFile(htmlPath, source);

        log.info("htmlFile: {}", htmlPath);
    }

    /**
     * 소스에서 src url 변환
     *
     * @param source html소스
     * @return
     */
    private String replaceImageUrlPath(String source) throws IOException {
        String replaceSource = source;

        List<String> imageUrlPath = findImageUrlPath(source);
        for (String url : imageUrlPath) {
            String changeSourceImageUrlPath = getChangeImageUrlPath(url, this.changImgUrlPath);

            log.info("originalImagePath: {}", url);
            log.info("changeImagePath: {}", changeSourceImageUrlPath);

            replaceSource = replaceSource.replace(url, changeSourceImageUrlPath);
        }

        return replaceSource;
    }

    /**
     * 소스에서 src 추출
     *
     * @param source html 소스
     * @return
     */
    private List<String> findImageUrlPath(String source) throws IOException {

        // src를 추출하는 정규식
        Pattern regex = Pattern.compile("<*src=[\"']?([^>\"']+)[\"']?[^>]*>");
        Matcher matcher = regex.matcher(source);

        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group(1);

            if (!isAllowUrl(group)) continue; // 방화벽 허용되지 않은 URL의 경우 제외
            if (!isImageFile(group)) continue; // 이미지파일이 아닌 경우 제외

            result.add(group);
            this.changeImgUrlPathList.add(group);
        }
        return result;
    }


    /**
     * 변환할 url 리턴
     *
     * @param url
     * @return
     */
    private String getChangeImageUrlPath(String url, String changeUrl) {
        int startIdx = 0;
        int endIdx = url.lastIndexOf("/") + 1;
        String substring = url.substring(startIdx, endIdx);
        String replace = url.replace(substring, changeUrl);

        return replace;

    }

    /**
     * 방화벽 허용 URL인지 체크
     *
     * @param url
     * @return
     */
    private Boolean isAllowUrl(String url) {
        Boolean isAllow = false;
        for (String allowUrl : this.allowUrls) {
            if (url.contains(allowUrl)) {
                isAllow = true;
                break;
            }
        }
        return isAllow;
    }

    /**
     * 이미지파일 여부 체크
     *
     * @param url
     * @return
     */
    private Boolean isImageFile(String url) throws IOException {
        BufferedImage readImage = ImageIO.read(new URL(url));
        return readImage != null;
    }

    /**
     * 스크립트 소스 내용생성
     *
     * @return
     */
    private String makeScriptSource() {
        StringBuffer stringBuffer = new StringBuffer();
        for (String path : this.changeImgUrlPathList) {

            String commandLine = String.format("wget -P %s '%s'", this.attachedFilePath + "image/", path);
            stringBuffer.append(commandLine);
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }

    /**
     * 이미지 다운로드 스크립트 실행
     */
    private void execImageDownload() throws IOException, InterruptedException {

        // 스크립트 소스 생성
        String source = makeScriptSource();
        if(ObjectUtils.isEmpty(source)) return;

        // 스크립트 파일 생성
        String filePath = this.scriptPath + this.scriptFileName;
        FileUtil.makeFile(filePath, source);

        // 스크립트 파일 권한 설정
        FileUtil.setFilePermission(filePath);

        // 스크립트 파일 실행
        log.info("runScriptFile: {}", filePath);
        Process process = Runtime.getRuntime().exec(filePath);
        int waitFor = process.waitFor();
        if (waitFor == 0) {
            log.info("Success! download image files");
        }

    }
}
