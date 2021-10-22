package kcs.edc.batch.jobs.kot.kot002m.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Kot002mVO {

//    @JsonProperty("TWBS_SRNO")
    private String bbstxSn; // 게시글 일련번호

//    @JsonProperty("MAKE_DTTM")
    private String newsWrtDt; // 뉴스 작성일시

//    @JsonProperty("KYWD_CN")
    private String kwrd; // 키워드

//    @JsonProperty("Clet_DT")
    private String CletFileCtrnDttm; // 수집일자
}
