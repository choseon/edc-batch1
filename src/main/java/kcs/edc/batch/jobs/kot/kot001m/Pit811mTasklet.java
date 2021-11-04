package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.KOTFileUtil;
import kcs.edc.batch.jobs.kot.kot001m.vo.Pit811mVO;
import kcs.edc.batch.jobs.kot.kot001m.vo.Pit812mVO;
import kcs.edc.batch.jobs.kot.kot002m.vo.Kot002mVO;
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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
public class Pit811mTasklet extends CmmnJob implements Tasklet {

//    private String scriptPath = "C:\\dev\\edc-batch\\resources\\";
//    private String scriptFileName = "img_download.sh";
//    private String kotNasStorePath = "C:/dev/data_nas/anl_data/BA201/kotra/";
    private String encoding = "UTF-8";

    private String rootPath;
    private String scriptPath;

    @Value("${kot.scriptFileName}")
    private String scriptFileName;

    private static List<String> imgURLsOrg = new ArrayList<String>();
    private static List<String> imgChangePaths = new ArrayList<String>();

    private List<Pit812mVO> newsKeywordList = new ArrayList<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {

        super.beforeStep(stepExecution);

        this.scriptPath = this.fileService.getResourcePath();
        this.rootPath = this.fileService.getRootPath();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("search4", this.baseDt);

        // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않는 설정으로 build한다.
        URI uri = builder.build(true).toUri();

        Pit811mVO resultVO = this.apiService.sendApiForEntity(uri, Pit811mVO.class);
        if (Objects.isNull(resultVO)) return RepeatStatus.FINISHED;

        // {"resultCode":"3","resultMsg":"NODATA_ERROR"}
        if (!resultVO.getResultCode().equals("00")) return RepeatStatus.FINISHED;

        // 결과리스트 데이터 가공
        for (Pit811mVO.Item item : resultVO.getItems()) {
            String htmlPath = item.getBbstxSn() + ".html";

            if (Objects.isNull(item.getNewsBdt())) {

            } else {
                htmlPath = this.rootPath + item.getBbstxSn() + "/" + htmlPath;
                makeHtmlFile(htmlPath, item.getNewsBdt());
            }

            item.setNewsBdt(htmlPath);
            item.setCletFileCtrnDt(this.baseDt);
            this.resultList.add(item);

            if(Objects.nonNull(item.getKwrd())) {
                List<Pit812mVO> keywordList = getKeywordList(item);
                this.newsKeywordList.addAll(keywordList);
            }
        }

        // pit811m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_PIT811M, this.resultList);

        // pit812m 파일생성
        this.fileService.makeFile(CmmnConst.JOB_ID_PIT812M, this.newsKeywordList);

        // html 파일생성
        replaceImgURLtaskFolder();

        // script 파일생성
        makeImgDownLoadScript();

        // imageDownload script 파일 실행
        runImageDownloadScript();

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return super.afterStep(stepExecution);
    }

    /**
     *
     * @param item
     * @return
     */
    public List<Pit812mVO> getKeywordList(Pit811mVO.Item item) {

        List<Pit812mVO> resultList = new ArrayList<>();
        String[] split = item.getKwrd().split(",");
        List<String> keywordList = Arrays.asList(split);

        for (String keyword : keywordList) {
            Pit812mVO vo = new Pit812mVO();
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
        try {
            log.info("htmlPath: {}", htmlPath);
            BufferedWriter bw = KOTFileUtil.getBufferedWriter(htmlPath, this.encoding);
            bw.write(input);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceImgURLtaskFolder() {

        // 초기화
        this.imgURLsOrg.clear();
        this.imgChangePaths.clear();

        ArrayList<String> fileNames = KOTFileUtil.getFileNamesOfSearchTree(this.rootPath, true);

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
        try {
            BufferedReader br = KOTFileUtil.getBufferedReader(fileName, encoding);
            BufferedWriter bw = KOTFileUtil.getBufferedWriter(tempFileName, encoding);

            String parentFile = KOTFileUtil.getParentFolder(fileName);

            String line = null;
            while ((line = br.readLine()) != null) {
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
                        bw.write(temp[i].replace(imgURLOrg.substring(0, idx), parentFile + "/image") + " ");
                        this.imgChangePaths.add(parentFile + "/image");

                        log.info("imagePath: {}", parentFile + "/image");

                    } else {
                        bw.write(temp[i] + " ");
                    }
                }
            }
            br.close();
            bw.close();

            KOTFileUtil.rmFile(fileName);
            KOTFileUtil.renameFile(tempFileName, fileName);

        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }


    /**
     * 이미지파일 다운로드하는 script 파일 생성
     */
    private void makeImgDownLoadScript() {
        if (this.imgChangePaths.size() == this.imgURLsOrg.size()) {
            try {
                BufferedWriter bw = KOTFileUtil.getBufferedWriter(this.scriptPath + this.scriptFileName, encoding);
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
                bw.close();
                log.info("makeImgDownLoadScript: {}", this.scriptPath + this.scriptFileName);
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        } else {
            log.info("KotraWriter.imgDownLoadScriptWrite imgpath size diff : ");
        }
    }

    /**
     * 이미지파일 다운로드하는 script 파일 실행
     */
    private void runImageDownloadScript() {
        String command = this.scriptPath + this.scriptFileName;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }


}
