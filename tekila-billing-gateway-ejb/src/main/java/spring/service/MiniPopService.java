package spring.service;


import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.Filters;
import spring.controller.vm.SubscriptionMinipopVM;
import spring.dto.MiniPopDTO;
import spring.mapper.MiniPopMapper;

import javax.ejb.EJB;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author Gurban Azimli
 * @date 19/02/2019 11:18 AM
 */
@Service
public class MiniPopService {


    private static final Logger log = LoggerFactory.getLogger(MiniPopService.class);

    @EJB(mappedName = INJECTION_POINT+"MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPopPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    private MiniPopMapper miniPopMapper;

    public MiniPopService(MiniPopMapper miniPopMapper) {
        this.miniPopMapper = miniPopMapper;
    }


    public List<MiniPopDTO> findAllMiniPops(){
        log.debug("~~~Fetching all MiniPops...");
        return miniPopPersistenceFacade.findAllPaginated(0, 20).stream()
                .map(miniPopMapper::toDto).collect(Collectors.toList());
    }

    public List<MiniPopDTO> findAllMiniPopsByProvider(Long providerId){
        log.debug("~~~Fetching MiniPops by provider Id...");
        return miniPopPersistenceFacade.findByProviderId(providerId)
                .stream()
                .map(miniPopMapper::toDto)
                .collect(Collectors.toList());

//        return miniPopPersistenceFacade.findAll().parallelStream()
//                .filter(m -> m.getProvider().getId() == providerId)
//                .map(miniPopMapper::toDto).collect(Collectors.toList());

    }

    public List<MiniPopDTO> findAllPaginated(String macAddress,
                                             String ip,
                                             String switch_id,
                                             String address,
                                             MinipopCategory category,
                                             Integer pageId,
                                             Integer rowsPerPage){


        log.debug("~~~~~~~~Search starts~~~~~~~~");

        Filters filters = new Filters();
        filters.clearFilters();

        if(Objects.nonNull(macAddress) && !macAddress.isEmpty()){
            MiniPopPersistenceFacade.Filter macFilter = MiniPopPersistenceFacade.Filter.MAC_ADDRESS;
            filters.addFilter(macFilter, macAddress);
        }

        if(Objects.nonNull(ip) && !ip.isEmpty()){
            MiniPopPersistenceFacade.Filter ipFilter = MiniPopPersistenceFacade.Filter.IP;
            filters.addFilter(ipFilter, ip);
        }

        if(Objects.nonNull(switch_id) && !switch_id.isEmpty()){
            MiniPopPersistenceFacade.Filter switchFilter = MiniPopPersistenceFacade.Filter.SWITCH_ID;
            filters.addFilter(switchFilter, switch_id);
        }

        if(Objects.nonNull(address) && !address.isEmpty()){
            MiniPopPersistenceFacade.Filter addressFilter = MiniPopPersistenceFacade.Filter.ADDRESS;
            filters.addFilter(addressFilter, address);
        }

        if(Objects.nonNull(category)){
            MiniPopPersistenceFacade.Filter categoryFilter = MiniPopPersistenceFacade.Filter.CATEGORY;
            filters.addFilter(categoryFilter, category);
        }

        log.debug("~~~~~~~~END OF SEARCH~~~~~~~~");



        return miniPopPersistenceFacade.findAllPaginated((pageId - 1)*rowsPerPage, rowsPerPage, filters)
                .stream()
                .map(miniPopMapper::toDto)
                .collect(Collectors.toList());



    }


    /**
     * TODO : BUG.
     */
    public List<MiniPopDTO> findPaginatedByProviderId(Long providerId,
                                                      String macAddress,
                                                      String ip,
                                                      String switch_id,
                                                      String address,
                                                      MinipopCategory category,
                                                      Integer pageId,
                                                      Integer rowsPerPage){
        log.debug("~~~~MiniPop search starts for providerId : {}~~~~~~" , providerId);


        ServiceProvider provider = serviceProviderPersistenceFacade.find(providerId);

        log.debug("~~~~~~"+provider.getName().toUpperCase()+"~~~~~~~~");

        Filters filters = new Filters();
        filters.clearFilters();

        MiniPopPersistenceFacade.Filter providerFilter = MiniPopPersistenceFacade.Filter.PROVIDER;
        providerFilter.setOperation(MatchingOperation.EQUALS);
        filters.addFilter(providerFilter, provider);


        if(Objects.nonNull(macAddress) && !macAddress.isEmpty()){
            MiniPopPersistenceFacade.Filter macFilter = MiniPopPersistenceFacade.Filter.MAC_ADDRESS;
            filters.addFilter(macFilter, macAddress);
        }

        if(Objects.nonNull(ip) && !ip.isEmpty()){
            MiniPopPersistenceFacade.Filter ipFilter = MiniPopPersistenceFacade.Filter.IP;
            filters.addFilter(ipFilter, ip);
        }

        if(Objects.nonNull(switch_id) && !switch_id.isEmpty()){
            MiniPopPersistenceFacade.Filter switchFilter = MiniPopPersistenceFacade.Filter.SWITCH_ID;
            filters.addFilter(switchFilter, switch_id);
        }

        if(Objects.nonNull(address) && !address.isEmpty()){
            MiniPopPersistenceFacade.Filter addressFilter = MiniPopPersistenceFacade.Filter.ADDRESS;
            filters.addFilter(addressFilter, address);
        }

        if(Objects.nonNull(category)){
            MiniPopPersistenceFacade.Filter categoryFilter = MiniPopPersistenceFacade.Filter.CATEGORY;
            filters.addFilter(categoryFilter, category);
        }



        log.debug("~~~~Search ends for provider with id : {}" , providerId);


        return miniPopPersistenceFacade.findAllPaginated((pageId-1)*rowsPerPage, rowsPerPage, filters)
                .stream()
                .map(miniPopMapper::toDto)
                .collect(Collectors.toList());

    }

    public void setNextAvailablePort(SubscriptionMinipopVM subscriptionMinipopVM){
        log.info("~~~Updating launch for Subscription Settings~~~");
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionMinipopVM.getSubscriptionId());
        log.info("SUBSCRIPTION: " + subscription.toString());
        MiniPop miniPop = miniPopPersistenceFacade.find(subscriptionMinipopVM.getMinipopId());
        log.info("MINIPOP: " + miniPop);

        log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        miniPop.setNextAvailablePortHintAsNumber(subscriptionMinipopVM.getPortId());
        log.info(miniPop.getPreferredPort().toString());
        log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        subscriptionPersistenceFacade.changeMinipop(subscription, miniPop);
    }

