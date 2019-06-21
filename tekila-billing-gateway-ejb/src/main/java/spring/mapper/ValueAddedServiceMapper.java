package spring.mapper;


import com.jaravir.tekila.module.service.entity.ValueAddedService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring.dto.ValueAddedServiceDTO;
import spring.mapper.subscription.VasCodeMapper;

@Mapper(componentModel = "spring", uses = {VasCodeMapper.class, ServiceProviderMapper.class})
public interface ValueAddedServiceMapper extends EntityMapper<ValueAddedServiceDTO, ValueAddedService> {

    @Mapping(ignore = true, target="resource")
    @Mapping(ignore = true, target="category")
    @Mapping(ignore = true, target="settings")
    ValueAddedService toEntity(ValueAddedServiceDTO valueAddedServiceDTO);

}
