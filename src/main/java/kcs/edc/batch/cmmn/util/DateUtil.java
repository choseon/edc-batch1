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
	 *  지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(디폴트형식 yyyyMMdd)
	 *
	 * @param strDate 지정된 일자(String)
	 * @param offset 이동할 일수( -2147483648 ~ 2147483647 )
	 * @return 변경된 날짜
	 */
	public static String getOffsetDate(String strDate, int offset) throws Exception {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
		Date date = fmt.parse(strDate);
		return getOffsetDate(date, offset, "yyyyMMdd");
	}

	/**
	 *  지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(형식 지정)
	 *
	 * @param strDate 지정된 일자(String)
	 * @param offset 이동할 일수( -2147483648 ~ 2147483647 )
	 * @return 변경된 날짜
	 */
	public static String getOffsetDate(String strDate, int offset, String pFormat) {
		SimpleDateFormat fmt = new SimpleDateFormat(pFormat);
		Date date = null;
		try {
			date = fmt.parse(strDate);
		} catch (ParseException e) {
			log.error(e.getMessage());
		}
		return getOffsetDate(date, offset, pFormat);
	}

	/**
	 *  지정된 날짜에서 원하는 일수 만큼 이동된 날짜를 반환한다.(형식 지정)
	 *
	 * @param pDate Date 객체
	 * @param offset 이동할 일수( -2147483648 ~ 2147483647 )
	 * @param pFormat 날짜형식
	 * @return 변경된 날짜
	 */
	public static String getOffsetDate(Date pDate, int offset, String pFormat) {
		SimpleDateFormat fmt = new SimpleDateFormat(pFormat);
		Calendar c = Calendar.getInstance();
		String ret = "";

		try {
			c.setTime(pDate);
			c.add(Calendar.DAY_OF_MONTH, offset);
			ret = fmt.format(c.getTime());
		}
		catch(Exception e) {
			log.error(e.getMessage());
		}
		return ret;
	}

	/**
	 *  오늘 날짜를 구한다.
	 * @return
	 */
	public static String getToday(String format) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(cal.getTime());
	}

	public static String getCurrentTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	public static String getCurrentTime2() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
	}

	public static String getFormatDate(String strDate) throws Exception {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
		Date date = fmt.parse(strDate);
		return getOffsetDate(date, 0, "yyyy-MM-dd");
	}

}


