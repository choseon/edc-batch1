package kcs.edc.batch.cmmn.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class FileUtil {


    /**
     * tsv 파일 생성
     *
     * @param filePath 파일경로
     * @param fileName 파일명
     * @param list     리스트
     * @param <T>
     */
    public static <T> void makeTsvFile(String filePath, String fileName, List<T> list) {
        makeTsvFile(filePath, fileName, list, false);
    }

    /**
     * tsv 파일생성
     *
     * @param filePath 파일경로
     * @param fileName 파일명
     * @param list     리스트
     * @param append   이어쓰기여부
     * @param <T>
     */
    public static <T> void makeTsvFile(String filePath, String fileName, List<T> list, Boolean append) {
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        TsvWriter tsvWriter = null;
        try {

            Writer writer = new OutputStreamWriter(new FileOutputStream(filePath + fileName, append), StandardCharsets.UTF_8);
            tsvWriter = new TsvWriter(writer, new TsvWriterSettings());

            if (list.size() == 0) return;
            if (list.get(0) instanceof Object[]) {
                tsvWriter.writeRows((Collection<Object[]>) list);
            } else {
                for (T t : list) {
                    Field[] fields = t.getClass().getDeclaredFields();

                    Object[] fieldArr = new Object[fields.length];
                    for (int i = 0; i < fields.length; i++) {

                        // som004mVO의 categoryList 항목은 제외
                        if (fileName.contains("som004m") && fields[i].getName().equals("categoryList")) continue;

                        fields[i].setAccessible(true);
                        fieldArr[i] = fields[i].get(t);
                    }
                    tsvWriter.writeRow(fieldArr);
                }
            }

        } catch (FileNotFoundException | IllegalAccessException e) {
            log.info(e.getMessage());
        } finally {
            if (tsvWriter != null) {
                tsvWriter.close();
            }
        }

    }

    /**
     * 파일 병합
     *
     * @param sourceFilePath
     * @param targetPath
     * @param targetFileName
     */
    public static void mergeFile(String sourceFilePath, String targetPath, String targetFileName) {

        File dir = new File(sourceFilePath);
        if (dir == null) return;
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {

            File targetFile = new File(targetPath + targetFileName);
            // 파일이 존재하면 삭제
            if (targetFile.exists()) {
                targetFile.delete();
            }
            // 파일생성
            targetFile.createNewFile();
            log.info("createNewFile >> {}", targetFile);

            // 파일 이어쓰기 가능하게 설정
            bos = new BufferedOutputStream(new FileOutputStream(targetFile, true));

            for (File file : files) {
                bis = new BufferedInputStream(new FileInputStream(file.getPath()));
                log.info("mergeFile >> {}", file.getPath());

                byte[] buffer = new byte[1024];
                int readCount = 0;

                while ((readCount = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readCount);
                    bos.flush();
                }
                bis.close();
            }

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    /**
     * 파일 삭제
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        File dir = new File(filePath);
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            boolean delete = file.delete();
        }
        boolean delete = dir.delete();
        log.info("deleteFolder : {}, isDelete : {}", dir, delete);
    }

    /**
     * json 파일을 읽어 리스트로 변환하여 반환
     *
     * @param filePath
     * @param objName
     * @return
     * @throws FileNotFoundException
     */
    public static JsonArray readJsonFile(String filePath, String objName) throws FileNotFoundException {
        FileReader reader = new FileReader(filePath);
        JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
        JsonArray jsonArray = obj.getAsJsonArray(objName);

        return jsonArray;
    }

    /**
     * txt파일을 읽어 리스트로 변환하여 반환
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static List<String> readTextFile(String filePath) throws IOException {

        Path path = Paths.get(filePath);
        log.info("readTextFile filePath {}", path);
        List<String> list = Files.readAllLines(path);
        return list;
    }

    /**
     * csv 파일을 리스트로 변환
     *
     * @param filePath
     * @return
     */
    public static List<Object[]> readCsvFile(String filePath) {
        List<Object[]> rows = new ArrayList<>();
        File csv = new File(filePath);
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csv));
            while ((line = br.readLine()) != null) { // readLine()은 파일에서 개행된 한 줄의 데이터를 읽어온다.
                List<String> aLine = new ArrayList<String>();
                String[] lineArr = line.split(","); // 파일의 한 줄을 ,로 나누어 배열에 저장 후 리스트로 변환한다.
                rows.add(lineArr);
            }
        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close(); // 사용 후 BufferedReader를 닫아준다.
                }
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
        return rows;
    }

}
