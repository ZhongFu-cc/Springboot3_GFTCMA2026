package tw.com.gftcma.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.com.gftcma.pojo.entity.EmailTemplate;

@Mapper(componentModel = "spring")
public interface EmailTemplateConvert {

	EmailTemplate insertDTOToEntity(AddEmailTemplateDTO addArticleDTO);

	EmailTemplate updateDTOToEntity(PutEmailTemplateDTO updateArticleDTO);
	
}
