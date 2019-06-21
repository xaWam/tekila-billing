package spring.model;
/**
 * @author gurbanaz
 * @date 08.04.2019 / 13:52
 */

public enum RecordType {
    A("A"), M("M"), D("D");

    private String code;

    RecordType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RecordType convertFromCode(String code){
        for(RecordType type : values()){
            if(type.getCode().equals(code)){
                return type;
            }
        }
        return null;
    }
}
