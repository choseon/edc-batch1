package kcs.edc.batch.jobs.kot.kot001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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

//        @JsonProperty("TWBS_SRNO")
        private String bbstxSn; // 게시글 일련번호

        private String newsTitl; // 뉴스제목

        private String newsBdt; // 뉴스본문

        private String newsWrtDt; // 뉴스 작성일시

        private String WrterNm; // 뉴스작성자명

        private String infoCl; // 정보문류

        private String inqreCnt; // 조회수

        private String cntntSumar; // 내용ㅇ요약

        private String jobSeNm;// 일자리구분명

        private String kwrd; // 키워드

        private String regn; // 지역

        private String natn; // 국가

        private String OvrofInfo; // 무역관정보

        private String indstCl; // 산업분류

        private String hsCdNm; // HS코드명

        private String cmdItNmKorn; // 품목한글명

        private String cmdItNmEng; // 품목영문명

        private String crrspOvrofLink; // 해당무역관링크

        private String inquryItmLink; // 문의사항링크

        private String regDt; // 등록일시

        private String updDt; // 수정일시

        private String cletDT; // 수집일자
    }

}
