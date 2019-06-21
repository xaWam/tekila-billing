package spring.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ElmarMa on 5/3/2018
 */
public class UserHolder {

    public static final Map<String,String> credentials = new ConcurrentHashMap<>();

}
