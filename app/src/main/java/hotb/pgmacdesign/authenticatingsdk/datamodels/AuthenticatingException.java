package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pmacdowell on 2017-07-25.
 */

public class AuthenticatingException extends Exception {

    @SerializedName("authErrorString")
    private String authErrorString;
    @SerializedName("authErrorStringDetails")
    private String authErrorStringDetails;

    public String getAuthErrorString() {
        return authErrorString;
    }

    public void setAuthErrorString(String authErrorString) {
        this.authErrorString = authErrorString;
    }

    public String getAuthErrorStringDetails() {
        return authErrorStringDetails;
    }

    public void setAuthErrorStringDetails(String authErrorStringDetails) {
        this.authErrorStringDetails = authErrorStringDetails;
    }
}
