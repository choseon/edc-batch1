package kcs.edc.batch.jobs.nav.nav004m;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.service.SftpService;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Nav004mTasklet extends CmmnJob implements Tasklet {

    @Autowired
    SftpService sftpService;

    private String fileNamePattern = "HT_%s_%s.csv";

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart();

        try {

            // set JobId
            this.sftpService.init(this.jobId);
            // SFTP Connection
            ChannelSftp channelSftp = this.sftpService.connectSFTP();

            String fileName = String.format(this.fileNamePattern, this.jobId.toUpperCase(), this.baseDt);
            String downloadPath = this.fileService.getTempPath();
            // File SFTP Download
            File downloadFile = this.sftpService.download(channelSftp, fileName, downloadPath);

            // CSV -> List Conversion
            List<Object[]> csvToList = FileUtil.readCsvFile(downloadFile.getPath());
            if(ObjectUtils.isEmpty(csvToList)) return null;

            // header 삭제
            csvToList.remove(0);
            // 수집파일생성일자 현재날짜로 셋팅
            for (Object[] objects : csvToList) {
                objects[objects.length - 1] = DateUtil.getCurrentDate();
            }

            // Make TSV File
            this.fileService.makeFile(csvToList);
            // Download TempFile 삭제
            this.fileService.cleanTempFile();

        } catch (JSchException e) {
            this.makeErrorLog(e.getMessage());
        } catch (SftpException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IOException e) {
            this.makeErrorLog(e.getMessage());
        } catch (IllegalAccessException e) {
            this.makeErrorLog(e.getMessage());
        } finally {
            this.writeCmmnLogEnd();
        }

        return RepeatStatus.FINISHED;
    }
}



