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

    public ChannelSftp connectSFTP() throws JSchException {
        JSch jSch = new JSch();

        Session session = jSch.getSession(this.sftpProp.getUser(), this.sftpProp.getHost(), this.sftpProp.getPort());
        session.setPassword(this.sftpProp.getPassword());

        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        session.setConfig(properties);

        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();

        log.info("Sftp Connection Successed");

        return (ChannelSftp) channel;
    }

    /**
     * @param channelSftp
     * @param remoteFileName
     * @return
     */
    public File download(ChannelSftp channelSftp, String remoteFileName, String downloadPath) throws IOException, SftpException {

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File file = null;

        try {
            String remoteFilePath = this.sftpProp.getRemoteFilePath().replaceAll("\\\\", "/");
            log.info("download remoteFile: {}", remoteFilePath + remoteFileName);

            channelSftp.cd(remoteFilePath);
            InputStream inputStream = channelSftp.get(remoteFileName);

            bis = new BufferedInputStream(inputStream);

            File dir = new File(downloadPath);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
            }

            String pathName = downloadPath + remoteFileName.toLowerCase();
            file = new File(pathName);
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[1024];
            int readCount = 0;

            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }

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
