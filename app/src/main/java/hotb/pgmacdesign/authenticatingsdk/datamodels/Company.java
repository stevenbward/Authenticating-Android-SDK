package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Company header class. This 'header' structure allows easier error parsing.
 * Created by pmacdowell on 2017-07-24.
 */
public class Company extends TopLevelObj {

    @SerializedName("companyId")
    private String companyId;
    @SerializedName("companyName")
    private String companyName;
    @SerializedName("contactName")
    private String contactName;
    @SerializedName("contactEmail")
    private String contactEmail;
    @SerializedName("daysToTakeTest")
    private Float daysToTakeTest;
    @SerializedName("networks")
    private List<String> allowedNetworks;

    public String getCompanyId() {
        return companyId;
    }

    public List<String> getAllowedNetworks() {
        return allowedNetworks;
    }

    public void setAllowedNetworks(List<String> allowedNetworks) {
        this.allowedNetworks = allowedNetworks;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Float getDaysToTakeTest() {
        return daysToTakeTest;
    }

    public void setDaysToTakeTest(Float daysToTakeTest) {
        this.daysToTakeTest = daysToTakeTest;
    }
}
