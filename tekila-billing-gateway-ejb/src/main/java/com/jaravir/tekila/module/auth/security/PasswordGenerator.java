package com.jaravir.tekila.module.auth.security;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by sajabrayilov on 12/13/2014.
 */
@Singleton
public class PasswordGenerator {
    private final static char[] charList = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'W', 'X', 'Y', 'Z'
    };
    private final static char[] specialChars = {
            '-', '*', '+', '=', '%', '#', '$', '@', '(', ')'
    };

    private final static Logger log = Logger.getLogger(PasswordGenerator.class);

    public char[] generatePassword() {
        char[] pass = new char[8];

        List<String> usedCharList = new ArrayList<>();
        List<Integer> usedNumbers = new ArrayList<>();
        List<Character> usedSpecials = new ArrayList<>();

        Random random = new Random(System.currentTimeMillis());
        //position of the special char
        int firstSpecialPosition = random.nextInt(3) + 1;
        int firstDigitPosition = firstSpecialPosition;

        do {
            firstDigitPosition = random.nextInt(3) + 1;
        } while (firstDigitPosition == firstSpecialPosition);

        int lastSpeicalPosition = random.nextInt(4) + 4;
        int lastDigitPosition = lastSpeicalPosition;

        do {
            lastDigitPosition = random.nextInt(4) + 4;
        } while (lastDigitPosition == lastSpeicalPosition);

        //generate first three characters of the password
        char firstLetter = charList[random.nextInt(charList.length)];
        usedCharList.add(String.valueOf(firstLetter));
        pass[0] = firstLetter;
        fillPassArray(pass, usedCharList, usedNumbers, usedSpecials, random, 1, 4, firstSpecialPosition, firstDigitPosition);
        fillPassArray(pass, usedCharList, usedNumbers, usedSpecials, random, 4, 8, lastSpeicalPosition, lastDigitPosition);

        Random rand = new Random();
        int capitalLetterPos = rand.nextInt(21) + 21;
        char c = charList[capitalLetterPos];
        char finalPass [] = new char[pass.length+1];
        System.arraycopy(pass,0,finalPass,0,pass.length);
        finalPass[finalPass.length-1] = c;
        log.info("Pass: " + String.valueOf(finalPass));

        return finalPass;
    }

    public String encodePassword(char[] password) {
        return hashPassword(password);
    }

    private String hashPassword(char[] password) {
        MessageDigest md = null;
        String hashedPassword = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
            String passwordFromCharArray = String.copyValueOf(password);
            md.update(passwordFromCharArray.getBytes("UTF-8"));
            hashedPassword = Base64.encodeBase64String(md.digest());
            passwordFromCharArray = null;
        } catch (NoSuchAlgorithmException ex) {
            log.error("Cannot find algorithm: " + ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            log.error("Encoding not supported: " + ex.getMessage());
        }
        if (hashedPassword != null) {
            log.debug("Password hashed successfully");
        }
        return hashedPassword;
    }

    private void fillPassArray(char[] pass, List<String> usedCharList, List<Integer> usedNumbers,
                               List<Character> usedSpecials, Random random, int start, int end, int specialPosition, int digitPosition) {
        for (int i = start; i < end; i++) {
            if (i == specialPosition) {
                char special;

                do {
                    special = specialChars[random.nextInt(specialChars.length)];
                } while (usedSpecials.contains(special));

                usedSpecials.add(special);
                pass[i] = special;
            } else if (i == digitPosition) {
                int firstDigit;
                do {
                    firstDigit = random.nextInt(8) + 2;
                } while (usedNumbers.contains(firstDigit));

                usedNumbers.add(firstDigit);
                pass[i] = Integer.toString(firstDigit).charAt(0);
            } else {
                char c;

                do {
                    c = charList[random.nextInt(charList.length)];
                } while (usedCharList.contains(String.valueOf(c).toLowerCase()));

                usedCharList.add(String.valueOf(c).toLowerCase());
                pass[i] = c;
            }
        }




    }

    public boolean validate(String password) {
        boolean hasSpecialChar = false;
        boolean hasDigit = false;

        for (int i = 0; i < specialChars.length; i++)
            if (password.contains(String.valueOf(specialChars[i]))) {
                hasSpecialChar = true;
                break;
            }

        char[] nums = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        for (int i = 0; i < nums.length; i++) {
            if (password.contains(String.valueOf(nums[i]))) {
                hasDigit = true;
                break;
            }
        }

        if (hasSpecialChar && hasDigit)
            return true;

        return false;
    }
}
