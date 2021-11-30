package kcs.edc.batch.cmmn.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.io.*;
import java.util.ArrayList;

@Slf4j
public class KOTFileUtil {

    private static ArrayList<String> fileNames = new ArrayList<String>();

    /**
     * ######################################################################################################################################################
     **/

    public static BufferedReader getBufferedReader(String fileName, String encoding) {
        BufferedReader br = null;

        try {
            FileInputStream fis = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(fis, encoding);
            br = new BufferedReader(isr);
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }

        return br;
    }

    public static BufferedWriter getBufferedWriter(String fileName, String encoding) {
        return getBufferedWriter(fileName, encoding, false);
    }

    public static BufferedWriter getBufferedWriter(String fileName, String encoding, boolean isAppend) {
        return getBufferedWriter(fileName, encoding, isAppend, true);
    }

    public static BufferedWriter getBufferedWriter(String fileName, String encoding, boolean isAppend, boolean makeParentFlag) {
        if (makeParentFlag) {
            makeParent(fileName);
        }

        BufferedWriter bw = null;

        try {
            FileOutputStream fos = new FileOutputStream(fileName, isAppend);
            OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
            bw = new BufferedWriter(osw);
            return bw;
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if(bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }

        return bw;
    }

    /**
     * ######################################################################################################################################################
     **/

    public static void makeParent(String fileName) {
        File file = new File(fileName);
        File parentFile = file.getParentFile();

        ArrayList<String> mkdirList = new ArrayList<String>();

        // 부모 폴더가 없는 경우를 탐색
        while (true) {
            // 부모 폴더가 존재하면 종료
            if (parentFile.exists()) {
                break;
            }
            // 부모 폴더가 존재하지 않으면, 새로 생성하기 위한 목록에 저장
            else {
                mkdirList.add(parentFile.getAbsolutePath());
                parentFile = parentFile.getParentFile();
            }
        }

        // 부모 폴더 생성
        for (int i = mkdirList.size() - 1; i >= 0; i--) {
            parentFile = new File(mkdirList.get(i));
            if (!parentFile.mkdir()) {
                log.info("폴더 생성 실패: {}", parentFile.getPath());
            }
        }

    }


    /**
     * ######################################################################################################################################################
     **/

    public static ArrayList<String> getFileNamesOfSearchTree(String path, boolean innerFlag) {
        ArrayList<String> result = new ArrayList<String>();

        File file = new File(path);

        // 파일 검사
        if (file.isFile()) {
            if (path.charAt(path.length() - 1) == '/' || path.charAt(path.length() - 1) == '\\') {
                result.add(path.substring(0, path.length() - 1));
            } else {
                result.add(path);
            }
        }
        // 폴더 검사
        else if (file.isDirectory()) {
            if (path.charAt(path.length() - 1) != '/' && path.charAt(path.length() - 1) != '\\') {
                path += "/";
            }

            clearFileNames();
            searchTree(path, innerFlag);

            result.addAll(fileNames);
            clearFileNames();
        }

        return result;
    }

    private static void clearFileNames() {
        fileNames.clear();
    }

    private static void searchTree(String path, boolean innerFlag) {
        File file = new File(path);
        File[] list = file.listFiles();
        if (ObjectUtils.isEmpty(list)) return;

        for (int i = 0; i < list.length; i++) {
            String fileName = list[i].getName();

            // 파일인지 폴더인지 검사
            File fileTemp = new File(path + fileName);

            if (fileTemp.isDirectory()) {
                if (innerFlag) {
                    searchTree(path + fileName + "/", innerFlag);
                }
            } else if (fileTemp.isFile()) {
                fileNames.add(path + fileName);
            }
        }

    }

    public static String getRealName(String fileName) {
        return getRealName(fileName, false);
    }

    /**
     * 파일이면, 실제 파일 이름만 반환
     * [rmExtFlag]가 [true]이면, 확장자를 제거하고 반환
     *
     * @param fileName
     * @param rmExtFlag
     * @return
     */
    public static String getRealName(String fileName, boolean rmExtFlag) {
        String realName = null;

        File file = new File(fileName);
        if (!file.exists()) return null;

        if (file.isFile() || file.isDirectory()) {
            realName = file.getName();

            if (rmExtFlag && realName.contains(".")) {
                realName = realName.substring(0, realName.lastIndexOf('.'));
            }
        }
        return realName;
    }

    /**
     * ######################################################################################################################################################
     **/


    public static String getParentFolder(String fileName) {
        File file = new File(fileName);
        return file.getParent();
    }

    public static Boolean rmFile(String fileName) {
        File file = new File(fileName);
        return file.delete();
    }

    public static Boolean renameFile(String fileNameOrg, String fileNameRep) {
        File fileOrg = new File(fileNameOrg);
        File fileRep = new File(fileNameRep);
        return fileOrg.renameTo(fileRep);
    }
}
