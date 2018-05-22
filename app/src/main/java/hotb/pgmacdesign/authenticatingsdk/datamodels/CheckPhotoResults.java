package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pmacdowell on 2017-12-11.
 */

public class CheckPhotoResults extends TopLevelObj {

    /**
     * The result String will be one of the following:
     * "pending_results", "complete", "id_expired", "parsing_failed", "unknown_error"
     */
    @SerializedName("result")
    private String result;
    @SerializedName("numAttemptsLeft")
    private Integer numAttemptsLeft;
    @SerializedName("description")
    private String description;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getNumAttemptsLeft() {
        return numAttemptsLeft;
    }

    public void setNumAttemptsLeft(Integer numAttemptsLeft) {
        this.numAttemptsLeft = numAttemptsLeft;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
