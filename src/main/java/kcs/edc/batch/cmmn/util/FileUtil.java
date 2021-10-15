package kcs.edc.batch.cmmn.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class FileUtil {

    public static final String CURRENT_MAKE_FILE_TYPE = FileUtil.MAKE_FILE_TYPE_TSV;
    public static final String MAKE_FILE_TYPE_TSV = "tsv";
    public static final String MAKE_FILE_TYPE_JSON = "json";

    /**
     * 파일생성
     *
     * @param filePath
     * @param fileName
     * @param list
     * @param <T>
     * @throws FileNotFoundException
     * @throws IllegalAccessException
     */
    public static <T> void makeFile(String filePath, String fileName, List<T> list) throws FileNotFoundException, IllegalAccessException {
        makeTsvFile(filePath, fileName, list);
    }

    /**
     * tsv 파일생성
     *
     * @param filePath
     * @param fileName
     * @param list
     * @param <T>
     * @throws FileNotFoundException
     * @throws IllegalAccessException
     */
    public static <T> void makeTsvFile(String filePath, String fileName, List<T> list) throws FileNotFoundException, IllegalAccessException {

        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Writer writer = new OutputStreamWriter(new FileOutputStream(filePath + fileName), StandardCharsets.UTF_8);
        TsvWriter tsvWriter = new TsvWriter(writer, new TsvWriterSettings());

        if (list.get(0) instanceof Object[]) {
            tsvWriter.writeRows((Collection<Object[]>) list);
        } else {
            for (T t : list) {
                Field[] fields = t.getClass().getDeclaredFields();

                Object[] fieldArr = new Object[fields.length];
                for (int i = 0; i < fields.length; i++) {

                    // som004mVO의 categoryList 항목은 제외
                   if(fileName.contains("som004m") && fields[i].getName().equals("categoryList")) continue;

                    fields[i].setAccessible(true);
                    fieldArr[i] = fields[i].get(t);
                }
                tsvWriter.writeRow(fieldArr);
            }
        }
        tsvWriter.close();
    }

    public static JsonArray readJsonFile(String filePath, String objName) throws FileNotFoundException {
        FileReader reader = new FileReader(filePath);
        JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
        JsonArray jsonArray = obj.getAsJsonArray(objName);

        return jsonArray;
    }

    public static List<String> readTextFile(String filePath) throws IOException {
//        ClassPathResource resource = new ClassPathResource(filePath);
//        Path path = Paths.get(resource.getURI());

        Path path = Paths.get(filePath);
        log.info("readTextFile filePath {}", path);
        List<String> list = Files.readAllLines(path);
        return list;
    }

/*    public static List<String> readTextFile(String resourceName) throws IOException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        Path path = new File(resource.getPath()).toPath();
        List<String> list = Files.readAllLines(path);
        return list;
    }*/



    /**
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static StringBuffer readTsvFile(String filePath) throws IOException {

        File dir = new File(filePath);
        if(dir == null) return null;
        File[] files = dir.listFiles();
        if(files == null || files.length == 0) return null;

        String line = "";
        StringBuffer sb = new StringBuffer();
        FileInputStream fis;
        BufferedReader inFiles = null;
        for (File file : files) {

            fis = new FileInputStream(file.getPath());
            inFiles = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            while((line = inFiles.readLine()) != null) {
                if(line.trim().length() > 0) {
//                    sb += line + "\n";
                    sb.append(line + "\r\n");
                }
            }
        }
        if(inFiles != null) {
            inFiles.close();
        }

        log.info("sb {}", sb);
        return sb;
    }

    public static void mergeFile(String filePath, String targetPath, String fileName) throws IOException {
//        Files.readAllBytes(filePath);

        StringBuffer sb = readTsvFile(filePath);
        deleteFiles(filePath);

        BufferedWriter bw = new BufferedWriter(new FileWriter(targetPath + fileName));
        bw.write(sb.toString());
//        bw.flush();
//        bw.close();

        FileOutputStream fos = new FileOutputStream(targetPath + fileName);
        try (OutputStreamWriter osw = new OutputStreamWriter(fos)) {

            osw.write(sb.toString());
            osw.flush();
            osw.close();
        }

    }

    /**
     * temp 폴더에서 tsv 파일을 읽어 List로 변화하여 리턴
     * @param filePath
     * @return
     */
    public static List<Object[]> getListFromTsvFile(String filePath) {
        File dir = new File(filePath);
        File[] files = dir.listFiles();
        List<Object[]> rows = new ArrayList<>();

        TsvParserSettings settings = new TsvParserSettings();
        settings.setMaxCharsPerColumn(1000000);

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) continue;
                TsvParser parser = new TsvParser(settings);
                rows.addAll(parser.parseAll(file));
                file.delete(); // 파일삭제
            }
            dir.delete(); // 폴더 삭제
        }
        return rows;
    }

    /**
     * TempFile 삭제
     * @param filePath
     */
    public static void deleteFiles(String filePath) {
        File dir = new File(filePath);
        if(dir == null) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            file.delete();
        }
    }
}
