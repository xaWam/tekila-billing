package spring.dto;

public class Utils {
    public static String merge(String... values) {
        String result = "";
        if (values != null) {
            boolean first = true;
            for (String value : values) {
                if (value != null) {
                    if (!first) {
                        result += ", ";
                    }
                    result += value;
                    first = false;
                }
            }
        }
        return result;
    }
}
