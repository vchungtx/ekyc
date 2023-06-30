package online.chungnv.ekyc.demo.bean;

import java.util.Date;

public class EkycResponse {
    String errorCode;
    String errorDesc;
    PersonInfomation personInfomation;
    float similarity;
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public PersonInfomation getPersonInfomation() {
        return personInfomation;
    }

    public void setPersonInfomation(PersonInfomation personInfomation) {
        this.personInfomation = personInfomation;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }
}
