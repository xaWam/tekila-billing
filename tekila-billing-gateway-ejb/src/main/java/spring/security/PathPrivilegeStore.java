package spring.security;

import com.jaravir.tekila.base.auth.PathPrivilegeItem;
import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.SubModule;
import com.jaravir.tekila.module.auth.SubModulePersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import javax.annotation.PostConstruct;
import javax.ejb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spring.util.Constants.INJECTION_POINT;


/**
 * @author MusaAl
 * @date 6/7/2018 : 5:21 PM
 */
@Service
//@Startup
public class PathPrivilegeStore {

    private static final Logger log = LoggerFactory.getLogger(PathPrivilegeStore.class);
    @EJB(mappedName = INJECTION_POINT+"SubModulePersistenceFacade")
    private SubModulePersistenceFacade submoduleFacade;
    private static final Map<String, List<PathPrivilegeItem>> pathPrivMap = new ConcurrentHashMap<>();
    private static final Map<String, List<PathPrivilegeItem>> defaultPrivMap = new ConcurrentHashMap<>();
    private static volatile PathPrivilegeStore instance;

    private PathPrivilegeStore(){
    }

    public static PathPrivilegeStore getInstance(){
        if(instance == null){
            synchronized (PathPrivilegeStore.class){
                instance = new PathPrivilegeStore();
            }
        }
        return instance;
    }

    @PostConstruct
    public void init(){
        //add default permissions
        pathPrivMap.putAll(getDefaultPermissions());
        populate(submoduleFacade.findAll());
    }


    public void populate(List<SubModule> subModuleList) {
        log.info("Loading path - privilege mapping...");
        for (SubModule subModule : subModuleList) {
            if (subModule.getPathMap() == null || subModule.getPathMap().isEmpty())
                continue;
            for (Map.Entry<String, Privilege> entry : subModule.getPathMap().entrySet()) {
                if (pathPrivMap.containsKey(entry.getKey())) {
                    pathPrivMap.get(entry.getKey()).add(new PathPrivilegeItem(subModule.getName(), entry.getValue()));
                }
                else {
                    List<PathPrivilegeItem> privList = new ArrayList<>();
                    privList.add(new PathPrivilegeItem(subModule.getName(), entry.getValue()));
                    pathPrivMap.put(entry.getKey(), privList);
                }
//                log.info(String.format("Added submodule=%s, path=%s, privilege=%s", subModule, entry.getKey(), entry.getValue()));
            }
        }
        log.info(String.format("Finished loading path - privileges. From Upgraded Security Boomber Total=%d, map=%s", pathPrivMap.size(), pathPrivMap));
//        printMap(pathPrivMap);
    }

    private static Map<String, List<PathPrivilegeItem>> getDefaultPermissions () {

        List<PathPrivilegeItem> manualLinkPrivList = new ArrayList<>();

        manualLinkPrivList.add(new PathPrivilegeItem("Subscription", Privilege.READ));
        defaultPrivMap.put("admin/docs/index.xhtml",manualLinkPrivList);

        List<PathPrivilegeItem> manualDocPrivList = new ArrayList<>();

        manualDocPrivList.add(new PathPrivilegeItem("Subscription", Privilege.READ));
        defaultPrivMap.put("admin/docs/TEKILA_Billing_System_manual_current.pdf", manualDocPrivList);
        return defaultPrivMap;
    }

    public static List<PathPrivilegeItem> check(String path) {
//        log.debug("check: Checking store for path="+path);
        if (pathPrivMap.containsKey(path)) {
//            log.debug("check: path found. Finished");
            return pathPrivMap.get(path);
        }
//        log.debug("check: Path not found. Finished");
        return null;
    }


    public static void printMap(Map<String, List<PathPrivilegeItem>> pathPrivMap){
        pathPrivMap.forEach((k,v)-> {
            String privileges="";
            for(int i=0; i<v.size(); i++){
                privileges+=v.get(i)+" ";
            }
           log.info("key: "+k+" privileges "+privileges);
        });
    }

}
