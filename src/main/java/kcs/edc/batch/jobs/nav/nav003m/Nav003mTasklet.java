package kcs.edc.batch.jobs.nav.nav003m;

import com.jcraft.jsch.ChannelSftp;
import kcs.edc.batch.cmmn.jobs.CmmnJobs;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.util.DateUtil;
import kcs.edc.batch.cmmn.util.FileUtil;
import kcs.edc.batch.cmmn.util.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.List;

@Slf4j
@StepScope
public class Nav003mTasklet extends CmmnJobs implements Tasklet {

    private String host = "210.114.22.185";
    private String user = "root";
    private String password = "grunet2013!";
    private int port = 16001;

    private String remotePath = "/opt/merge/HT_NAV003M/";
    private String fileName = "HT_NAV003M";
    private String downloadPath = "C:\\dev\\hdata\\ht_nav003m\\temp\\";

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        writeCmmnLogStart();

        ChannelSftp channelSftp = null;
        channelSftp = SftpUtil.connectSFTP(host, port, user, password);

        if (ObjectUtils.isEmpty(channelSftp)) return RepeatStatus.FINISHED;

        // fullFileName : HT_NAV003M.20211018.csv (D-4)
        String strDate = DateUtil.getOffsetDate(cletDt, -4, "yyyyMMdd");
        fileName = fileName + "_" + strDate + ".csv";

        File file = SftpUtil.download(channelSftp, remotePath, fileName, downloadPath);

        List<Object[]> csvToList = FileUtil.getCsvToList(file.getPath());
        if (ObjectUtils.isEmpty(csvToList)) return RepeatStatus.FINISHED;
        csvToList.remove(0); // header 삭제

        this.makeFile(getJobId(), csvToList);

        this.writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }


}
