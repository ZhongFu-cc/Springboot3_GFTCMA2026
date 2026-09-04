package tw.com.gftcma.manager;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.gftcma.config.RegistrationFeeConfig;
import tw.com.gftcma.constants.I18nMessageKey;
import tw.com.gftcma.context.ProjectModeContext;
import tw.com.gftcma.convert.MemberConvert;
import tw.com.gftcma.enums.GroupRegistrationEnum;
import tw.com.gftcma.enums.MemberCategoryEnum;
import tw.com.gftcma.enums.RegistrationPhaseEnum;
import tw.com.gftcma.exception.ImportExcelException;
import tw.com.gftcma.exception.RegistrationClosedException;
import tw.com.gftcma.helper.MessageHelper;
import tw.com.gftcma.helper.TagAssignmentHelper;
import tw.com.gftcma.pojo.BO.MemberImportRow;
import tw.com.gftcma.pojo.DTO.AddGroupMemberDTO;
import tw.com.gftcma.pojo.DTO.AddMemberForAdminDTO;
import tw.com.gftcma.pojo.DTO.GroupRegistrationDTO;
import tw.com.gftcma.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.gftcma.pojo.VO.MemberImportResultVO;
import tw.com.gftcma.pojo.entity.Attendees;
import tw.com.gftcma.pojo.entity.Member;
import tw.com.gftcma.service.AttendeesService;
import tw.com.gftcma.service.AttendeesTagService;
import tw.com.gftcma.service.InvitedSpeakerService;
import tw.com.gftcma.service.MemberService;
import tw.com.gftcma.service.MemberTagService;
import tw.com.gftcma.service.OrdersService;
import tw.com.gftcma.service.SettingService;
import tw.com.gftcma.service.TagService;
import tw.com.gftcma.pojo.excelPojo.MemberImportExcel;
import tw.com.gftcma.utils.CountryUtil;

@Component
@RequiredArgsConstructor
public class MemberRegistrationManager {

	/**
	 * 匯入名單時,會員資格一律視為 Member
	 */
	private static final MemberCategoryEnum IMPORT_DEFAULT_CATEGORY = MemberCategoryEnum.MEMBER;

	/**
	 * 匯入名單時的餐食預設值,名單上沒有這個欄位
	 */
	private static final String IMPORT_DEFAULT_FOOD = "葷";

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	// 團體折扣 , 從application.yml 進行修改 
	@Value("${project.group-discount}")
	private Double GROUP_DISCOUNT;


	private final RegistrationFeeConfig registrationFeeConfig;

	private final ProjectModeContext projectModeContext;

	private final MessageHelper messageHelper;
	private final TagAssignmentHelper tagAssignmentHelper;
	private final MemberConvert memberConvert;
	private final MemberService memberService;
	private final OrdersService ordersService;
	private final AttendeesService attendeesService;
	private final TagService tagService;
	private final MemberTagService memberTagService;
	private final AttendeesTagService attendeesTagService;
	private final SettingService settingService;
	private final InvitedSpeakerService invitedSpeakerService;

	/**
	 * 註冊功能,新增會員,產生「付費」訂單
	 * 
	 * @param addMemberDTO
	 * @return
	 */
	@Transactional
	public SaTokenInfo addMember(AddMemberDTO addMemberDTO) {

		// 1.先判斷是否處於註冊時間內
		if (!settingService.isRegistrationOpen()) {
			throw new RegistrationClosedException(messageHelper.get(I18nMessageKey.Registration.CLOSED));
		}

		// 2.新增會員
		Member member = memberService.addMember(addMemberDTO);

		// 3.以當前模式策略,執行註冊流程 (計算金額=>產生訂單=>產生通知信並寄出)
		projectModeContext.getStrategy().handleRegistration(member);

		// 4.獲取當下Member群體的Index,進行會員標籤分組
		tagAssignmentHelper.assignTag(member.getMemberId(), memberService::getMemberGroupIndex,
				tagService::getOrCreateMemberGroupTag, memberTagService::addMemberTag);

		// 5.獲取當下Member Category群體的Index,進行會員身份標籤分組
		tagAssignmentHelper.assignMemberCategoryTag(member.getMemberId(),
				MemberCategoryEnum.fromValue(member.getCategory()), memberService::getMemberCategoryGroupIndex,
				tagService::getOrCreateMemberCategoryGroupTag, memberTagService::addMemberTag);

		// 6.返回token , 讓用戶於註冊後登入
		return memberService.login(member);
	}

