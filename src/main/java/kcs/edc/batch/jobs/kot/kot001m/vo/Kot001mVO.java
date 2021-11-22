package kcs.edc.batch.jobs.kot.kot001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kot001mVO {

    private String pageNo;
    private String resultCode;
    private String totalCount;

    private List<Item> items;

    private String numOfRows;
    private String resultMsg;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        private String resultCode;

        private String resultMsg;

        private String numOfRows;

        private String pageNo;

        private String totalCount;

        @JsonProperty
        private String bbstxSn; // 게시글 일련번호

        @JsonProperty
        private String newsTitl; // 뉴스제목

        @JsonProperty
        private String newsBdt; // 뉴스본문

        @JsonProperty
        private String newsWrtDt; // 뉴스 작성일시

        @JsonProperty
        private String WrterNm; // 뉴스작성자명

        @JsonProperty
        private String infoCl; // 정보문류

        @JsonProperty
        private String inqreCnt; // 조회수

        @JsonProperty
        private String cntntSumar; // 내용ㅇ요약

        @JsonProperty
        private String jobSeNm;// 일자리구분명

        @JsonProperty
        private String kwrd; // 키워드

        @JsonProperty
        private String regn; // 지역

        @JsonProperty
        private String natn; // 국가

        @JsonProperty
        private String OvrofInfo; // 무역관정보

        @JsonProperty
        private String indstCl; // 산업분류

        @JsonProperty
        private String hsCdNm; // HS코드명

        @JsonProperty
        private String cmdItNmKorn; // 품목한글명

        @JsonProperty
        private String cmdItNmEng; // 품목영문명

        @JsonProperty
        private String crrspOvrofLink; // 해당무역관링크

        @JsonProperty
        private String inquryItmLink; // 문의사항링크

        @JsonProperty
        private String regDt; // 등록일시

        @JsonProperty
        private String updDt; // 수정일시

        private String loadCmplDttm;

        private String CletFileCtrnDt; // 수집파일생성일자
    }

}
