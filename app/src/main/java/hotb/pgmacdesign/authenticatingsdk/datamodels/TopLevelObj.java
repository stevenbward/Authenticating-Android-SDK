package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pmacdowell on 2018-05-07.
 */

public class TopLevelObj {
    @SerializedName("successful")
    private Boolean successful;

    public Boolean getSuccessful() {
        if(successful == null){
            this.successful = false;
        }
        return successful;
    }

    public void setSuccessful(Boolean successful) {
        this.successful = successful;
    }
}