	/**
	 * 團體報名 註冊功能,新增會員,產生「付費」訂單
	 * 
	 * @param groupRegistrationDTO
	 */
	@Transactional
	public void addGroupMember(GroupRegistrationDTO groupRegistrationDTO) {

		// 1.先判斷是否處於 團體報名 註冊時間內
		if (!settingService.isGroupRegistrationOpen()) {
			throw new RegistrationClosedException(messageHelper.get(I18nMessageKey.Registration.Group.CLOSED));
		}

		// 2.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();

		// 3.在外部直接產生團體的代號
		String groupCode = UUID.randomUUID().toString();

		// 4.提取團體報名的所有人，方便後續調用
		List<AddGroupMemberDTO> groupMembers = groupRegistrationDTO.getGroupMembers();

		// 5.計算所有成員的費用總和，折扣後的金額總額(9折
		BigDecimal discountedTotalFee = groupMembers.stream()
				.map(m -> registrationFeeConfig.getFee(registrationPhaseEnum.getValue(),
						CountryUtil.getTaiwanOrForeign(m.getCountry()),
						MemberCategoryEnum.fromValue(m.getCategory()).getConfigKey()))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.multiply(BigDecimal.valueOf(GROUP_DISCOUNT));

		// 6.團體報名有複數會員,遍歷進行新增
		for (int i = 0; i < groupMembers.size(); i++) {

			// 6-1獲取當前團體報名對象
			AddGroupMemberDTO addGroupMemberDTO = groupMembers.get(i);
			boolean isMaster = i == 0;

			// 6-2新增會員
			Member member = memberService.addMemberByRoleAndGroup(groupCode,
					isMaster ? GroupRegistrationEnum.MASTER.getValue() : GroupRegistrationEnum.SLAVE.getValue(),
					addGroupMemberDTO);

			// 6-3以當前模式,去執行團體報名的策略
			projectModeContext.getStrategy().handleGroupRegistration(member, isMaster, discountedTotalFee);

			// 6-4.獲取當下Member群體的Index,進行會員標籤分組
			tagAssignmentHelper.assignTag(member.getMemberId(), memberService::getMemberGroupIndex,
					tagService::getOrCreateMemberGroupTag, memberTagService::addMemberTag);

			// 6-5.獲取當下Member Category群體的Index,進行會員身份標籤分組
			tagAssignmentHelper.assignMemberCategoryTag(member.getMemberId(),
					MemberCategoryEnum.fromValue(member.getCategory()), memberService::getMemberCategoryGroupIndex,
					tagService::getOrCreateMemberCategoryGroupTag, memberTagService::addMemberTag);

			
		}

	}

