package kcs.edc.batch.jobs.nav.nav003m;

import com.jcraft.jsch.*;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

public class Nav003mTasklet extends CmmnTask implements Tasklet {

    private String host = "210.114.22.185";
    private String user = "root";
    private String password = "grunet2013!";
    private int port = 16001;

    private String dir = "c:/";
    private String fileName = "ht_nav003m";
    private String path = "/opt/merge/HT_NAV004M";

    private ChannelSftp channelSftp;


    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        writeCmmnLogStart();

        JSch jSch = new JSch();
        Session session = null;
        try {
            session = jSch.getSession(user, host, port);
            session.setPassword(password);

            Properties properties = new Properties();
            properties.put("StrictHostKeyChecking", "no");
            session.setConfig(properties);

            session.connect();

            Channel sftp = session.openChannel("sftp");
            sftp.connect();

            channelSftp = (ChannelSftp) sftp;
            download(dir, fileName, path);

        } catch (JSchException e) {
            e.printStackTrace();
        }
        writeCmmnLogEnd();

        return RepeatStatus.FINISHED;
    }

    /**
     *
     * @param dir
     * @param fileName
     * @param path
     * @return
     */
    private InputStream download(String dir, String fileName, String path) {

        InputStream in = null;
        FileOutputStream out = null;

        try {

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.add(Calendar.DATE, -4);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
            String strDate = sdf.format(calendar.getTime());
            String fullFileName = fileName + "_" + strDate;

            dir = "/opt/merge/HT_NAV004M/";
            fullFileName = "HT_NAV004M_20210711.csv";

            channelSftp.cd(dir.replaceAll("\\\\", "/"));
            in = channelSftp.get(fullFileName);
            out = new FileOutputStream(new File("c:\\\\" + fullFileName));
            int i = 0;
            while((i = in.read()) != -1) {
                out.write(i);
            }
            in.close();
            out.close();

        } catch (SftpException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return in;
    }
}
