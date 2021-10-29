package kcs.edc.batch.jobs.uct.uct001m;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnPartitioner;
import kcs.edc.batch.cmmn.property.CmmnConst;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * MultiThread를 위한 partitioner
 */
@Slf4j
public class Uct001mPartitioner extends CmmnPartitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // 국가리스트 조회
        this.list = getAreaList();
        log.info("list.size() : {}", this.list.size());

        // 상속받은 상위 클래스의 method 실행
        return super.partition(gridSize);
    }

    /**
     * 리소스파일에서 ISO 3166-1 국가 리스트 조회
     *
     * @return
     */
    public List<Object> getAreaList() {

        String resourcePath = "C:/dev/edc-batch/resources/";
//        String resourcePath = fileProperty.getResourcePath();
//        String resourcePath = this.fileService.getResourcePath();
//        String filePath = resourcePath + "/" + JobConstant.RESOURCE_FILE_NAME_UCT_AREA;

        String filePath = resourcePath + CmmnConst.RESOURCE_FILE_NAME_UCT_AREA;

        List<Object> pList = new ArrayList<>();

        JsonArray jsonArray = null;
        try {
            jsonArray = FileUtil.readJsonFile(filePath, "results");

            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String id = jsonObject.get("id").getAsString();
                if (id.equals("all")) continue;
                pList.add(id);
            }

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
            return null;
        }

        return pList;
    }


}