package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Top Level class. This 'header' structure allows easier error parsing.
 * Created by pmacdowell on 2017-07-24.
 */
public class TopLevelObj {
    @SerializedName("code")
    private Integer code;
    @SerializedName("hasError")
    private Boolean hasError;

    public Integer getCode() {
        if(code == null){
            code = 400;
        }
        return code;
    }

    public Boolean getHasError() {
        if(hasError == null){
            hasError = true;
        }
        return hasError;
    }

    public void setHasError(Boolean hasError) {
        this.hasError = hasError;
    }
}
