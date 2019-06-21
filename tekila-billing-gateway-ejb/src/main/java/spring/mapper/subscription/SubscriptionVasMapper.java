package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring.dto.SubscriptionVASCreationDTO;
import spring.dto.SubscriptionVASDTO;
import spring.mapper.EntityMapper;
import spring.mapper.ResourceMapper;
import spring.mapper.ValueAddedServiceMapper;

@Mapper(componentModel = "spring",
        uses = {ValueAddedServiceMapper.class, SubscriptionMapper.class, ResourceMapper.class , VasCodeMapper.class})
public interface SubscriptionVasMapper extends EntityMapper<SubscriptionVASDTO, SubscriptionVAS> {

//    @Mapping(source = "subscriptionId", target = "subscription.id")
//    SubscriptionVAS toEntity(SubscriptionVASDTO subscriptionVASDTO);
//
//    @Mapping(source = "subscriptionId", target = "subscription.id")
//    @Mapping(source = "vasId", target = "vas.id")
//    SubscriptionVAS toEntity(SubscriptionVASCreationDTO subscriptionVASCreationDTO);

    @Mapping(source = "subscription.id", target = "subscriptionId")
    SubscriptionVASDTO toDto(SubscriptionVAS subscriptionVAS);

    @Mapping(source = "subscription.id", target = "subscriptionId")
    @Mapping(source = "vas.id", target = "vasId")
    SubscriptionVASCreationDTO toCreationDTO(SubscriptionVAS subscriptionVAS);

}
