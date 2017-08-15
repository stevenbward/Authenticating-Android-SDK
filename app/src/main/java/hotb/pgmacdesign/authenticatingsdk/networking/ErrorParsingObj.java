package hotb.pgmacdesign.authenticatingsdk.networking;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import hotb.pgmacdesign.authenticatingsdk.datamodels.TopLevelObj;

/**
 * Error information class. This 'header' structure allows easier error parsing.
 * Created by pmacdowell on 2017-07-13.
 */
class ErrorParsingObj extends TopLevelObj {

    @SerializedName("error")
    private ErrorInfo errorInfo;

    protected ErrorInfo getErrorInfo() {
        return errorInfo;
    }

    protected void setErrorInfo(ErrorInfo errorInfo) {
        this.errorInfo = errorInfo;
    }

    static class ErrorInfo {
        @SerializedName("missingInfo")
        private List<String>missingInfo;
        @SerializedName("errorMessage")
        private String errorMessage;

        protected List<String> getMissingInfo() {
            return missingInfo;
        }

        protected void setMissingInfo(List<String> missingInfo) {
            this.missingInfo = missingInfo;
        }

        protected String getErrorMessage() {
            return errorMessage;
        }

        protected void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

}
