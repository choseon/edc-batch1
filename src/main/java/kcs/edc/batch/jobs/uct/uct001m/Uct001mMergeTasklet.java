package kcs.edc.batch.jobs.uct.uct001m;

import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.util.KOTFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
public class Uct001mMergeTasklet extends CmmnJob implements Tasklet {

    @Value("#{jobExecutionContext[baseYearList]}")
    private List<String> baseYearList;

    @Autowired
    protected FileService fileService;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        String scriptPath = this.fileService.getResourcePath() + CmmnConst.RESOURCE_FILE_NAME_UCT_SCRIPT;

        BufferedWriter bw = KOTFileUtil.getBufferedWriter(scriptPath, "UTF-8");
        bw.write("#!/bin/bash");
        bw.newLine();

        for (String year : this.baseYearList) {
            for (int i = 1; i < 10; i++) {

                this.fileService.getTempFileVO().setAppendingFileName(year + "_" + i);
                String filePattern = this.fileService.getTempFileVO().getFilePath() + this.fileService.getTempFileVO().getFileName();
                String filePath = this.fileService.getTempFileVO().getFileFullName();

                // 리눅스 파일병합
                // ls ht_uct001m_202111_2020_1* | xargs cat > ht_uct001m_202111_2020_1.txt
                String commandline = String.format("ls %s* | xargs cat > %s", filePattern, filePath);
                log.info("commandline:: {}", commandline);

                bw.write(commandline);
                bw.newLine();
            }
        }

        bw.close();

        // 스크립트 실행
        runShellScript(scriptPath);

        return RepeatStatus.FINISHED;
    }

    /**
     * 스크립트 실행
     */
    private void runShellScript(String scriptPath) {
        try {
            Runtime.getRuntime().exec(scriptPath);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }
}
