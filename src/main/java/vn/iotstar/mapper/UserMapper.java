package vn.iotstar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // productCount không có trong entity => service tự set
    @Mapping(target = "roleName", source = "role.name")
    UserDTO toDTO(User user);

    // role/password/products do service xử lý (không map từ form)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "products", ignore = true)
    User toEntity(UserDTO dto);
}
