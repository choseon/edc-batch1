package kcs.edc.batch.jobs.big.news.code;

import java.util.HashMap;
import java.util.Map;

public class NewNationWideComCode {

	private Map<String, String> codeMap;

	public NewNationWideComCode() {
		codeMap = new HashMap<>();

		codeMap.put("1100101", "전국");
		codeMap.put("1100201", "전국");
		codeMap.put("1100301", "전국");
		codeMap.put("1100401", "전국");
		codeMap.put("1100501", "전국");
		codeMap.put("1100611", "전국");
		codeMap.put("1100701", "전국");
		codeMap.put("1100751", "전국");
		codeMap.put("1100801", "전국");
		codeMap.put("1100901", "전국");
		codeMap.put("1101001", "전국");
		codeMap.put("1101101", "전국");
		codeMap.put("1300101", "전국");
		codeMap.put("2100101", "전국");
		codeMap.put("2100201", "전국");
		codeMap.put("2100311", "전국");
		codeMap.put("2100501", "전국");
		codeMap.put("2100601", "전국");
		codeMap.put("2100701", "전국");
		codeMap.put("2100801", "전국");
		codeMap.put("2100851", "전국");
		codeMap.put("7100501", "전국");
		codeMap.put("7101201", "전국");

		codeMap.put("1200101", "수도권");
		codeMap.put("1200201", "수도권");

		codeMap.put("1400201", "충청");
		codeMap.put("1400351", "충청");
		codeMap.put("1400401", "충청");
		codeMap.put("1400501", "충청");
		codeMap.put("1400551", "충청");
		codeMap.put("1400601", "충청");
		codeMap.put("1400701", "충청");

		codeMap.put("1500051", "경상");
		codeMap.put("1500151", "경상");
		codeMap.put("1500301", "경상");
		codeMap.put("1500401", "경상");
		codeMap.put("1500501", "경상");
		codeMap.put("1500601", "경상");
		codeMap.put("1500701", "경상");
		codeMap.put("1500801", "경상");
		codeMap.put("1500901", "경상");

		codeMap.put("1600201", "전라");
		codeMap.put("1600301", "전라");
		codeMap.put("1600501", "전라");
		codeMap.put("1600801", "전라");
		codeMap.put("1601001", "전라");
		codeMap.put("1601101", "전라");
		codeMap.put("1700101", "전라");
		codeMap.put("1700201", "전라");


		codeMap.put("8100201", "방송사");
		codeMap.put("8100301", "방송사");
		codeMap.put("8100401", "방송사");
		codeMap.put("8200101", "방송사");
	}

	public String getNewsNationName(String code) {
		return this.codeMap.get(code);
	}
}
