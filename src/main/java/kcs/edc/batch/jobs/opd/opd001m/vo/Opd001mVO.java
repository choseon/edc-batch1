package kcs.edc.batch.jobs.opd.opd001m.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 기업개황정보 vo
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opd001mVO {

    /**
     * ENTS_KORE_NM	업체한글명
     **/
    @JsonProperty
    private String corp_name; // 정식회사명칭

    /**
     * ENGL_CO_NM	업체영문명
     **/
    @JsonProperty
    private String corp_name_eng; // 영문 정식회사명칭

    /**
     * STCK_ITM_NM 주식종목명
     */
    @JsonProperty
    private String stock_name; // 종목명

    /**
     * STREX_CD 거래소코드
     */
    @JsonProperty
    private String stock_code; // 상장회사의 품목코드 (6자리)

    /**
     * RPPN_NM 대표자명
     */
    @JsonProperty
    private String ceo_nm; // 대표자명

    /**
     * PBLS_CO_JRPN_TPCD 발행회사법인구분코드
     */
    @JsonProperty
    private String corp_cls; // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)

    /**
     * JRNO 법인등록번호
     */
    @JsonProperty
    private String jurir_no; // 법인등록번호

    /**
     * BRNO 사업자등록번호
     */
    @JsonProperty
    private String bizr_no; // 사업자등록번호

    /**
     * ENTS_ADDR 업체주소
     */
    @JsonProperty
    private String adres; // 주소

    /**
     * HMPG_URL 홈페이지url
     */
    @JsonProperty
    private String hm_url; // 홈페이지

    /**
     * ENTS_TELNO 업체 전화번호
     */
    @JsonProperty
    private String phn_no; // 전화번호

    /**
     * ENTS_FAX_NO 업체팩스번호
     */
    @JsonProperty
    private String fax_no; // 팩스번호

    /**
     * ESTB_DT	설립일자
     **/
    @JsonProperty
    private String est_dt; // 설립일(YYYYMMDD)

    /**
     * SAMM 결산월
     */
    @JsonProperty
    private String acc_mt; // 결산월

    @JsonProperty
    private String corp_code; // 정식회사명칭

    /**
     * CLET_FILE_CRTN_DT 수집파일생성일자
     */
    private String cletFileCrtnDt;

}
