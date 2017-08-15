package hotb.pgmacdesign.authenticatingsdk.networking;

import com.google.gson.Gson;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AuthenticatingException;

import okhttp3.ResponseBody;

/**
 * Created by pmacdowell on 2017-07-25.
 */

class ErrorHandler {

    static void checkForAuthenticatingError(retrofit2.Response response) throws AuthenticatingException {
        ErrorParsingObj errorParsingObj = parseMessageToError(response);
        if(errorParsingObj != null){
            AuthenticatingException authE = new AuthenticatingException();
            ErrorParsingObj.ErrorInfo errorInfo = errorParsingObj.getErrorInfo();
            if(errorInfo != null){
                authE.setAuthErrorString(errorInfo.getErrorMessage());
                authE.setAuthErrorStringDetails("");
                throw authE;
            }
        }
        //If nothing else, means it is not an error
    }

    /**
     * Parse the error message and convert to an ErrorParsingObj
     * @param response
     * @return
     */
    private static ErrorParsingObj parseMessageToError(retrofit2.Response response){
        try {
            ResponseBody rb = response.errorBody();
            if(rb != null) {
                try {
                    String str = rb.string();
                    return (new Gson().fromJson(str, ErrorParsingObj.class));
                } catch (Exception e1){}
            }

            try {
                ErrorParsingObj errorParsingObj = (ErrorParsingObj) response.body();
                return (errorParsingObj);
            } catch (Exception e1){}

            try {
                String str = response.body().toString();
                ErrorParsingObj errorParsingObj = new Gson().fromJson(str,
                        ErrorParsingObj.class);
                return errorParsingObj;
            } catch (Exception e1){}

        } catch (Exception e){}
        return null;
    }
}
