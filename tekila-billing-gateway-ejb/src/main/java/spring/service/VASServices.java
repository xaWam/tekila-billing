package spring.service;

import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.Filters;
import spring.dto.ValueAddedServiceDTO;
import spring.mapper.ValueAddedServiceMapper;

import javax.ejb.EJB;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;


@Service
public class VASServices {

    private final Logger log = LoggerFactory.getLogger(VASServices.class);

    @EJB(mappedName = INJECTION_POINT + "VASPersistenceFacade")
    private VASPersistenceFacade vasPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;

    private final ValueAddedServiceMapper valueAddedServiceMapper;

    public VASServices(ValueAddedServiceMapper valueAddedServiceMapper) {
        this.valueAddedServiceMapper = valueAddedServiceMapper;
    }

    public List<ValueAddedServiceDTO> findAll() {
        log.info("findAllVas()");
        List<ValueAddedService> valueAddedService = vasPersistenceFacade.findAllPaginated(0,20);
        return valueAddedService.stream().map(valueAddedServiceMapper::toDto).collect(Collectors.toList());
    }

    public ValueAddedServiceDTO findOne(Long id){
        ValueAddedService vas = vasPersistenceFacade.find(id);
        return valueAddedServiceMapper.toDto(vas);
    }

    public List<ValueAddedServiceDTO> findAllPaginatedByProvider(Long providerId,
                                                                 String vasName,
                                                                 Integer pageId,
                                                                 Integer rowsPerPage){
//        ServiceProvider provider = serviceProviderPersistenceFacade.find(providerId);

        Filters filters = new Filters();
        filters.clearFilters();

        VASPersistenceFacade.Filter providerIdFilter = VASPersistenceFacade.Filter.PROVIDER;
        providerIdFilter.setOperation(MatchingOperation.EQUALS);
        filters.addFilter(providerIdFilter, providerId);

        if(Objects.nonNull(vasName) && !vasName.isEmpty()){
            VASPersistenceFacade.Filter vasNameFilter = VASPersistenceFacade.Filter.NAME;
            filters.addFilter(vasNameFilter, vasName);
        }

        return vasPersistenceFacade.findAllPaginated((pageId-1)*rowsPerPage, rowsPerPage, filters)
                .stream()
                .map(valueAddedServiceMapper::toDto)
                .collect(Collectors.toList());
    }

    public long findAllPaginatedByProviderCount(Long providerId,
                                                String vasName){

        Filters filters = new Filters();
        filters.clearFilters();

        VASPersistenceFacade.Filter providerIdFilter = VASPersistenceFacade.Filter.PROVIDER;
        providerIdFilter.setOperation(MatchingOperation.EQUALS);
        filters.addFilter(providerIdFilter, providerId);

        if(Objects.nonNull(vasName) && !vasName.isEmpty()){
            VASPersistenceFacade.Filter vasNameFilter = VASPersistenceFacade.Filter.NAME;
            filters.addFilter(vasNameFilter, vasName);
        }
        return vasPersistenceFacade.count(filters);
    }
}


