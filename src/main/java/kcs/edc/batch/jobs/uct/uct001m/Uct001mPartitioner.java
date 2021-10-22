package kcs.edc.batch.jobs.uct.uct001m;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnPartitioner;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.cmmn.service.FileService;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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
        List<String> areaList = getAreaList();
        if(Objects.isNull(areaList)) {
            return null;
        }

        // 국가리스트의 String을 Object에 담기위해 Collections.singletonList 사용
        this.list = Collections.singletonList(areaList);

        // 상속받은 상위 클래스의 method 실행
        return super.partition(gridSize);
    }

    /**
     * 리소스파일에서 ISO 3166-1 국가 리스트 조회
     *
     * @return
     */
    public List<String> getAreaList() {

        String resourcePath = "C:/dev/edc-batch/resources/";
//        String resourcePath = fileProperty.getResourcePath();
//        String resourcePath = this.fileService.getResourcePath();
//        String filePath = resourcePath + "/" + JobConstant.RESOURCE_FILE_NAME_UCT_AREA;

        String filePath = resourcePath + "/" + JobConstant.RESOURCE_FILE_NAME_UCT_AREA;

        List<String> pList = new ArrayList<>();

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