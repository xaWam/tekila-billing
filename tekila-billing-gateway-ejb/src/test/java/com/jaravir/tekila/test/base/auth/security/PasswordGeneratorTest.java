package com.jaravir.tekila.test.base.auth.security;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.module.auth.security.PasswordGenerator;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Created by sajabrayilov on 12/13/2014.
 */

public class PasswordGeneratorTest {
    private final static Logger log = Logger.getLogger("com.jaravir.tekila");

    @Test
    public void testGeneratePassword() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        char[] pass = passwordGenerator.generatePassword();
        System.out.println("Password: " + String.valueOf(pass) + ", hash=" + passwordGenerator.encodePassword(pass));
    }
}
