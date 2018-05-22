package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pmacdowell on 2017-07-25.
 */

public class SimpleResponse  extends TopLevelObj {

    @SerializedName("resultMessage")
    private String resultMessage;

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
