package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

/**
 * Created by pmacdowell on 2017-07-25.
 */

public class SimpleResponseObj {

    @SerializedName("data")
    private SimpleResponse simpleResponse;

    public SimpleResponse getSimpleResponse() {
        return simpleResponse;
    }

    public void setSimpleResponse(SimpleResponse simpleResponse) {
        this.simpleResponse = simpleResponse;
    }

    public static class SimpleResponse {
        @SerializedName("success")
        private Boolean success;
        @SerializedName("resultMessage")
        private String resultMessage;

        public Boolean getSuccess() {
            if(success == null){
                success = false;
            }
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getResultMessage() {
            return resultMessage;
        }

        public void setResultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
        }
    }
}
