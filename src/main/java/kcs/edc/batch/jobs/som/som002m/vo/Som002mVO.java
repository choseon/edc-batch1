package kcs.edc.batch.jobs.som.som002m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Som002mVO {

    private String keyword;
    private int totalCnt;
    private List<Item> documentList = new ArrayList<>();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        /** CLET_DT */
        private String date;

        /** CLET_CHNL_NM */
        private String source;

        /** REGR_DTTM */
        @JsonProperty
        private String documentDate;

        /** ORTX_TITL_NM */
        @JsonProperty
        private String title;

        /** ORTX_CN */
        @JsonProperty
        private String content;

        /** ORTX_URL */
        @JsonProperty
        private String url;

        /** KCS_REGR_YN */
        private String registYn;

        private String delYn = "N";

        private String frstRegstId = "EX_BDP";

        private String frstRgsrDtlDttm;

        private String lastChprId = "EX_BDP";

        private String lastChngDtlDttm;

//        private String source;
//        private String date;
//        private int sequence;
//        private String projectId;
//        private String status;
//        private String title;
//        private String content;
//        private String summaries;
//        private String tag;
//        private String url;
//        private String docID;
//        private String writerCodeString;
//        private String writeName;
//        private String documentDate;
//        private String crawlDate;
//        private String writerRealName;
//        private String profileImageUrl;
//        private String writeCount;
//        private String likeCount;
//        private String friendCount;
//        private String writerCode;
//        private String exposureMetric;
//        private String category;
//        private Object countMap;
//        private List comments;
//        private List<String> vks;
//        private List<String> vksDlOnly;
//        private Author author;
//        private String subjectKeyword;
//        private String registYn;
//        private String writerName;

    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        private String site;
        private int viewCnt;
        private String imageUrl;
        private int writeCnt;
        private int friendCnt;
        private int likeCnt;
        private boolean valid;
        private String sequence;
        private String name;
        private String realName;

    }
}
