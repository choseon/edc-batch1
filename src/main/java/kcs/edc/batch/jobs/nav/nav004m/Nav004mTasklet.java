package kcs.edc.batch.jobs.nav.nav004m;

import com.jcraft.jsch.ChannelSftp;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.service.SftpService;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.List;

@Slf4j
public class Nav004mTasklet extends CmmnJob implements Tasklet {

    @Autowired
    SftpService sftpService;

    private String fileNamePattern = "HT_%s_%s.csv";


    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        // set JobId
        this.sftpService.setJobId(getCurrentJobId());

        // SFTP Connection
        ChannelSftp channelSftp = this.sftpService.connectSFTP();
        if (ObjectUtils.isEmpty(channelSftp)) return RepeatStatus.FINISHED;

        // File Download
        String fileName = String.format(this.fileNamePattern, this.getCurrentJobId().toUpperCase(), this.baseDt);
        log.info("fileName: {}", fileName);

        File downloadFile = this.sftpService.download(channelSftp, fileName);
        if (ObjectUtils.isEmpty(downloadFile)) return RepeatStatus.FINISHED;

        // CSV -> List Conversion
        List<Object[]> csvToList = FileUtil.readCsvFile(downloadFile.getPath());
        if (ObjectUtils.isEmpty(csvToList)) return RepeatStatus.FINISHED;

        // header 삭제
        csvToList.remove(0);
        // 수집파일생성일자 현재날짜로 셋팅
        for (Object[] objects : csvToList) {
            objects[objects.length - 1] = DateUtil.getCurrentDate();
        }

        // Make TSV File
        this.fileService.makeFile(csvToList);

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }
}
