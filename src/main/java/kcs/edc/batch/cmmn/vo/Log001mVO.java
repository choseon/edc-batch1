package kcs.edc.batch.cmmn.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Log001mVO {

    /** 년월일값 */
    private String paramYmd;

    /** 단계 */
    private String step;

    /** 테이블명 */
    private String tableName;

    /** 시작시간 */
    private String startTime;

    /** 종료시간 */
    private String endTime;

    /** 상태 */
    private String jobStat;

    /** 오류메시지 */
    private String errm;

    /** 적재성공 Row수 */
    private int targSuccessRows;

    /** 적재용량 */
    private int bytes;

}