	/**
	 * 後台新增會員功能,產生「免費」訂單
	 * 
	 * @param addMemberForAdminDTO
	 */
	@Transactional
	public void addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO) {

		// 1.判斷Email是否被註冊，如果沒有新增會員
		Member member = memberService.addMemberForAdmin(addMemberForAdminDTO);

		// 2.會員新增後的後續流程
		this.completeAdminRegistration(member);

	}

	/**
	 * 後台新增會員後的共用流程<br>
	 * 免費且已付款的訂單 => 會員標籤分組 => 會員身份標籤分組 => 與會者 => 與會者標籤分組 => 講者名單<br>
	 * <br>
	 * 由 {@link #addMemberForAdmin(AddMemberForAdminDTO)} 與 {@link #importMemberExcel(MultipartFile)} 共用,
	 * 兩者差別只在於「Member 是怎麼被新增的」
	 *
	 * @param member 已經新增完成(具有memberId)的會員
	 */
	private void completeAdminRegistration(Member member) {

		// 1.新增「免費」的訂單,並標註 「已付款」
		ordersService.createFreeRegistrationOrder(member);

		// 2.獲取當下Member群體的Index,進行會員標籤分組
		tagAssignmentHelper.assignTag(member.getMemberId(), memberService::getMemberGroupIndex,
				tagService::getOrCreateMemberGroupTag, memberTagService::addMemberTag);

		// 3.獲取當下Member Category群體的Index,進行會員身份標籤分組
		tagAssignmentHelper.assignMemberCategoryTag(member.getMemberId(),
				MemberCategoryEnum.fromValue(member.getCategory()), memberService::getMemberCategoryGroupIndex,
				tagService::getOrCreateMemberCategoryGroupTag, memberTagService::addMemberTag);

		// 4.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 5.獲取當下與會者群體的Index,進行與會者標籤分組
		tagAssignmentHelper.assignTag(attendees.getAttendeesId(), attendeesService::getAttendeesGroupIndex,
				tagService::getOrCreateAttendeesGroupTag, attendeesTagService::addAttendeesTag);

		// 6.如果是講者身分,則新增到invited-speaker, 這個也再考慮, 可能違反SRP
		if (MemberCategoryEnum.SPEAKER.getValue().equals(member.getCategory())) {
			invitedSpeakerService.addInviredSpeaker(member);
		}

	}

	/**
	 * 匯入會員名單 (Excel)<br>
	 * 匯入的對象都是已經繳完費的人,所以走的是後台新增會員的流程:<br>
	 * Member => 免費且已付款的訂單 => 會員標籤分組 => Attendees => 與會者標籤分組<br>
	 * <br>
	 * 名單上會有多位不同的人共用同一個信箱,所以這裡「允許」重複的Email:<br>
	 * 每一列都會產生自己的 Member 與 Attendees,報到QRcode 是綁 attendeesId 而不是Email,<br>
	 * 因此共用信箱的人各自會收到自己的QRcode,簽到退也互不影響。<br>
	 * 重複的Email仍會列在回傳結果的 duplicateEmailRows 中供後台核對,但不會被跳過。<br>
	 * <br>
	 * 唯一會被跳過的情況是「Email為空白」,因為寄送報到QRcode時沒有收件者
	 *
	 * @param file 匯入的名單
	 * @return 匯入結果(總筆數 / 成功筆數 / 跳過明細 / 重複Email明細)
	 * @throws IOException
	 */
	@Transactional
	public MemberImportResultVO importMemberExcel(MultipartFile file) throws IOException {

		// 1.讀取Excel,並保留每一筆資料在Excel中的實際列號
		List<MemberImportRow> rowList = this.readImportRows(file);

		// 2.一次撈出系統中既有的Email,避免每一列都打一次DB。這裡只用於「回報」,不會擋下匯入
		Set<String> existingEmails = memberService.list(new LambdaQueryWrapper<Member>().select(Member::getEmail))
				.stream()
				.map(Member::getEmail)
				.filter(Objects::nonNull)
				.map(MemberRegistrationManager::normalizeEmail)
				.collect(Collectors.toSet());

		// 3.本次匯入已處理過的Email,用來認出「同一份檔案內」的重複
		Set<String> importedEmails = new HashSet<>();

		MemberImportResultVO result = new MemberImportResultVO();

		// 4.逐列匯入
		for (MemberImportRow row : rowList) {

			MemberImportExcel data = row.getData();
			String email = trimToNull(data.getEmail());
			String chineseName = trimToNull(data.getChineseName());

			result.setTotalCount(result.getTotalCount() + 1);

			// 4-1.沒有Email就沒辦法寄報到QRcode,這是唯一會被跳過的情況
			if (email == null) {
				result.addSkipped(row.getRowNumber(), null, chineseName, "Email 為空白");
				continue;
			}

			String emailKey = normalizeEmail(email);

			// 4-2.重複的Email照常匯入,只記錄下來讓後台知道這個信箱會收到多封信
			if (!importedEmails.add(emailKey)) {
				result.addDuplicateEmail(row.getRowNumber(), email, chineseName, "Email 與這份檔案中前面的資料重複");
			} else if (existingEmails.contains(emailKey)) {
				result.addDuplicateEmail(row.getRowNumber(), email, chineseName, "Email 已存在於系統中");
			}

			// 4-3.新增會員。這裡不走 memberService.addMemberForAdmin,因為它會擋掉重複的Email
			Member member = memberConvert
					.forAdminAddDTOToEntity(this.toAddMemberForAdminDTO(data, email, chineseName));
			memberService.save(member);

			// 4-4.後續沿用後台新增會員的完整流程
			this.completeAdminRegistration(member);

			result.addImported();
		}

		return result;
	}

	/**
	 * 下載會員名單的匯入模板 (Excel)
	 *
	 * @param response
	 * @throws IOException
	 */
	public void generateImportTemplate(HttpServletResponse response) throws IOException {

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("會員匯入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 匯入是以「欄位順序」對應的,所以模板的標題只是給人看的,順序才是重點
		List<List<String>> head = List.of(List.of("報名者類型"), List.of("電子郵件地址"), List.of("中文姓名"),
				List.of("服務單位 / Organization"), List.of("職稱 / Title"), List.of("手機 / Mobile Number"));

		List<List<String>> example = List
				.of(List.of("報名者", "user@example.com", "王小明", "OO中醫診所", "醫師", "0912345678"));

		EasyExcel.write(response.getOutputStream()).head(head).sheet("匯入模板").doWrite(example);
	}

	/**
	 * 讀取匯入的Excel,並保留每一筆資料在Excel中的實際列號
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private List<MemberImportRow> readImportRows(MultipartFile file) throws IOException {

		List<MemberImportRow> rowList = new ArrayList<>();

		EasyExcel.read(file.getInputStream(), MemberImportExcel.class, new AnalysisEventListener<MemberImportExcel>() {

			@Override
			public void invoke(MemberImportExcel data, AnalysisContext context) {
				// 整列都是空的就直接略過,客戶的名單尾端常常帶著大量空白列
				if (isBlankRow(data)) {
					return;
				}
				// rowIndex為0-based,加1才是Excel上看到的列號
				rowList.add(new MemberImportRow(context.readRowHolder().getRowIndex() + 1, data));
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
				// 不需要收尾動作
			}

		}).sheet().headRowNumber(1).doRead();

		if (rowList.isEmpty()) {
			throw new ImportExcelException("匯入的檔案中沒有任何資料");
		}

		return rowList;
	}

	/**
	 * 將Excel的一列,組裝成後台新增會員用的DTO<br>
	 * 名單上沒有的必填欄位,一律帶入匯入預設值
	 *
	 * @param data        Excel該列的原始資料
	 * @param email       已去除前後空白的Email
	 * @param chineseName 已去除前後空白的中文姓名
	 * @return
	 */
	private AddMemberForAdminDTO toAddMemberForAdminDTO(MemberImportExcel data, String email, String chineseName) {

		AddMemberForAdminDTO addMemberForAdminDTO = new AddMemberForAdminDTO();
		addMemberForAdminDTO.setEmail(email);
		addMemberForAdminDTO.setChineseName(chineseName);
		// 名單只有中文姓名,華人的姓氏在前,所以統一放進lastName
		addMemberForAdminDTO.setLastName(chineseName);
		addMemberForAdminDTO.setAffiliation(trimToNull(data.getAffiliation()));
		addMemberForAdminDTO.setJobTitle(trimToNull(data.getJobTitle()));
		addMemberForAdminDTO.setPhone(trimToNull(data.getPhone()));
		// 名單上的報名者類型不對應會員資格,保留在備註供後台辨識
		addMemberForAdminDTO.setRemark(trimToNull(data.getRegistrationType()));

		// 以下是Excel沒有,但Member必要的欄位,帶入匯入預設值
		addMemberForAdminDTO.setCategory(IMPORT_DEFAULT_CATEGORY.getValue());
		addMemberForAdminDTO.setCountry(CountryUtil.getHomeCountry());
		addMemberForAdminDTO.setFood(IMPORT_DEFAULT_FOOD);
		// 密碼給隨機字串,會員可以透過「找回密碼」取得
		addMemberForAdminDTO.setPassword(UUID.randomUUID().toString().replace("-", "").substring(0, 8));

		return addMemberForAdminDTO;
	}

	/**
	 * 判斷Excel的這一列是不是完全空白
	 */
	private static boolean isBlankRow(MemberImportExcel data) {
		return trimToNull(data.getRegistrationType()) == null && trimToNull(data.getEmail()) == null
				&& trimToNull(data.getChineseName()) == null && trimToNull(data.getAffiliation()) == null
				&& trimToNull(data.getJobTitle()) == null && trimToNull(data.getPhone()) == null;
	}

	/**
	 * 去除前後空白,空字串一律視為null
	 */
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Email比對用的標準化,大小寫不同視為同一個Email
	 */
	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
