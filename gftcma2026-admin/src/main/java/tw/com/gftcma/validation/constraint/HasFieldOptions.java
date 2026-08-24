package tw.com.gftcma.validation.constraint;

import tw.com.gftcma.enums.FormFieldTypeEnum;
import tw.com.gftcma.pojo.DTO.FormFieldOptionDTO;

public interface HasFieldOptions {

	public FormFieldTypeEnum getFieldType();
	
	public FormFieldOptionDTO getOptions();
	
}
