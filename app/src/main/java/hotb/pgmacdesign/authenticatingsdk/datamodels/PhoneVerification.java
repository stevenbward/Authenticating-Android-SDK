package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import hotb.pgmacdesign.authenticatingsdk.networking.AuthenticatingAPICalls;

/**
 * This class is used for phone verification
 * {@link AuthenticatingAPICalls}
 * Created by pmacdowell on 2017-07-24.
 */

public class PhoneVerification {
    @SerializedName("accessCode")
    private String accessCode;
    @SerializedName("smsCode")
    private String smsCode;

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }
}
