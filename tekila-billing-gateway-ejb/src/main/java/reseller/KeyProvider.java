package reseller;

import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.crypto.KeyGenerator;
import javax.ejb.Singleton;
import java.security.Key;

@Singleton
public class KeyProvider {
    private final static Logger log = Logger.getLogger(KeyProvider.class);
    private KeyGenerator generator;
    private Key key;

    @PostConstruct
    public void init() {
        try {
            generator = KeyGenerator.getInstance("AES");
            key = KeyGenerator.getInstance("AES").generateKey();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public KeyGenerator getInstance() {
        return generator;
    }

    public Key getKey() {
        return key;
    }
}
