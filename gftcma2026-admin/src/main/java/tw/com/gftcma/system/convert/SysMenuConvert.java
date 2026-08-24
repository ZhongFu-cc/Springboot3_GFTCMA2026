package tw.com.gftcma.system.convert;

import org.mapstruct.Mapper;

import tw.com.gftcma.system.pojo.BO.RouteBO;
import tw.com.gftcma.system.pojo.entity.SysMenu;

@Mapper(componentModel = "spring")
public interface SysMenuConvert {
	//最後返回為UserVo對象, 方法名為entityToVO, 參數為User對象
		RouteBO entityToBO(SysMenu sysMenu);
}
