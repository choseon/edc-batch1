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

        @JsonProperty("topic")
        private String issueNm;

        @JsonProperty("topic_rank")
        private int issueRnk;

        @JsonProperty("topic_keyword")
        private String issuekywdNm;

//        @JsonProperty("news_cluster")
        private String artcListCn;

        private String KcsRgrsYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;

        @JsonProperty("news_cluster")
        private List<String> newsCluster;

//        public String newsClusterToString() {
//            StringBuilder sb = new StringBuilder();
//            for(String str : this.artcListCn) {
//                sb.append("\"").append(str).append("\"").append(",");
//            }
//
//            String clusterStr = sb.toString();
//            return clusterStr.substring(0, clusterStr.length() - 1) + "]";
//        }
    }
}
