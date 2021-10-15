package kcs.edc.batch.jobs.som.som001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KCSFrequencyVO {

    private String[] header;
    private int totalDocumentCount;
    private Map<String, Object> keywordDocumentCount = new HashMap<>();
    private List<Map<String, Object>> rows = new ArrayList<>();

}
