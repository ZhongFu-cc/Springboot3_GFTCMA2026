package tw.com.gftcma.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.com.gftcma.pojo.entity.InvitedSpeaker;

@Mapper(componentModel = "spring")
public interface InvitedSpeakerConvert {

	InvitedSpeaker addDTOToEntity(AddInvitedSpeakerDTO addInvitedSpeakerDTO);

	InvitedSpeaker putDTOToEntity(PutInvitedSpeakerDTO putInvitedSpeakerDTO);
	
	
}
