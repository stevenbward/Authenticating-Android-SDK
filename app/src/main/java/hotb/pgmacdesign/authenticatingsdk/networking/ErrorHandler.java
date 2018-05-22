package hotb.pgmacdesign.authenticatingsdk.networking;

import com.google.gson.Gson;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AuthenticatingException;

/**
 * Created by pmacdowell on 2017-07-25.
 */

class ErrorHandler {

    static void checkForAuthenticatingError(String responseBodyString) throws AuthenticatingException {
        ErrorParsingObj errorParsingObj = parseMessageToError(responseBodyString);
        if(errorParsingObj != null){
            if(!errorParsingObj.getSuccessful()){
                AuthenticatingException authE = new AuthenticatingException();
                authE.setAuthErrorString(errorParsingObj.getErrorMessage());
                authE.setAuthErrorStringDetails(errorParsingObj.getErrorMessage());
                throw authE;
            }
        }
        //If nothing else, means it is not an error
    }

    static void checkForAuthenticatingErrorObject(Object responseBody) throws AuthenticatingException {
        String str = null;
        try {
            str = new Gson().toJson(responseBody);
        } catch (Exception e){}
        ErrorParsingObj errorParsingObj = parseMessageToError(str);
        if(errorParsingObj != null){
            if(!errorParsingObj.getSuccessful()){
                AuthenticatingException authE = new AuthenticatingException();
                authE.setAuthErrorString(errorParsingObj.getErrorMessage());
                authE.setAuthErrorStringDetails(errorParsingObj.getErrorMessage());
                throw authE;
            }
        }
        //If nothing else, means it is not an error
    }

    /**
     * Parse the error message and convert to an ErrorParsingObj
     * @param responseString
     * @return
     */
    private static ErrorParsingObj parseMessageToError(String responseString){
        if(StringUtilities.isNullOrEmpty(responseString)){
            return null;
        }
        try {
            return (new Gson().fromJson(responseString, ErrorParsingObj.class));
        } catch (Exception e1){}
        return null;
    }
}
