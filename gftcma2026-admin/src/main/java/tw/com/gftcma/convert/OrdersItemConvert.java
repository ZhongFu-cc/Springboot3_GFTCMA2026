package tw.com.gftcma.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.com.gftcma.pojo.VO.OrdersItemVO;
import tw.com.gftcma.pojo.entity.OrdersItem;

@Mapper(componentModel = "spring")
public interface OrdersItemConvert {

	OrdersItem addDTOToEntity(AddOrdersItemDTO addOrdersItemDTO);

	OrdersItem putDTOToEntity(PutOrdersItemDTO putOrdersItemDTO);
	
	OrdersItemVO entityToVO(OrdersItem ordersItem);
	
	List<OrdersItemVO> entityListToVOList(List<OrdersItem> ordersItemList);
	
}
