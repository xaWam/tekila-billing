package spring.service;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResourceBucket;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionResourceBucketPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionResourcePersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.dto.SubscriptionResourceBucketDTO;
import spring.dto.SubscriptionResourceDTO;
import spring.mapper.subscription.SubscriptionResourceBucketMapper;
import spring.mapper.subscription.SubscriptionResourceMapper;

import javax.ejb.EJB;

import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author GurbanAz
 * @date 01/02/2019 11:51 AM
 *
 */

@Service
public class SubscriptionResourceService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionResourceService.class);

    @EJB(mappedName = INJECTION_POINT+"SubscriptionResourcePersistenceFacade")
    private SubscriptionResourcePersistenceFacade subscriptionResourcePersistenceFacade;
    @EJB(mappedName = INJECTION_POINT+"SubscriptionResourceBucketPersistenceFacade")
    private SubscriptionResourceBucketPersistenceFacade subscriptionResourceBucketPersistenceFacade;

    private SubscriptionResourceMapper resourceMapper;

    private SubscriptionResourceBucketMapper bucketMapper;

    public SubscriptionResourceService(SubscriptionResourceMapper resourceMapper, SubscriptionResourceBucketMapper bucketMapper) {
        this.resourceMapper = resourceMapper;
        this.bucketMapper = bucketMapper;
    }

    public List<SubscriptionResourceDTO> findSubscriptionResourceBySubscriptionId(Long id){
        return subscriptionResourcePersistenceFacade.findSubscriptionResourceBySubscriptionId(id)
                .stream().map(resourceMapper::toDto).collect(Collectors.toList());
    }

    public SubscriptionResourceBucketDTO findResourceBucketById(Long id){
        return bucketMapper.toDto(subscriptionResourceBucketPersistenceFacade.find(id));
    }

    public SubscriptionResourceBucket updateSubscriptionResourceBucket(SubscriptionResourceBucketDTO subscriptionResourceBucketDTO){
        log.debug("ID FROM INCOMING DTO : {}" , subscriptionResourceBucketDTO.getId());
        if(subscriptionResourceBucketDTO.getId() == 0){
            log.error("CANNOT FIND PROPERTY WITH ID : 0");
        }
        return subscriptionResourceBucketPersistenceFacade.update(bucketMapper.toEntity(subscriptionResourceBucketDTO));
    }


    public void deleteResourceBucketById(Long id){
        SubscriptionResourceBucket bucket = bucketMapper.toEntity(findResourceBucketById(id));
        subscriptionResourceBucketPersistenceFacade.delete(bucket);
    }

}
