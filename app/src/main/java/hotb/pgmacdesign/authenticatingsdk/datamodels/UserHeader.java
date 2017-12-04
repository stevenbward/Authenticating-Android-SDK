package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * User header class. This 'header' structure allows easier error parsing.
 * Created by pmacdowell on 2017-07-13.
 */
public class UserHeader extends TopLevelObj {

    @SerializedName("data")
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * The actual User object. This is obtained from the {@link UserHeader#getUser()} call
     */
    public static class User {
        private String email;
        @SerializedName("phone")
        private String phone;
        @SerializedName("companyId")
        private String companyId;
        @SerializedName("userId")
        private String userId;
        @SerializedName("accessCode")
        private String accessCode;
        @SerializedName("accessKeyExpirationDate")
        private String accessKeyExpirationDate;
        @SerializedName("firstName")
        private String firstName;
        @SerializedName("lastName")
        private String lastName;
        @SerializedName("country")
        private String country;
        @SerializedName("year")
        private Integer year;
        @SerializedName("month")
        private Integer month;
        @SerializedName("day")
        private Integer day;
        @SerializedName("address")
        private String address;
        @SerializedName("city")
        private String city;
        @SerializedName("state")
        private String state;
        @SerializedName("zipcode")
        private String zipcode;
        @SerializedName("ssn")
        private String ssn;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getSsn() {
            return ssn;
        }

        public void setSsn(String ssn) {
            this.ssn = ssn;
        }

        public Integer getYear() {
            if(year == null){
                year = 0;
            }
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public Integer getMonth() {
            if(month == null){
                month = 0;
            }
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public Integer getDay() {
            if(day == null){
                day = 0;
            }
            return day;
        }

        public void setDay(Integer day) {
            this.day = day;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getZipcode() {
            return zipcode;
        }

        public void setZipcode(String zipcode) {
            this.zipcode = zipcode;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCompanyId() {
            return companyId;
        }

        public void setCompanyId(String companyId) {
            this.companyId = companyId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getAccessCode() {
            return accessCode;
        }

        public void setAccessCode(String accessCode) {
            this.accessCode = accessCode;
        }

        public String getAccessKeyExpirationDate() {
            return accessKeyExpirationDate;
        }

        public void setAccessKeyExpirationDate(String accessKeyExpirationDate) {
            this.accessKeyExpirationDate = accessKeyExpirationDate;
        }
    }

}
