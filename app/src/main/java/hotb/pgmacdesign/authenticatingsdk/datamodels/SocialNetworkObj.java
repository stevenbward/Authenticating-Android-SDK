package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import hotb.pgmacdesign.authenticatingsdk.networking.AuthenticatingAPICalls;

/**
 * This class is used to verify social networks.
 * {@link AuthenticatingAPICalls}
 * Created by pmacdowell on 2017-07-24.
 */
public class SocialNetworkObj {

    @SerializedName("accessCode")
    private String accessCode;
    @SerializedName("network")
    private String network;
    @SerializedName("socialMediaAccessToken")
    private String socialMediaAccessToken;
    @SerializedName("socialMediaUserId")
    private String socialMediaUserId;

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getSocialMediaAccessToken() {
        return socialMediaAccessToken;
    }

    public void setSocialMediaAccessToken(String socialMediaAccessToken) {
        this.socialMediaAccessToken = socialMediaAccessToken;
    }

    public String getSocialMediaUserId() {
        return socialMediaUserId;
    }

    public void setSocialMediaUserId(String socialMediaUserId) {
        this.socialMediaUserId = socialMediaUserId;
    }
}
