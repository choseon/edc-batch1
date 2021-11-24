package kcs.edc.batch.cmmn.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipUtil {

    //파일 압축하기
    public static void createZipFile(String dirPath, String zipPath, String zipName) {

        // 압축할 파일 디렉토리 경로
        File[] fileList = null;
        String zipFileName = null;
        File f = new File(dirPath);

        fileList = f.listFiles();

        // 압축 파일을 저장할 디렉토리 경로 및 압축 파일 명
        zipFileName = zipPath + zipName;

        ZipOutputStream zos = null;
        FileInputStream in = null;

        byte[] buf = new byte[1024 * 100];

        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFileName));

            for (int i = 0; i < fileList.length; i++) {
                File chkFile = fileList[i];

                if (chkFile.isFile()) { // 파일만 압축
                    in = new FileInputStream(dirPath + File.separator + chkFile.getName());
                    zos.putNextEntry(new ZipEntry(chkFile.getName()));

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }

                    zos.closeEntry();
                    in.close();
                    // 압축 후 대상 파일 삭제
                    chkFile.delete();
                }
            }
            //압축 대상 파일이 있던 폴더도 삭제
            File folder = new File(dirPath);
            if (folder.exists()) {
                folder.delete();
            }

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }

    }
}
