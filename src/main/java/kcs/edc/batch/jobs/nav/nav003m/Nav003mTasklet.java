package kcs.edc.batch.jobs.nav.nav003m;

import com.jcraft.jsch.ChannelSftp;
import kcs.edc.batch.cmmn.jobs.CmmnJob;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.service.SftpService;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Nav003mTasklet extends CmmnJob implements Tasklet {

    @Autowired
    SftpService sftpService;

    private String fileNamePattern = "HT_%s_%s.csv";

    private List<String> navJobList;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        super.beforeStep(stepExecution);
        this.navJobList = new ArrayList<>();
        this.navJobList.add(CmmnConst.JOB_ID_NAV003M);
        this.navJobList.add(CmmnConst.JOB_ID_NAV004M);
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        this.writeCmmnLogStart(jobId);

        for (String jobId : this.navJobList) {

            Map<String, Object> resultMap = downloadSFTP(jobId);
            if(resultMap.get("result").equals("fail")) {
                this.fileService.makeLogFile(resultMap.get("msg").toString());

            } else {
                List<Object[]> list = (List<Object[]>) resultMap.get("data");
                // Make TSV File
                this.fileService.makeFile(jobId, list);
                // Download TempFile 삭제
                this.fileService.cleanTempFile(jobId);
            }
        }

        this.writeCmmnLogEnd(jobId);

        return RepeatStatus.FINISHED;
    }

    public Map<String, Object> downloadSFTP(String jobId) {

        Map<String, Object> resultMap = new HashMap<>();
        List<Object[]> csvToList = new ArrayList<>();

        // set JobId
        this.sftpService.init(jobId);

        // SFTP Connection
        ChannelSftp channelSftp = this.sftpService.connectSFTP();
        if (ObjectUtils.isEmpty(channelSftp)) {
            resultMap.put("result", "fail");
            resultMap.put("msg", "connect SFTP failed");
            return resultMap;
        }

        String fileName = String.format(this.fileNamePattern, jobId.toUpperCase(), this.baseDt);
        String downloadPath = this.fileService.getTempPath(jobId);
        // File SFTP Download
        File downloadFile = this.sftpService.download(channelSftp, fileName, downloadPath);
        if (ObjectUtils.isEmpty(downloadFile)) {
            resultMap.put("result", "fail");
            resultMap.put("msg", "SFTP file download failed");
            return resultMap;
        }

        // CSV -> List Conversion
        csvToList = FileUtil.readCsvFile(downloadFile.getPath());
        if (ObjectUtils.isEmpty(csvToList)) {
            resultMap.put("result", "fail");
            resultMap.put("msg", "convert CSV to List failed");
            return resultMap;
        }

        // header 삭제
        csvToList.remove(0);
        // 수집파일생성일자 현재날짜로 셋팅
        for (Object[] objects : csvToList) {
            objects[objects.length - 1] = DateUtil.getCurrentDate();
        }
        resultMap.put("result", "success");
        resultMap.put("data", csvToList);

        return resultMap;
    }


}
