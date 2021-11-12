package kcs.edc.batch.cmmn.service;

import com.jcraft.jsch.*;
import kcs.edc.batch.cmmn.property.SftpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

@Slf4j
@Service
public class SftpService {

    @Autowired
    private SftpProperties sftpProperties;

    private SftpProperties.SftpProp sftpProp;

    public void init(String jobId) {
        this.sftpProp = this.sftpProperties.getCurrentJobProp(jobId);
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

    /**
     *
     * @param channelSftp
     * @param remoteFileName
     * @return
     */
    public File download(ChannelSftp channelSftp, String remoteFileName, String downloadPath) {

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = null;

        try {
            String remoteFilePath = this.sftpProp.getRemoteFilePath().replaceAll("\\\\", "/");
            log.info("remoteFile: {}", remoteFilePath + remoteFileName);

            channelSftp.cd(remoteFilePath);
            InputStream inputStream = channelSftp.get(remoteFileName);

            bis = new BufferedInputStream(inputStream);

            File dir = new File(downloadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String pathName = downloadPath + remoteFileName.toLowerCase();
            file = new File(pathName);
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[1024];
            int readCount = 0;

            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }

        } catch (SftpException | IOException e) {
            log.info(e.getMessage());

        } finally {
            try {
                if (bis != null) bis.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
            try {
                if (bos != null) bos.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
        return file;
    }

}
