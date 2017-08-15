package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Company header class. This 'header' structure allows easier error parsing.
 * Created by pmacdowell on 2017-07-24.
 */
public class CompanyHeader extends TopLevelObj {

    @SerializedName("data")
    private Company company;

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public static class Company {

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
}
