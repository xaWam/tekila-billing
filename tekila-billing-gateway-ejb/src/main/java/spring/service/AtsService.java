package spring.service;

import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.dto.AtsDTO;
import spring.mapper.AtsMapper;

import javax.ejb.EJB;
import javax.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:39 PM
 */
@Service
//@Transactional
public class AtsService {


    private final Logger log = LoggerFactory.getLogger(AtsService.class);

    @EJB(mappedName = INJECTION_POINT+"AtsPersistenceFacade")
    private AtsPersistenceFacade atsPersistenceFacade;

    private final AtsMapper atsMapper;

    public AtsService( AtsMapper atsMapper) {
        this.atsMapper = atsMapper;
    }

    public AtsDTO save(AtsDTO atsDTO){
        log.debug("Request for saving Ats : {}", atsDTO);
        Ats ats = atsMapper.toEntity(atsDTO);
        atsPersistenceFacade.save(ats);
        return atsMapper.toDto(atsPersistenceFacade.findForceRefresh(ats.getId()));
    }


    public AtsDTO update(AtsDTO atsDTO){
        log.debug("Request for updateing Ats : {}", atsDTO);
        Ats ats = atsPersistenceFacade.find(atsDTO.getId());

//        ats.setAtsIndex(atsDTO.getAtsIndex());
//        ats.setCoor(atsDTO.getCoordinate());
//        ats.setName(atsDTO.getName());
//        ats.setStatus(atsDTO.getStatus());

        atsDTO.attachToEntity(ats);

        ats = atsPersistenceFacade.update(ats);

        return atsMapper.toDto(ats);
    }

    public List<AtsDTO> findAll(){
        log.debug("Request to get all Ats");
        return atsPersistenceFacade.findAllAscending().stream().
                map(atsMapper::toDto).collect(Collectors.toList());
    }

    public AtsDTO find(Long id){
        log.debug("Request to get one Ats: {} ", id);
        return atsMapper.toDto(atsPersistenceFacade.find(id));
    }

    public void delete(Long id){
        log.debug("Request to delete Ats: {} ", id);
        Ats ats = atsPersistenceFacade.find(id);
        atsPersistenceFacade.removeIt(ats);
    }

    public List<AtsDTO> findAllByCustomCriteria(String name, String atsIndex){
        log.debug("Request to get all Ats by custom criteria name: {} and atsIndex: {} ",name, atsIndex);
        return atsPersistenceFacade.findAllByCustomCriteria(name, atsIndex)
                .stream()
                .map(atsMapper::toDto).collect(Collectors.toList());

    }
}
