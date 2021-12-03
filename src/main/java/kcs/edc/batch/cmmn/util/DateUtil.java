package kcs.edc.batch.cmmn.util;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class DateUtil {

    /**
     * 지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(디폴트형식 yyyyMMdd)
     *
     * @param strDate 지정된 일자(String)
     * @param offset  이동할 일수( -2147483648 ~ 2147483647 )
     * @return 변경된 날짜
     */
    public static String getOffsetDate(String strDate, int offset) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        String result = null;
        try {
            Date date = fmt.parse(strDate);
            result = getOffsetDate(date, offset, "yyyyMMdd");
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    /**
     * 지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(형식 지정)
     *
     * @param strDate 지정된 일자(String)
     * @param offset  이동할 일수( -2147483648 ~ 2147483647 )
     * @return 변경된 날짜
     */
    public static String getOffsetDate(String strDate, int offset, String pFormat) {
        SimpleDateFormat fmt = new SimpleDateFormat(pFormat);
        String result = null;
        try {
            String formatDate = getFormatDate(strDate);
            Date date = fmt.parse(formatDate);
            result = getOffsetDate(date, offset, pFormat);
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    /**
     * 지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(형식 지정)
     *
     * @param pDate   Date 객체
     * @param offset  이동할 일수( -2147483648 ~ 2147483647 )
     * @param pFormat 날짜형식
     * @return 변경된 날짜
     */
    public static String getOffsetDate(Date pDate, int offset, String pFormat) {
        SimpleDateFormat fmt = new SimpleDateFormat(pFormat);
        Calendar c = Calendar.getInstance();

        c.setTime(pDate);
        c.add(Calendar.DAY_OF_MONTH, offset);
        String ret = fmt.format(c.getTime());

        return ret;
    }

    public static String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String getCurrentTime2() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public static String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public static String getFormatDate(String strDate) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        String result = null;
        try {
            Date date = fmt.parse(strDate);
            result = getOffsetDate(date, 0, "yyyy-MM-dd");
        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    public static String convertDateFormat(String strDate) {

        String result = null;
        try {
            SimpleDateFormat beforeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date tempDate = beforeFormat.parse(strDate);

            SimpleDateFormat afterFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            result = afterFormat.format(tempDate);

        } catch (ParseException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    public static String getYesterDayDtlDttm() {
        return LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}


