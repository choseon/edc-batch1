package kcs.edc.batch.jobs.kot.kot001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.util.KOTFileUtil;
import kcs.edc.batch.jobs.kot.kot001m.vo.Kot001mVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 대한무역투자진흥공사 해외시장 뉴스 수집 Tasklet
 */
@Slf4j
public class Kot001mTasklet extends CmmnJob implements Tasklet {

    private String scriptPath = "C:\\dev\\edc-batch\\resources\\";
    private String scriptFileName = "img_download.sh";
    private String kotNasStorePath = "C:/dev/data_nas/anl_data/BA201/kotra/";
    private String encoding = "UTF-8";

    private static List<String> imgURLsOrg = new ArrayList<String>();
    private static List<String> imgChangePaths = new ArrayList<String>();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        this.writeCmmnLogStart();

        UriComponentsBuilder builder = this.apiService.getUriComponetsBuilder();
        builder.replaceQueryParam("search4", this.baseDt);

        // serviceKey에 encoding이 되어 있기 때문에 encoding을 하지 않는 설정으로 build한다.
        URI uri = builder.build(true).toUri();

        Kot001mVO resultVO = this.apiService.sendApiForEntity(uri, Kot001mVO.class);
        if (Objects.isNull(resultVO)) return RepeatStatus.FINISHED;
        if (!resultVO.getResultCode().equals("00")) return RepeatStatus.FINISHED; // NODATA_ERROR

        // 결과리스트에서 데이터 가공
        for (Kot001mVO.Item item : resultVO.getItems()) {
            String htmlPath = item.getBbstxSn() + ".html";

            if (Objects.isNull(item.getNewsBdt())) {

            } else {
                htmlPath = this.kotNasStorePath + item.getBbstxSn() + "/" + htmlPath;
                makeHtmlFile(htmlPath, item.getNewsBdt());
            }

            item.setNewsBdt(htmlPath);
            item.setCletFileCtrnDt(this.baseDt);
            this.resultList.add(item);
        }

        // kot001m 파일 생성
        this.fileService.makeFile(this.resultList);

        replaceImgURLtaskFolder();

        // script생성
        makeImgDownLoadScript();

        // script실행
        runImageDownloadScript();

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put("resultList", resultList);
        return super.afterStep(stepExecution);
    }

    public void makeKeywordFile() {

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
            BufferedWriter bw = KOTFileUtil.getBufferedWriter(htmlPath, "UTF-8");
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

        ArrayList<String> fileNames = KOTFileUtil.getFileNamesOfSearchTree(this.kotNasStorePath, true);

        int cnt = 0;
        for (String fileName : fileNames) {
            String tempFileName = fileName + "#%#";
            replaceImgURLtaskFile(fileName, tempFileName, encoding);

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
            e.printStackTrace();
        }
    }

    /**
     * #############################################################################################################
     **/

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("KotraWriter.imgDownLoadScriptWrite imgpath size diff : ");
        }
    }

    private void runImageDownloadScript() {
        String command = this.scriptPath + this.scriptFileName;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }


}
