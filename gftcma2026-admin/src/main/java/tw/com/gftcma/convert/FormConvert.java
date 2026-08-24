package tw.com.gftcma.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.enums.CommonStatusEnum;
import tw.com.gftcma.pojo.DTO.addEntityDTO.AddFormDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutFormDTO;
import tw.com.gftcma.pojo.VO.FormVO;
import tw.com.gftcma.pojo.entity.Form;

@Mapper(componentModel = "spring")
public interface FormConvert {

    // 宣告默認映射 , 告訴 MapStruct 如何把 CommonStatusEnum → Integer
    default Integer commonStatusEnumMapToInteger(CommonStatusEnum status) {
        return status == null ? null : status.getValue();
    }
	
	Form addDTOToEntity(AddFormDTO addFormDTO);
	
	Form putDTOToEntity(PutFormDTO putFormDTO);
	
	FormVO entityToVO(Form form);
	
}
