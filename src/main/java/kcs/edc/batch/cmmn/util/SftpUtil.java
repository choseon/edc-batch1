package kcs.edc.batch.cmmn.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;

@Slf4j
public class SftpUtil {

    public static ChannelSftp connectSFTP(String host, int port, String user, String password) {
        JSch jSch = new JSch();
        Session session = null;
        Channel channel = null;
//        try {
        try {
            session = jSch.getSession(user, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        session.setPassword(password);

            Properties properties = new Properties();
            properties.put("StrictHostKeyChecking", "no");
            session.setConfig(properties);

        try {
            session.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }

        try {
            channel = session.openChannel("sftp");
        } catch (JSchException e) {
            e.printStackTrace();
        }
        try {
            channel.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }

        log.info("Sftp Connection Successed");

//        } catch (JSchException e) {
//            log.info(e.getMessage());
//        }
        return (ChannelSftp) channel;
    }

    /**
     *
     *
     * @param channelSftp
     * @param remotePath
     * @param fileName
     * @param downloadPath
     * @return
     */
    public static File download(ChannelSftp channelSftp, String remotePath, String fileName, String downloadPath) {

        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        File file = null;

        try {

//            GregorianCalendar calendar = new GregorianCalendar();
//            calendar.add(Calendar.DATE, -4);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
//            String strDate = sdf.format(calendar.getTime());

//            String strDate = null;
//            try {
//                strDate = DateUtils.getOffsetDate(baseDt, -4, "yyyyMMdd");
//            } catch (Exception e) {
//                log.info(e.getMessage());
//            }
//
//            String fullFileName = fileName + "_" + strDate + ".csv";
//            log.info("fullFilePath : {}", remotePath + fullFileName);

            InputStream inputStream = null;
            try {
                channelSftp.cd(remotePath.replaceAll("\\\\", "/"));
                inputStream = channelSftp.get(fileName);
            } catch (SftpException e) {
                log.info(e.getMessage());
            }

            bis = new BufferedInputStream(inputStream);

            File dir = new File(downloadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            file = new File(downloadPath + fileName.toLowerCase());
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                log.info(e.getMessage());
            }
            bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[1024];
            int readCount = 0;

            try {
                while ((readCount = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readCount);
                }
            } catch (IOException e) {
                log.info(e.getMessage());
            }

        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
            try {
                fos.close();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
//        log.info("Complete File Download : {}", file.getPath());
        return file;
    }
}
