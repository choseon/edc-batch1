package kcs.edc.batch.jobs.uct.uct001m;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kcs.edc.batch.cmmn.jobs.CmmnPartitioner;
import kcs.edc.batch.cmmn.jobs.CmmnTask;
import kcs.edc.batch.cmmn.property.JobConstant;
import kcs.edc.batch.cmmn.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;

import java.io.FileNotFoundException;
import java.util.*;

@Slf4j
public class Uct001mPartitioner extends CmmnPartitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        if(Objects.isNull(list)) list = new ArrayList<>();

        try {

            // kcs_keyword_for_somtrend.txt list
//            String resourcePath = "/data/edc-batch/dev/resources/";
            String resourcePath = "C:/dev/edc-batch/resources/";
            String filePath = resourcePath + JobConstant.RESOURCE_FILE_NAME_UCT_AREA;

            // ISO 3166-1 국가 리스트
            JsonArray jsonArray = FileUtil.readJsonFile(filePath, "results");
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String id = jsonObject.get("id").getAsString();
                if (id.equals("all")) continue;
                list.add(id);

                if(list.size() == 128) break; // TEST
            }
            log.info("arealist.size() >> " + list.size());

        } catch (FileNotFoundException e) {
            log.info(e.getMessage());
        }

        return super.partition(gridSize);
    }
}