package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring.dto.SubscriptionCreationDTO;
import spring.dto.SubscriptionDTO;
import spring.dto.SubscriptionSmallDTO;
import spring.mapper.EntityMapper;
import spring.mapper.SubscriberSmallDetailsMapper;
import spring.mapper.SubscriptionServiceMapper;

@Mapper(componentModel = "spring",
        uses = {BalanceMapper.class,
                SubscriptionDetailMapper.class,
                SubscriptionDetailsMapper.class,
                ServiceMapper.class,
                SubscriptionServiceMapper.class,
                ServicePropertyMapper.class,
                SubscriberSmallDetailsMapper.class
        })
public interface SubscriptionMapper extends EntityMapper<SubscriptionDTO, Subscription> {

    SubscriptionCreationDTO toCreationDTO(Subscription subscription);

    SubscriptionSmallDTO toSmallDTO(Subscription subscription);

    @Mapping(ignore = true, target="subscriberDetails")
    @Mapping(source="billingModel.principle", target="billingModel")
    SubscriptionDTO toDto(Subscription subscription);

    @Mapping(ignore=true, target="billingModel")
    Subscription toEntity(SubscriptionDTO subscriptionDTO);

//    @Mapping(source = "", target = "")
    Subscription creationDTOtoEntity(SubscriptionCreationDTO subscriptionCreationDTO);

}