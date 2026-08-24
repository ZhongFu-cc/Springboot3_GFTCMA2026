package tw.com.gftcma.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.gftcma.pojo.VO.CheckinRecordVO;
import tw.com.gftcma.pojo.entity.CheckinRecord;
import tw.com.gftcma.pojo.excelPojo.AttendeesExcel;
import tw.com.gftcma.pojo.excelPojo.CheckinRecordExcel;

@Mapper(componentModel = "spring")
public interface CheckinRecordConvert {

	CheckinRecord addDTOToEntity(AddCheckinRecordDTO addCheckinRecordDTO);

	CheckinRecord putDTOToEntity(PutCheckinRecordDTO putCheckinRecordDTO);

	CheckinRecordVO entityToVO(CheckinRecord checkinRecord);

	List<CheckinRecordVO> entityListToVOList(List<CheckinRecord> checkinRecordList);

	CheckinRecordExcel attendeesExcelToCheckinRecordExcel(AttendeesExcel attendeesExcel);
	
}
