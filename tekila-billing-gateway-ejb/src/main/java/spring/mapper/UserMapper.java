package spring.mapper;

import com.jaravir.tekila.base.auth.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring.dto.UserDTO;
import spring.dto.UserDTOSecure;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface UserMapper extends EntityMapper<UserDTO, User>{

    @Mapping(source = "userName", target = "username")
    @Mapping(source = "dsc", target = "description")
    UserDTO toDto(User user);

    @Mapping(source = "userName", target = "username")
    @Mapping(source = "dsc", target = "description")
    UserDTOSecure toSecureDto(User user);

    @Mapping(source = "username", target = "userName")
    @Mapping(source = "description", target = "dsc")
    User toEntity(UserDTOSecure userDTOSecure);

}
