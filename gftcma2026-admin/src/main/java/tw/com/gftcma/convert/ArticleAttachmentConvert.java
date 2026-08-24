package tw.com.gftcma.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddArticleAttachmentDTO;
import tw.com.gftcma.pojo.entity.ArticleAttachment;

@Mapper(componentModel = "spring")
public interface ArticleAttachmentConvert {
	ArticleAttachment addDTOToEntity(AddArticleAttachmentDTO addArticleAttachmentDTO);
}
