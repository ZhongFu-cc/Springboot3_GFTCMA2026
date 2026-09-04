package tw.com.gftcma.pojo.VO;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(name = "MemberImportResultVO", description = "會員名單匯入結果")
public class MemberImportResultVO {

	@Schema(description = "Excel中實際有資料的列數(不含標題列與空白列)")
	private Integer totalCount = 0;

	@Schema(description = "成功匯入的筆數(同時新增了 Member 與 Attendees)")
	private Integer importedCount = 0;

	@Schema(description = "被跳過的筆數")
	private Integer skippedCount = 0;

	@Schema(description = "被跳過的明細")
	private List<RowDetail> skippedRows = new ArrayList<>();

	@Schema(description = "Email重複的筆數,這些資料「有」被匯入,只是提醒該信箱會收到多封信")
	private Integer duplicateEmailCount = 0;

	@Schema(description = "Email重複的明細,這些資料「有」被匯入")
	private List<RowDetail> duplicateEmailRows = new ArrayList<>();

	public void addImported() {
		this.importedCount++;
	}

	public void addSkipped(Integer rowNumber, String email, String chineseName, String reason) {
		this.skippedRows.add(new RowDetail(rowNumber, email, chineseName, reason));
		this.skippedCount++;
	}

	public void addDuplicateEmail(Integer rowNumber, String email, String chineseName, String reason) {
		this.duplicateEmailRows.add(new RowDetail(rowNumber, email, chineseName, reason));
		this.duplicateEmailCount++;
	}

	@Data
	@AllArgsConstructor
	@Schema(name = "RowDetail", description = "需要讓後台知道的匯入明細")
	public static class RowDetail {

		@Schema(description = "Excel中的列號(1為標題列)")
		private Integer rowNumber;

		@Schema(description = "該列的Email")
		private String email;

		@Schema(description = "該列的中文姓名")
		private String chineseName;

		@Schema(description = "原因")
		private String reason;

	}

}
