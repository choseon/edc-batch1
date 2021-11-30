package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
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

    private String attachedFilePath;

    private String scriptPath;

    @Value("${kot.scriptFileName}")
    private String scriptFileName; // script 파일명

    private List<String> changingImageList = new ArrayList<>();

    @Value("${kot.period}")
    private int period; // 수집기간

    @Value("${kot.imageDownAllowUrl}")
    private List<String> imageDownAllowUrl; // 이미지다운로드시 방화벽 허용된 url 목록

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
//                log.info("uri: {}", uri);
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
                String fileDirName = this.fileService.getAttachFileVO().getFileDirName();
                String htmlFileName = "bbstxSn_" + item.getBbstxSn() + ".html";
                String htmlPath = fileDirName + "/html/" + htmlFileName;
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


        // kot001m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_KOT001M, this.resultList);

        // kot002m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_KOT002M, this.newsKeywordList);

        // 이미지파일 다운로드 실행
        execImageDownload();



//        // html 이미지 경로 변경
//        replaceImgURLtaskFolder(this.attachedFilePath);
//
//        // imageDownload script 파일생성
//        makeImgDownLoadScript(this.scriptPath + this.scriptFileName);
//
//        // imageDownload script 실행
//        runImageDownloadScript(this.scriptPath + this.scriptFileName);

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
    private void makeHtmlFile(String htmlPath, String input) {
        log.info("htmlPath: {}", htmlPath);

        String source = replaceImageUrlPath(input);
        FileUtil.makeHtmlFile(htmlPath, source);
    }

    /**
     * 소스에서 src 추출
     *
     * @param source html 소스
     * @return
     */
    private List<String> findImageUrlPath(String source) {

        // src를 추출하는 정규식
        Pattern regex = Pattern.compile("<*src=[\"']?([^>\"']+)[\"']?[^>]*>");
        Matcher matcher = regex.matcher(source);

        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group(1);
            result.add(group);
            this.changingImageList.add(group);
        }
        return result;
    }

    /**
     * 소스에서 url 변환
     *
     * @param source html소스
     * @return
     */
    private String replaceImageUrlPath(String source) {
        String result = null;

        List<String> imageUrlPath = findImageUrlPath(source);
        for (String url : imageUrlPath) {
            String changeSourceImageUrlPath = getChangeImageUrlPath(url, "anl_data/BA201/kotra/");
            log.info("url: {}", url);
            log.info("changeUrl: {}", changeSourceImageUrlPath);

            result = source.replace(url, changeSourceImageUrlPath);
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

    private Boolean isAllowUrl(String url) {
        for (String allowUrl : this.imageDownAllowUrl) {
            if(allowUrl.contains(url)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isImageFile(String url) {
        File file = new File(url);
        try {
            BufferedImage readImage = ImageIO.read(file);
            return readImage != null;
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        return false;
    }

    private void execImageDownload() {
        if (ObjectUtils.isEmpty(this.scriptPath)) return;

        BufferedWriter bw = KOTFileUtil.getBufferedWriter(this.scriptPath, this.encoding);
        try {
            bw.write("#!/bin/bash");
            bw.newLine();

            for (String path : this.changingImageList) {

                if(!isAllowUrl(path)) continue;
                if(!isImageFile(path)) continue;

                String.format("wget -P %s %s", this.attachedFilePath, path);
                bw.newLine();
            }

            Runtime.getRuntime().exec(this.scriptPath);

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



    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * script 생성
     *
     * @param scriptPath
     * @param changePath
     * @param originPath
     */
    private void makeScript(String scriptPath, String changePath, List<String> originPath) {
        BufferedWriter bw = KOTFileUtil.getBufferedWriter(scriptPath, encoding);
        try {
            bw.write("#!/bin/bash");
            bw.newLine();

            for (String path : originPath) {
                String.format("wget -P %s %s", changePath, path);
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

            Boolean rmFile = KOTFileUtil.rmFile(fileName);
            if (!rmFile) log.info("파일 삭제 오류  {}", fileName);

            Boolean renameFile = KOTFileUtil.renameFile(tempFileName, fileName);
            if (!renameFile) log.info("파일명 변경 오류");

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

                        log.info("filePath: {}, download imagePath: {}", parentFile, imgURLOrg);

                    } else {
                        bw.write(temp[i] + " ");
                    }
                }
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
     *
     * @param scriptPath 스크립트 파일경로
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
