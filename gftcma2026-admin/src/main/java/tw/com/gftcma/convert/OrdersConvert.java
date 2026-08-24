package tw.com.gftcma.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.gftcma.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.gftcma.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.gftcma.pojo.VO.OrdersVO;
import tw.com.gftcma.pojo.entity.Orders;

@Mapper(componentModel = "spring")
public interface OrdersConvert {

	Orders addDTOToEntity(AddOrdersDTO addOrdersDTO);

	Orders putDTOToEntity(PutOrdersDTO putOrdersDTO);
	
	OrdersVO entityToVO(Orders orders);
	
	List<OrdersVO> entityListToVOList(List<Orders> ordersList);
	
}
