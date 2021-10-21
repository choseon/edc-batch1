package kcs.edc.batch.cmmn.service;

import com.jcraft.jsch.*;
import kcs.edc.batch.cmmn.property.SftpProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

@Slf4j
@Service
public class SftpService {

    @Autowired
    private SftpProperty sftpProperty;

    private SftpProperty.JobProp sftpProp;

    public void setJobId(String jobId) {
        this.sftpProp = this.sftpProperty.getCurrentJobProp(jobId);
    }

    public ChannelSftp connectSFTP() {
        JSch jSch = new JSch();
        Channel channel = null;

        try {
            Session session = jSch.getSession(this.sftpProp.getUser(), this.sftpProp.getHost(), this.sftpProp.getPort());
            session.setPassword(this.sftpProp.getPassword());

            Properties properties = new Properties();
            properties.put("StrictHostKeyChecking", "no");
            session.setConfig(properties);

            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();
        } catch (JSchException e) {
            log.info(e.getMessage());
        }

        log.info("Sftp Connection Successed");

        return (ChannelSftp) channel;
    }

    public File download(ChannelSftp channelSftp, String fileName) {

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = null;

        try {
            String remoteFilePath = this.sftpProp.getRemoteFilePath().replaceAll("\\\\", "/");
            log.info("remote FilePath: {}", remoteFilePath + fileName);

            channelSftp.cd(remoteFilePath);
            InputStream inputStream = channelSftp.get(fileName);

            bis = new BufferedInputStream(inputStream);

            File dir = new File(this.sftpProp.getDownloadFilePath());
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String pathName = this.sftpProp.getDownloadFilePath() + fileName.toLowerCase();
            file = new File(pathName);
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[1024];
            int readCount = 0;

            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }

        } catch (SftpException | IOException e) {
            log.info(e.getMessage());

/*        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
            try {
                bos.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }*/
        }
        return file;
    }

}
