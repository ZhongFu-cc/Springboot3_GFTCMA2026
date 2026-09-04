package tw.com.gftcma.pojo.excelPojo;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

/**
 * 會員匯入用的Excel對象<br>
 * 客戶提供的名單標題列並不固定(第一欄甚至沒有標題)，所以這邊統一採用「欄位索引」對應，
 * 只要欄位順序符合匯入模板即可。
 */
@Data
public class MemberImportExcel {

	/** A欄 - 報名者類型, Ex:報名者 / 主持主講人報到 */
	@ExcelProperty(index = 0)
	private String registrationType;

	/** B欄 - 電子郵件地址 */
	@ExcelProperty(index = 1)
	private String email;

	/** C欄 - 中文姓名 */
	@ExcelProperty(index = 2)
	private String chineseName;

	/** D欄 - 服務單位 / Organization */
	@ExcelProperty(index = 3)
	private String affiliation;

	/** E欄 - 職稱 / Title */
	@ExcelProperty(index = 4)
	private String jobTitle;

	/** F欄 - 手機 / Mobile Number */
	@ExcelProperty(index = 5)
	private String phone;

}
