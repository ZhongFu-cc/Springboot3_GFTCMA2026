package tw.com.gftcma.pojo.BO;

import lombok.AllArgsConstructor;
import lombok.Data;
import tw.com.gftcma.pojo.excelPojo.MemberImportExcel;

/**
 * 讀取匯入Excel時的中繼對象<br>
 * 保留Excel實際的列號，讓匯入結果可以明確告訴使用者是哪一列出問題
 */
@Data
@AllArgsConstructor
public class MemberImportRow {

	/** Excel中的列號(1為標題列) */
	private Integer rowNumber;

	private MemberImportExcel data;

}