    /**
     *  Useful for Pagination
     *
     */
    public long findPaginatedByProviderIdCount(Long providerId,
                                               String macAddress,
                                               String ip,
                                               String switch_id,
                                               String address,
                                               MinipopCategory category){


        ServiceProvider provider = serviceProviderPersistenceFacade.find(providerId);

        log.debug("~~~~~~"+provider.getName().toUpperCase()+"~~~~~~~~");

        Filters filters = new Filters();
        filters.clearFilters();

        MiniPopPersistenceFacade.Filter providerFilter = MiniPopPersistenceFacade.Filter.PROVIDER;
        providerFilter.setOperation(MatchingOperation.EQUALS);
        filters.addFilter(providerFilter, provider);


        if(Objects.nonNull(macAddress) && !macAddress.isEmpty()){
            MiniPopPersistenceFacade.Filter macFilter = MiniPopPersistenceFacade.Filter.MAC_ADDRESS;
            filters.addFilter(macFilter, macAddress);
        }

        if(Objects.nonNull(ip) && !ip.isEmpty()){
            MiniPopPersistenceFacade.Filter ipFilter = MiniPopPersistenceFacade.Filter.IP;
            filters.addFilter(ipFilter, ip);
        }

        if(Objects.nonNull(switch_id) && !switch_id.isEmpty()){
            MiniPopPersistenceFacade.Filter switchFilter = MiniPopPersistenceFacade.Filter.SWITCH_ID;
            filters.addFilter(switchFilter, switch_id);
        }

        if(Objects.nonNull(address) && !address.isEmpty()){
            MiniPopPersistenceFacade.Filter addressFilter = MiniPopPersistenceFacade.Filter.ADDRESS;
            filters.addFilter(addressFilter, address);
        }

        if(Objects.nonNull(category)){
            MiniPopPersistenceFacade.Filter categoryFilter = MiniPopPersistenceFacade.Filter.CATEGORY;
            filters.addFilter(categoryFilter, category);
        }

        log.debug("~~~~Search ends for provider with id : {}" , providerId);

        return miniPopPersistenceFacade.count(filters);

    }



}
