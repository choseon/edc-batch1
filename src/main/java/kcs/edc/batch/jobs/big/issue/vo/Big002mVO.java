package kcs.edc.batch.jobs.big.issue.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Big002mVO {

    private int result;
    private String reason;
    private ReturnObjectVO return_object;


    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReturnObjectVO {
        private String date;
        private List<TopicItem> topics;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopicItem {

        private String artcPblsDt;

        @JsonProperty
        private String topic;

        @JsonProperty
        private int topic_rank;

        @JsonProperty
        private String topic_keyword;

        @JsonProperty
        private List<String> news_cluster;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;

    }
}
