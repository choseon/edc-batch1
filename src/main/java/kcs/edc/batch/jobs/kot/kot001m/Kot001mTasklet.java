package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.KOTFileUtil;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
public class Kot001mTasklet extends CmmnJob implements Tasklet {

    private String encoding = "UTF-8";

    private String attachedFilePath;

    private String scriptPath;

    @Value("${kot.scriptFileName}")
    private String scriptFileName;

    @Value("${kot.period}")
    private int period; // 수집기간

    private static List<String> imgURLsOrg = new ArrayList<String>();
    private static List<String> imgChangePaths = new ArrayList<String>();

    private List<Kot002mVO> newsKeywordList = new ArrayList<>();

    @SneakyThrows
    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.scriptPath = this.fileService.getResourcePath();
        this.attachedFilePath = this.fileService.getAttachedFilePath();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return super.afterStep(stepExecution);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        // 기준일부터 period 기간동안 api 호출
        for (int i = 0; i < period; i++) {

            String searchDate = DateUtil.getOffsetDate(this.baseDt, (i * -1));
            log.info("searchDate: {}", searchDate);

            UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
            builder.replaceQueryParam("search4", searchDate);

            // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않는 설정으로 build한다.
            URI uri = builder.build(true).toUri();

            Kot001mVO resultVO = this.apiService.sendApiForEntity(uri, Kot001mVO.class);
            if (!resultVO.getResultCode().equals("00")) {
                log.info("uri: {}", uri);
                log.info("resultVO: {}", resultVO.getResultMsg());
                this.fileService.makeLogFile(resultVO.getResultMsg());
                continue;
            }

            // 결과리스트 데이터 가공
            for (Kot001mVO.Item item : resultVO.getItems()) {

                item.setResultCode(resultVO.getResultCode());
                item.setResultMsg(resultVO.getResultMsg());
                item.setNumOfRows(resultVO.getNumOfRows());
                item.setPageNo(resultVO.getPageNo());
                item.setTotalCount(resultVO.getTotalCount());

                // html 파일
                String htmlFileName = "bbstxSn_" + item.getBbstxSn() + ".html";
                String htmlPath = this.attachedFilePath + item.getBbstxSn() + "/" + htmlFileName;
                makeHtmlFile(htmlPath, item.getNewsBdt());

                item.setNewsBdt(htmlPath);
                item.setLoadCmplDttm(DateUtil.getCurrentTime());
                item.setCletFileCtrnDt(DateUtil.getCurrentDate());
                this.resultList.add(item);

                if (Objects.nonNull(item.getKwrd())) {
                    List<Kot002mVO> keywordList = getKeywordList(item);
                    this.newsKeywordList.addAll(keywordList);
                }
            }
        }


        // kot001m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_KOT001M, this.resultList);

        // kot002m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_KOT002M, this.newsKeywordList);

        // html 파일생성
        replaceImgURLtaskFolder(this.attachedFilePath);

        // imageDownload script 파일생성
        makeImgDownLoadScript(this.scriptPath + this.scriptFileName);

        // imageDownload script 실행
        runImageDownloadScript(this.scriptPath + this.scriptFileName);

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    /**
     * @param item
     * @return
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
            vo.setCletFileCtrnDttm(item.getCletFileCtrnDt());
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
    private void makeHtmlFile(String htmlPath, String input) {
        log.info("htmlPath: {}", htmlPath);
        BufferedWriter bw = null;
        try {
            bw = KOTFileUtil.getBufferedWriter(htmlPath, this.encoding);
            bw.write(input);
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    private void replaceImgURLtaskFolder(String filePath) {

        // 초기화
        this.imgURLsOrg.clear();
        this.imgChangePaths.clear();

        ArrayList<String> fileNames = KOTFileUtil.getFileNamesOfSearchTree(filePath, true);

        int cnt = 0;
        for (String fileName : fileNames) {
            String tempFileName = fileName + "#%#";
            replaceImgURLtaskFile(fileName, tempFileName, this.encoding);

            cnt++;
            if (cnt % 100 == 0) {
                log.info("replaceImgURLtaskFolder : " + cnt);
            }
        }
    }

    private void replaceImgURLtaskFile(String fileName, String tempFileName, String encoding) {
        BufferedReader br = KOTFileUtil.getBufferedReader(fileName, encoding);
        BufferedWriter bw = KOTFileUtil.getBufferedWriter(tempFileName, encoding);

        String parentFile = KOTFileUtil.getParentFolder(fileName);

        try {
            String line = null;
            while (true) {

                if (!((line = br.readLine()) != null)) break;

                if (ObjectUtils.isEmpty(line)) {
                    bw.newLine();
                    continue;
                }

                String[] temp = line.split(" ");
                for (String s : temp) {
                    s = s.trim();
                }

                for (int i = 0; i < temp.length; i++) {

                    if (temp[i].startsWith("src=\"https:")) {

                        int start = temp[i].indexOf('"');
                        int end = temp[i].lastIndexOf('"');

                        String imgURLOrg = temp[i].substring(start + 1, end);
                        this.imgURLsOrg.add(imgURLOrg);

                        int idx = imgURLOrg.lastIndexOf('/');
                        bw.write(temp[i].replace(imgURLOrg.substring(0, idx), parentFile) + " ");
                        this.imgChangePaths.add(parentFile);

                        log.info("download imagePath: {}", imgURLOrg);

                    } else {
                        bw.write(temp[i] + " ");
                    }
                }
            }

            Boolean rmFile = KOTFileUtil.rmFile(fileName);
            if(!rmFile) {
                log.info("파일 삭제 오류");
            } else {
                Boolean renameFile = KOTFileUtil.renameFile(tempFileName, fileName);
                if(!renameFile) log.info("파일명 변경 오류");
            }

        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    /**
     * 이미지파일 다운로드 script 파일 생성
     */
    private void makeImgDownLoadScript(String scriptPath) {
        if (this.imgChangePaths.size() == this.imgURLsOrg.size()) {
            BufferedWriter bw = KOTFileUtil.getBufferedWriter(scriptPath, encoding);
            try {
                bw.write("#!/bin/bash");
                bw.newLine();

                int len = this.imgChangePaths.size();
                for (int i = 0; i < len; i++) {
                    bw.write("mkdir -p " + imgChangePaths.get(i));
                    bw.newLine();
                }
                bw.newLine();

                for (int i = 0; i < len; i++) {
                    bw.write("wget -P " + this.imgChangePaths.get(i) + " ");
                    bw.write("\"" + this.imgURLsOrg.get(i) + "\"");
                    bw.newLine();
                }
                log.info("makeImgDownLoadScript: {}", scriptPath);

            } catch (IOException e) {
                log.info(e.getMessage());
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        log.info(e.getMessage());
                    }
                }
            }
        } else {
            log.info("KotraWriter.imgDownLoadScriptWrite imgpath size diff : ");
        }
    }

    /**
     * 이미지파일 다운로드 script 파일 실행
     */
    private void runImageDownloadScript(String scriptPath) {
        if (ObjectUtils.isEmpty(scriptPath)) return;
        try {
            Runtime.getRuntime().exec(scriptPath);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

}
