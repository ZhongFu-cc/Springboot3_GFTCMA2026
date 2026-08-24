package tw.com.gftcma.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.com.gftcma.pojo.entity.PublishFile;

@Mapper(componentModel = "spring")
public interface PublishFileConvert {

	PublishFile addDTOToEntity(AddPublishFileDTO addPublishFileDTO);

	PublishFile putDTOToEntity(PutPublishFileDTO putPublishFileDTO);

}
