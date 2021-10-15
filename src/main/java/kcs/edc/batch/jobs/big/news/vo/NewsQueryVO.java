package kcs.edc.batch.jobs.big.news.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter @Setter
public class NewsQueryVO {

	private String access_key;
	private Argument argument = new Argument();

	@Getter @Setter
	public class Argument {
		private String query;
		private PublishedAtVO published_at = new PublishedAtVO();
		private List<String> newsIds = new ArrayList<>();
		private List<String> provider = new ArrayList<>();
		private List<String> category;
		private List<String> category_incident;
		private String byline;
		private List<String> provider_subject;
		private List<String> subject_info;
		private List<String> subject_info1;
		private List<String> subject_info2;
		private List<String> subject_info3;
		private List<String> subject_info4;
		private Sort sort = new Sort();
		private int hilight = 200;
		private int return_from = 0;
		private int return_size = 1000;
		private List<String> fields = Arrays.asList("title", "content", "byline", "category", "category_incident", "hilight", "images");


		@Getter @Setter
		class Sort {
			private String date = "desc";
		}

		@Getter @Setter
		@AllArgsConstructor
		@NoArgsConstructor
		public class PublishedAtVO {
			private String from;
			private String until;
		}
	}
}
