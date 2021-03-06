package hotb.pgmacdesign.authenticatingsdk.networking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.widget.ProgressBar;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AuthenticatingException;
import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworks;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResults;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObject;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponse;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UploadPhotosObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.User;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;
import hotb.pgmacdesign.authenticatingsdk.interfaces.OnTaskCompleteListener;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static hotb.pgmacdesign.authenticatingsdk.networking.StringUtilities.isNullOrEmpty;
import static hotb.pgmacdesign.authenticatingsdk.networking.StringUtilities.keepNumbersOnly;

/**
 * Class that houses API calls. These can be used or not if you want to define your own web
 * calls using the interface {@link APIService}
 * Link to web documentation: https://docs.authenticating.com
 * Created by pmacdowell on 2017-07-13.
 */
public class AuthenticatingAPICalls {

    private static final String NO_INTERNET = "No network connection, please check your internet connectivity and try again";
    private static final String UNAUTHORIZED_REQUEST = "Unauthorized request";
    private static final String GENERIC_ERROR_STRING = "An unknown error has occurred";
    private static final String SENT_IMAGE_BAD = "One or more of the passed image URIs was either null or was unable to be parsed";
    private static final String MUST_INCLUDE_ACCESS_CODE = "You must include the AccessCode in this call";
    private static final String MISSING_AUTH_KEY = "You did not include your authKey. This is obtained when you register for an account. Calls will not function without this key";
    private static final String PARSING_CONVERSION_ERROR = "Could not convert server response data. Please enabling logging to see full request and response logs.";
    private static final String BASE_64_ENCODED_STRING_REGEX = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
    private static final String URL_BASE = AuthenticatingConstants.BASE_URL;

    private static APIService myService;
    private static AsyncTask<Void, Void, Void> uploadPhotosAsynctask;

    private static enum UploadIdTypes {
        uploadId, uploadIdEnhanced, comparePhotos, uploadPassport
    }

    public static APIService getMyService() {
        init();
        return myService;
    }

    static {
        init();
    }

    private static void init() {
        boolean shouldResetService = false;
        if (WebCallsLogging.didUserChangeLogging()) {
            shouldResetService = true;
            WebCallsLogging.USER_CHANGED_LOGGING = false;
        }
        if (myService == null) {
            shouldResetService = true;
        }
        if (shouldResetService) {
//            RetrofitClient.Builder builder = new RetrofitClient.Builder(
//                    APIService.class, AuthenticatingConstants.STAGING_URL);
            RetrofitClient.Builder builder = new RetrofitClient.Builder(
                  APIService.class, URL_BASE);
            if (WebCallsLogging.isJsonLogging()) {
                Logging.m("Updating Logging to: True");
                builder.setLogLevel(HttpLoggingInterceptor.Level.BODY);
            } else {
                Logging.m("Updating Logging to: False");
                builder.setLogLevel(HttpLoggingInterceptor.Level.NONE);
            }
            builder.setCustomConverterFactory(new CustomConverterFactory());
            builder.setTimeouts(30000, 30000);

            try {
                myService = builder.build().buildServiceClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    ///////////////////////////////////////////////////////////////
    //Synchronous Calls - Require Async Thread && Error Handling //
    ///////////////////////////////////////////////////////////////

    /**
     * Get the networks available to you for the network verification test
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link AvailableNetworks}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object getAvailableNetworks(String companyAPIKey,
                                                         String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getAvailableNetworks(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}

            toReturn = response.body();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Verify your social network. This should only be called after the user has successfully
     * logged into one of the available social OAuth platforms (IE: facebook) where you have obtained
     * their accessToken and id.
     *
     * @param companyAPIKey          The company api key provided by Authenticating
     * @param accessCode             The identifier String given to a user. Obtained when creating the user
     * @param network                String network, in lowercase, of the network they logged in on. Samples are:
     *                               faecbook, google, twitter, instagram
     * @param socialMediaAccessToken The access token you received from the social media login
     * @param socialMediaUserId      The user id you received from the social media login
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object verifySocialNetworks(String companyAPIKey,
                                                      String accessCode, String network,
                                                      String socialMediaAccessToken,
                                                      String socialMediaUserId) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        SocialNetworkObj s = new SocialNetworkObj();
        s.setAccessCode(accessCode);
        s.setNetwork(network);
        s.setSocialMediaAccessToken(socialMediaAccessToken);
        s.setSocialMediaUserId(socialMediaUserId);
        Call<ResponseBody> call = myService.verifySocialNetworks(companyAPIKey, s);
        AuthenticatingAPICalls.printOutRequestJson(s, AuthenticatingConstants.TYPE_SOCIAL_NETWORK_OBJ, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * This endpoint will send a text / SMS to the user with a code for them to enter in
     * the {@link AuthenticatingAPICalls#verifyPhoneCode(OnTaskCompleteListener, String, String, String)}
     * endpoint.
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object verifyPhone(String companyAPIKey,
                                             String accessCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.verifyPhone(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Used after receiving an SMS and including it here as the code received to finish
     * the test and pass the SMS portion.
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param smsCode       The code received in the user's SMS to be sent outbound.
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object verifyPhoneCode(String companyAPIKey,
                                                 String accessCode, String smsCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        p.setSmsCode(smsCode);
        Call<ResponseBody> call = myService.verifyPhoneCode(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Verify an email
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object verifyEmail(String companyAPIKey,
                                             String accessCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        User user = new User();
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.verifyEmail(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedImage1 First Photo File already converted to base64 encoded String
     * @param base64EncodedImage2  Second Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object comparePhotos(String companyAPIKey, String accessCode,
                                               String base64EncodedImage1,
                                               String base64EncodedImage2) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, base64EncodedImage1,
                base64EncodedImage2, UploadIdTypes.comparePhotos);
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param photo1Bitmap  First Photo File to parse.
     * @param photo2Bitmap  Second Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object comparePhotos(String companyAPIKey, String accessCode,
                                               Bitmap photo1Bitmap, Bitmap photo2Bitmap) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, photo1Bitmap, 
                photo2Bitmap, UploadIdTypes.comparePhotos);
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadId(OnTaskCompleteListener, String, String, String, String)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @param base64EncodeIdBack  Second Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadId(String companyAPIKey, String accessCode,
                                          String base64EncodedIdFront,
                                          String base64EncodeIdBack) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, base64EncodedIdFront,
                base64EncodeIdBack, UploadIdTypes.uploadId);
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadId(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @param idBackBitmap  Second Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadId(String companyAPIKey, String accessCode,
                                          Bitmap idFrontBitmap, Bitmap idBackBitmap) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, idFrontBitmap,
                idBackBitmap, UploadIdTypes.uploadId);
    }

    /**
     * Upload a picture of a passport for the verification process. Note that only the front (The
     * portion with the data, usually on the first or second page) is required. 
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadPassport(OnTaskCompleteListener, String, String, String)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadPassport(String companyAPIKey, String accessCode,
                                                String base64EncodedIdFront) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, base64EncodedIdFront,
                null, UploadIdTypes.uploadPassport);
    }

    /**
     * Upload a picture of a passport for the verification process. Note that only the front (The
     * portion with the data, usually on the first or second page) is required. 
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadPassport(OnTaskCompleteListener, String, String, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadPassport(String companyAPIKey, String accessCode,
                                                Bitmap idFrontBitmap) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, idFrontBitmap,
                null, UploadIdTypes.uploadPassport);
    }
    
    /**
     * Upload 2 photos to the endpoint for uploadIdEnhanced and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadIdEnhanced(OnTaskCompleteListener, String, String, String, String)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @param base64EncodeIdBack  Second Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadIdEnhanced(String companyAPIKey, String accessCode,
                                                  String base64EncodedIdFront,
                                                  String base64EncodeIdBack) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, base64EncodedIdFront,
                base64EncodeIdBack, UploadIdTypes.uploadIdEnhanced);
    }
    
    /**
     * Upload 2 photos to the endpoint for uploadIdEnhanced and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadIdEnhanced(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @param idBackBitmap  Second Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object uploadIdEnhanced(String companyAPIKey, String accessCode,
                                                  Bitmap idFrontBitmap, Bitmap idBackBitmap) throws AuthenticatingException {

        return uploadIdEndpointsJoiner(companyAPIKey, accessCode, idFrontBitmap,
                idBackBitmap, UploadIdTypes.uploadIdEnhanced);
    }
    
    private static Object uploadIdEndpointsJoiner(final String companyAPIKey, final String accessCode,
                                                          String base64EncodedIdFront, String base64EncodeIdBack,
                                                          final UploadIdTypes type) throws AuthenticatingException{
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if (StringUtilities.isNullOrEmpty(base64EncodedIdFront) ||
                StringUtilities.isNullOrEmpty(base64EncodeIdBack)) {
            return null;
        }

        Call<ResponseBody> call;
        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        switch (type){

            case uploadIdEnhanced:
                uploadPhotosObj.setIdFront(base64EncodedIdFront);
                uploadPhotosObj.setIdBack(base64EncodeIdBack);
                call = myService.uploadIdEnhanced(companyAPIKey, uploadPhotosObj);
                break;

            case uploadId:
                uploadPhotosObj.setIdFront(base64EncodedIdFront);
                uploadPhotosObj.setIdBack(base64EncodeIdBack);
                call = myService.uploadId(companyAPIKey, uploadPhotosObj);
                break;

            default:
            case comparePhotos:
                uploadPhotosObj.setImg1(base64EncodedIdFront);
                uploadPhotosObj.setImg2(base64EncodeIdBack);
                call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
                break;
        }
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    private static Object uploadIdEndpointsJoiner(final String companyAPIKey, final String accessCode,
                                                          Bitmap idFrontBitmap, Bitmap idBackBitmap,
                                                          final UploadIdTypes type) throws AuthenticatingException{
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if(type == UploadIdTypes.uploadPassport){
            if (idFrontBitmap == null) {
                return null;
            }
        } else {
            if (idFrontBitmap == null || idBackBitmap == null) {
                return null;
            }
        }

        if(type == UploadIdTypes.uploadPassport){
            if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0) {
                return null;
            }
        } else {
            if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0 ||
                    idBackBitmap.getRowBytes() <= 0 || idBackBitmap.getHeight() <= 0) {
                return null;
            }
        }

        if(type == UploadIdTypes.uploadPassport){
            if (isBitmapTooLarge(idFrontBitmap)) {
                try {
                    idFrontBitmap = AuthenticatingAPICalls.resizePhoto(idFrontBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (isBitmapTooLarge(idFrontBitmap)) {
                try {
                    idFrontBitmap = AuthenticatingAPICalls.resizePhoto(idFrontBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (isBitmapTooLarge(idBackBitmap)) {
                try {
                    idBackBitmap = AuthenticatingAPICalls.resizePhoto(idBackBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        String idFront = null, idBack = null;
        try {
            if(type == UploadIdTypes.uploadPassport){
                idFront = encodeImage(idFrontBitmap);
            } else {
                idFront = encodeImage(idFrontBitmap);
                idBack = encodeImage(idBackBitmap);   
            }
        } catch (Exception e) {
            return null;
        }

        Call<ResponseBody> call;

        switch (type){
            case uploadPassport:
                uploadPhotosObj.setIdFront(idFront);
                call = myService.uploadPassport(companyAPIKey, uploadPhotosObj);
                break;
                
            case uploadId:
                uploadPhotosObj.setIdFront(idFront);
                uploadPhotosObj.setIdBack(idBack);
                call = myService.uploadId(companyAPIKey, uploadPhotosObj);

                break;
            case uploadIdEnhanced:
                uploadPhotosObj.setIdFront(idFront);
                uploadPhotosObj.setIdBack(idBack);
                call = myService.uploadIdEnhanced(companyAPIKey, uploadPhotosObj);
                break;

            case comparePhotos:
            default:
                uploadPhotosObj.setImg1(idFront);
                uploadPhotosObj.setImg2(idBack);
                call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
                break;
        }

        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Check the current status of the asynchronous image processing on the server
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link CheckPhotoResults}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object checkUploadId(String companyAPIKey,
                                                  String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.checkUploadId(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn =  object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Check the current status of the asynchronous image processing on the server
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link CheckPhotoResults}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object checkUploadPassport(String companyAPIKey,
                                                        String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.checkUploadPassport(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Get the quiz for the user to complete the verification proof test
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link QuizObject}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object getQuiz(String companyAPIKey,
                                     String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getQuiz(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Verify the quiz by passing in the answers for the questions sent. The other information
     * listed here can be found in the original returned quiz object.
     *
     * @param companyAPIKey    The company api key provided by Authenticating
     * @param accessCode       The identifier String given to a user. Obtained when creating the user
     * @param answers          Array of answer objects that contain the responses to the questions
     *                         that were obtained from the
     *                         {@link AuthenticatingAPICalls#getQuiz(OnTaskCompleteListener, String, String)} endpoint.
     * @param quizId           The quiz id. This is obtained from the {@link QuizObject} obtained from getQuiz()
     * @param transactionId    The quiz transaction id.
     *                         This is obtained from the {@link QuizObject} obtained from getQuiz()
     * @param responseUniqueId The quiz response unique id.
     *                         This is obtained from the {@link QuizObject} obtained from getQuiz()
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object verifyQuiz(String companyAPIKey,
                                            String accessCode, VerifyQuizObj.Answer[] answers,
                                            String quizId, String transactionId,
                                            String responseUniqueId) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        VerifyQuizObj v = new VerifyQuizObj();
        v.setAccessCode(accessCode);
        v.setAnswers(answers);
        v.setQuizId(quizId);
        v.setTransactionID(transactionId);
        v.setResponseUniqueId(responseUniqueId);

        Call<ResponseBody> call = myService.verifyQuiz(companyAPIKey, v);
        AuthenticatingAPICalls.printOutRequestJson(v, AuthenticatingConstants.TYPE_VERIFY_QUIZ_OBJ, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Generate a background report for a user. Keep in mind that a user must have
     * completed their identity quiz before attempting this else it will throw
     * an error upon calling.
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link SimpleResponse}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object generateBackgroundReport(String companyAPIKey,
                                                          String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User user = new User();
        user.setAccessCode(accessCode);

        Call<ResponseBody> call = myService.generateCriminalReport(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }


    /**
     * Get the user
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link User}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object getUser(String companyAPIKey,
                               String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User user = new User();
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Update User (Overloaded to allow a user object to be passed in)
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param user          {@link User}
     * @return {@link User}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object updateUser(@NonNull String companyAPIKey, @NonNull String accessCode,
                                  @NonNull User user) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.updateUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        Object toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
			Object object = response.body();
            ResponseBody errorBody = response.errorBody();
            try {
                ErrorHandler.checkForAuthenticatingErrorObject(object);
                ErrorHandler.checkForAuthenticatingError(errorBody.string());
            } catch (NullPointerException nope){}
            toReturn = object;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Update a user
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param firstName     First name of user (IE: John)
     * @param lastName      Last name of User (IE: Smith)
     * @param birthYear     Birth year of user (IE: 1985 or 2001)
     * @param birthMonth    Birth month of the user (IE: 12 or 4)
     * @param birthDay      Birth Day of the user (IE: 1 or 31)
     * @param address       Address of User (IE: 123 Fake St)
     * @param city          City of the User (IE: Los Angeles)
     * @param state         State Abbreviation of User (IE: CA or NY)
     * @param zipcode       5 digit zip code / postal code of user (IE: 90210 or 20500)
     * @param street        street, as used by Canadian users. (IE: Jones Ave)
     * @param province      province, as used by Canadian users. (IE: ON)
     * @param buildingNumber buildingNumber, as used by Canadian users. (IE: 137)
     * @param email         Email (IE, email@email.com)
     * @param phoneNumber   Phone number, numbers only (IE: 2138675309)
     * @param ssn           Social Security Number, 9 digits (IE: 123456789)
     * @return {@link User}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static Object updateUser(@NonNull String companyAPIKey,
                                  @NonNull String accessCode, @Nullable String firstName,
                                  @Nullable String lastName, @Nullable Integer birthYear, @Nullable Integer birthMonth,
                                  @Nullable Integer birthDay, @Nullable String address, @Nullable String city,
                                  @Nullable String state, @Nullable String zipcode, @Nullable String street,
                                  @Nullable String province, @Nullable String buildingNumber, @Nullable String email,
                                  @Nullable String phoneNumber, @Nullable String ssn) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        User user = new User();
        user.setAccessCode(accessCode);
        if (!isNullOrEmpty(firstName))
            user.setFirstName(firstName);
        if (!isNullOrEmpty(lastName))
            user.setLastName(lastName);
        if (birthYear != null)
            user.setYear(birthYear);
        if (birthMonth != null)
            user.setMonth(birthMonth);
        if (birthDay != null)
            user.setDay(birthDay);
        if(!StringUtilities.isNullOrEmpty(province))
            user.setProvince(province);
        if(!StringUtilities.isNullOrEmpty(buildingNumber))
            user.setBuildingNumber(buildingNumber);
        if(!StringUtilities.isNullOrEmpty(street))
            user.setStreet(street);
        if (!isNullOrEmpty(address))
            user.setAddress(address);
        if (!isNullOrEmpty(city))
            user.setCity(city);
        if (!isNullOrEmpty(state))
            user.setState(state);
        if (!isNullOrEmpty(zipcode))
            user.setZipcode(zipcode);
        if (!isNullOrEmpty(ssn))
            user.setSsn(ssn);
        if (!isNullOrEmpty(email))
            user.setEmail(email);
        if (!isNullOrEmpty(keepNumbersOnly(phoneNumber)))
            user.setPhone(keepNumbersOnly(phoneNumber));
        return updateUser(companyAPIKey, accessCode, user);
    }

    ///////////////////////////////////////////////////////////////
    //Asynchronous Calls - Do Not require Async or Error Handling//
    ///////////////////////////////////////////////////////////////

    /*
    All Asynchronous calls are identical to their counterparts above other than they take
    in OnTaskCompleteListener {@link OnTaskCompleteListener} on which the data is sent back
    upon, it handles the Errors {@link AuthenticatingException} and parses it specifically, and it
    runs it asynchronously, off the main thread, so that the dev does not need to worry
    about running an AsyncTask / Thread.
    */

    /**
     * Get the networks available to you for the network verification test
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void getAvailableNetworks(@NonNull final OnTaskCompleteListener listener,
                                            String companyAPIKey,
                                            String accessCode) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getAvailableNetworks(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    AvailableNetworks myObjectToReturn = (AvailableNetworks) RetrofitParser.convert(responseBodyString, AvailableNetworks.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_AVAILABLE_NETWORKS);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_AVAILABLE_NETWORKS);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Verify your social network. This should only be called after the user has successfully
     * logged into one of the available social OAuth platforms (IE: facebook) where you have obtained
     * their accessToken and id.
     *
     * @param listener               {@link OnTaskCompleteListener}
     * @param companyAPIKey          The company api key provided by Authenticating
     * @param accessCode             The identifier String given to a user. Obtained when creating the user
     * @param network                String network, in lowercase, of the network they logged in on. Samples are:
     *                               faecbook, google, twitter, instagram
     * @param socialMediaAccessToken The access token you received from the social media login
     * @param socialMediaUserId      The user id you received from the social media login
     */
    public static void verifySocialNetworks(@NonNull final OnTaskCompleteListener listener,
                                            String companyAPIKey,
                                            String accessCode, String network,
                                            String socialMediaAccessToken,
                                            String socialMediaUserId) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        SocialNetworkObj s = new SocialNetworkObj();
        s.setAccessCode(accessCode);
        s.setNetwork(network);
        s.setSocialMediaAccessToken(socialMediaAccessToken);
        s.setSocialMediaUserId(socialMediaUserId);
        Call<ResponseBody> call = myService.verifySocialNetworks(companyAPIKey, s);
        AuthenticatingAPICalls.printOutRequestJson(s, AuthenticatingConstants.TYPE_SOCIAL_NETWORK_OBJ, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }
                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * This endpoint will send a text / SMS to the user with a code for them to enter in
     * the {@link AuthenticatingAPICalls#verifyPhoneCode(OnTaskCompleteListener, String, String, String)}
     * endpoint.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void verifyPhone(@NonNull final OnTaskCompleteListener listener,
                                   String companyAPIKey,
                                   String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.verifyPhone(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Used after receiving an SMS and including it here as the code received to finish
     * the test and pass the SMS portion.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param smsCode       The code received in the user's SMS to be sent outbound.
     */
    public static void verifyPhoneCode(@NonNull final OnTaskCompleteListener listener,
                                       String companyAPIKey,
                                       String accessCode, String smsCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        p.setSmsCode(smsCode);
        Call<ResponseBody> call = myService.verifyPhoneCode(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Verify a user's email
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void verifyEmail(@NonNull final OnTaskCompleteListener listener,
                                   String companyAPIKey,
                                   String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        User user = new User();
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.verifyEmail(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }


    /**
     * Upload 2 photos to the endpoint for Photo proof. This method will run all bitmap conversion
     * and base64 string encoding on a thread and not impact the main UI thread.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param photo1Bitmap  First Photo File to parse.
     * @param photo2Bitmap  Second Photo File to parse.
     */
    public static void comparePhotos(@NonNull final OnTaskCompleteListener listener,
                                     final String companyAPIKey, final String accessCode,
                                     final Bitmap photo1Bitmap, final Bitmap photo2Bitmap) {
        AuthenticatingAPICalls.uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode,
                photo1Bitmap, photo2Bitmap, UploadIdTypes.comparePhotos);
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param listener            {@link OnTaskCompleteListener}
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedImage1 First Photo File already converted to base64 encoded String
     * @param base64EncodedImage2  Second Photo File already converted to base64 encoded String
     */
    public static void comparePhotos(@NonNull final OnTaskCompleteListener listener,
                                     String companyAPIKey, String accessCode,
                                     String base64EncodedImage1, String base64EncodedImage2) {
        AuthenticatingAPICalls.uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode,
                base64EncodedImage1, base64EncodedImage2, UploadIdTypes.comparePhotos);
    }

    /**
     * Upload 2 photos to the endpoint for uploadIdEnhanced and identify verification.
     * This method will run all bitmap conversion and base64 string encoding on a thread and
     * not impact the main UI thread.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @param idBackBitmap  Second Photo File to parse.
     */
    public static void uploadIdEnhanced(@NonNull final OnTaskCompleteListener listener,
                                final String companyAPIKey, final String accessCode,
                                final Bitmap idFrontBitmap, final Bitmap idBackBitmap) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, idFrontBitmap,
                idBackBitmap, UploadIdTypes.uploadIdEnhanced);
    }

    /**
     * Upload 2 photos to the endpoint for uploadIdEnhanced and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadIdEnhanced(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param listener            {@link OnTaskCompleteListener}
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @param base64EncodedIdBack  Second Photo File already converted to base64 encoded String
     */
    public static void uploadIdEnhanced(@NonNull final OnTaskCompleteListener listener,
                                String companyAPIKey, String accessCode,
                                String base64EncodedIdFront, String base64EncodedIdBack) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, base64EncodedIdFront,
                base64EncodedIdBack, UploadIdTypes.uploadIdEnhanced);
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification. This method will run all bitmap conversion
     * and base64 string encoding on a thread and not impact the main UI thread.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @param idBackBitmap  Second Photo File to parse.
     */
    public static void uploadId(@NonNull final OnTaskCompleteListener listener,
                                     final String companyAPIKey, final String accessCode,
                                     final Bitmap idFrontBitmap, final Bitmap idBackBitmap) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, idFrontBitmap,
                idBackBitmap, UploadIdTypes.uploadId);
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadId(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param listener            {@link OnTaskCompleteListener}
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @param base64EncodedIdBack  Second Photo File already converted to base64 encoded String
     */
    public static void uploadId(@NonNull final OnTaskCompleteListener listener,
                                     String companyAPIKey, String accessCode,
                                     String base64EncodedIdFront, String base64EncodedIdBack) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, base64EncodedIdFront,
                base64EncodedIdBack, UploadIdTypes.uploadId);
    }
    
    /**
     * Upload a picture of a passport for the verification process. Note that only the front (The
     * portion with the data, usually on the first or second page) is required. This method will run all bitmap conversion
     * and base64 string encoding on a thread and not impact the main UI thread.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     */
    public static void uploadPassport(@NonNull final OnTaskCompleteListener listener,
                                final String companyAPIKey, final String accessCode,
                                final Bitmap idFrontBitmap) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, idFrontBitmap,
                null, UploadIdTypes.uploadPassport);
    }

    /**
     * Upload a picture of a passport for the verification process. Note that only the front (The
     * portion with the data, usually on the first or second page) is required.
     * I recommend using the other asynchronous method over this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#uploadPassport(OnTaskCompleteListener, String, String, String)}
     *
     * @param listener            {@link OnTaskCompleteListener}
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     */
    public static void uploadPassport(@NonNull final OnTaskCompleteListener listener,
                                String companyAPIKey, String accessCode,
                                String base64EncodedIdFront) {
        uploadIdEndpointsJoiner(listener, companyAPIKey, accessCode, base64EncodedIdFront,
                null, UploadIdTypes.uploadPassport);
    }

    private static void uploadIdEndpointsJoiner(@NonNull final OnTaskCompleteListener listener,
                                          final String companyAPIKey, final String accessCode,
                                          String base64EncodedIdFront, String base64EncodedIdBack,
                                          UploadIdTypes type){
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if(type == UploadIdTypes.uploadPassport){
            if(StringUtilities.isNullOrEmpty(base64EncodedIdFront)){
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        } else {
            if (StringUtilities.isNullOrEmpty(base64EncodedIdFront) ||
                    StringUtilities.isNullOrEmpty(base64EncodedIdBack)) {
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        Call<ResponseBody> call;
        switch (type){

            case uploadPassport:
                uploadPhotosObj.setIdFront(base64EncodedIdFront);
                call = myService.uploadPassport(companyAPIKey, uploadPhotosObj);
                break;

            case uploadId:
                uploadPhotosObj.setIdFront(base64EncodedIdFront);
                uploadPhotosObj.setIdBack(base64EncodedIdBack);
                call = myService.uploadId(companyAPIKey, uploadPhotosObj);
                break;

            case uploadIdEnhanced:
                uploadPhotosObj.setIdFront(base64EncodedIdFront);
                uploadPhotosObj.setIdBack(base64EncodedIdBack);
                call = myService.uploadIdEnhanced(companyAPIKey, uploadPhotosObj);
                break;

            default:
            case comparePhotos:
                uploadPhotosObj.setImg1(base64EncodedIdFront);
                uploadPhotosObj.setImg2(base64EncodedIdBack);
                call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
                break;
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    private static void uploadIdEndpointsJoiner(@NonNull final OnTaskCompleteListener listener,
                                          final String companyAPIKey, final String accessCode,
                                          final Bitmap idFrontBitmap, final Bitmap idBackBitmap,
                                          final UploadIdTypes type){
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if(type == UploadIdTypes.uploadPassport){
            if (idFrontBitmap == null) {
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        } else {
            if (idFrontBitmap == null || idBackBitmap == null) {
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        }

        if(type == UploadIdTypes.uploadPassport){
            if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0) {
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        } else {
            if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0 ||
                    idBackBitmap.getRowBytes() <= 0 || idBackBitmap.getHeight() <= 0) {
                listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
                return;
            }
        }

        ConvertPhotosAsync async = new ConvertPhotosAsync(null, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(Object result, int customTag) {
                if(customTag == AuthenticatingConstants.TAG_UPLOAD_PHOTO_OBJECT){
                    UploadPhotosObj uploadPhotosObj = (UploadPhotosObj) result;
                    if(uploadPhotosObj == null){
                        listener.onTaskComplete(buildErrorObject("Could not convert images"),
                                AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    } else {
                        uploadPhotosObj.setAccessCode(accessCode);
                        Call<ResponseBody> call;
                        switch (type){
                            case uploadPassport:
                                call = myService.uploadPassport(companyAPIKey, uploadPhotosObj);
                                break;

                            case uploadId:
                                call = myService.uploadId(companyAPIKey, uploadPhotosObj);
                                break;

                            case uploadIdEnhanced:
                                call = myService.uploadIdEnhanced(companyAPIKey, uploadPhotosObj);
                                break;
                                
                            case comparePhotos:
                            default:
                                call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
                                break;
                        }
                        call.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                try {
                                    ResponseBody object = response.body();
                                    ResponseBody errorBody = response.errorBody();
                                    String responseBodyString, errorBodyString;
                                    try {
                                        responseBodyString = object.string();
                                    } catch (Exception e){
                                        responseBodyString = null;
                                    }
                                    try {
                                        errorBodyString = errorBody.string();
                                    } catch (Exception e){
                                        errorBodyString = null;
                                    }
                                    try {
                                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                                    } catch (NullPointerException nope){}
                                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                                    if(myObjectToReturn != null) {
                                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                                    } else {
                                        AuthenticatingException parseError = buildParsingError();
                                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                                    }

                                } catch (AuthenticatingException authE) {
                                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                t.printStackTrace();
                                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
                            }
                        });
                    }
                } else if (customTag == AuthenticatingConstants.TAG_ERROR_RESPONSE){
                    listener.onTaskComplete(((AuthenticatingException)result),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                } else {
                    listener.onTaskComplete(buildErrorObject("Could not convert images"),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }
        }, idFrontBitmap, idBackBitmap, type);
        async.execute();
    }

    /**
     * Check the current status of the asynchronous image processing on the server
     *
     * @param listener {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link CheckPhotoResults}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static void checkUploadId(@NonNull final OnTaskCompleteListener listener,
                                                                          String companyAPIKey,
                                                                          String accessCode) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.checkUploadId(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    CheckPhotoResults myObjectToReturn = (CheckPhotoResults) RetrofitParser.convert(responseBodyString, CheckPhotoResults.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_CHECK_PHOTO_RESULT);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_CHECK_PHOTO_RESULT);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }
                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE,
                            AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Check the current status of the asynchronous image processing on the server
     *
     * @param listener {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link CheckPhotoResults}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static void checkUploadPassport(@NonNull final OnTaskCompleteListener listener,
                                     String companyAPIKey,
                                     String accessCode) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.checkUploadPassport(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    CheckPhotoResults myObjectToReturn = (CheckPhotoResults) RetrofitParser.convert(responseBodyString, CheckPhotoResults.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_CHECK_PHOTO_RESULT);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_CHECK_PHOTO_RESULT);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }
                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE,
                            AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()),
                        AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Get the Quiz. Note, if you receive a response that contains an error and indicates that
     * information is missing (IE, address, last name, etc), just update the user via updateUser
     * and re-call this endpoint again.
     * Note! If no quiz can be generated after all information is filled out, it may be required
     * to include the User's Social Security at this point so as to generate correct data.
     * DO NOT PERSIST OR STORE THE USER'S SOCIAL SECURITY NUMBER IN ANY WAY
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void getQuiz(@NonNull final OnTaskCompleteListener listener,
                               String companyAPIKey,
                               String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        User u = new User();
        u.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getQuiz(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    QuizObject myObjectToReturn = (QuizObject) RetrofitParser.convert(responseBodyString, QuizObject.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_QUIZ_QUESTIONS);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_QUIZ_QUESTIONS);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Verify the quiz by passing in the answers for the questions sent. The other information
     * listed here can be found in the original returned quiz object.
     *
     * @param listener         {@link OnTaskCompleteListener}
     * @param companyAPIKey    The company api key provided by Authenticating
     * @param accessCode       The identifier String given to a user. Obtained when creating the user
     * @param answers          Array of answer objects that contain the responses to the questions
     *                         that were obtained from the
     *                         {@link AuthenticatingAPICalls#getQuiz(OnTaskCompleteListener, String, String)} endpoint.
     * @param quizId           The quiz id. This is obtained from the {@link QuizObject} obtained from getQuiz()
     * @param transactionId    The quiz transaction id.
     *                         This is obtained from the {@link QuizObject} obtained from getQuiz()
     * @param responseUniqueId The quiz response unique id.
     *                         This is obtained from the {@link QuizObject} obtained from getQuiz()
     */
    public static void verifyQuiz(@NonNull final OnTaskCompleteListener listener,
                                  String companyAPIKey,
                                  String accessCode, VerifyQuizObj.Answer[] answers,
                                  String quizId, String transactionId,
                                  String responseUniqueId) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        VerifyQuizObj v = new VerifyQuizObj();
        v.setAccessCode(accessCode);
        v.setAnswers(answers);
        v.setQuizId(quizId);
        v.setTransactionID(transactionId);
        v.setResponseUniqueId(responseUniqueId);

        Call<ResponseBody> call = myService.verifyQuiz(companyAPIKey, v);
        AuthenticatingAPICalls.printOutRequestJson(v, AuthenticatingConstants.TYPE_VERIFY_QUIZ_OBJ, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Generate a background report for a user. Keep in mind that a user must have
     * completed their identity quiz before attempting this else it will throw
     * an error upon calling.
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void generateBackgroundReport(@NonNull final OnTaskCompleteListener listener,
                                                String companyAPIKey,
                                                String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        User user = new User();
        user.setAccessCode(accessCode);

        Call<ResponseBody> call = myService.generateCriminalReport(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    SimpleResponse myObjectToReturn = (SimpleResponse) RetrofitParser.convert(responseBodyString, SimpleResponse.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }


    /**
     * Get the user object
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void getUser(@NonNull final OnTaskCompleteListener listener,
                               String companyAPIKey,
                               String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        User user = new User();
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.getUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}

                    User myObjectToReturn = (User) RetrofitParser.convert(responseBodyString, User.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_USER);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_USER);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }
                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }

    /**
     * Update user (overloaded for User Object)
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param user          {@link User}
     */
    public static void updateUser(@NonNull final OnTaskCompleteListener listener,
                                  @NonNull String companyAPIKey, @NonNull String accessCode,
                                  @NonNull User user) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        user.setAccessCode(accessCode);
        Call<ResponseBody> call = myService.updateUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ResponseBody object = response.body();
                    ResponseBody errorBody = response.errorBody();
                    String responseBodyString, errorBodyString;
                    try {
                        responseBodyString = object.string();
                    } catch (Exception e){
                        responseBodyString = null;
                    }
                    try {
                        errorBodyString = errorBody.string();
                    } catch (Exception e){
                        errorBodyString = null;
                    }
                    try {
                        ErrorHandler.checkForAuthenticatingErrorObject(object);
                        ErrorHandler.checkForAuthenticatingError(errorBodyString);
                    } catch (NullPointerException nope){}
                    User myObjectToReturn = (User) RetrofitParser.convert(responseBodyString, User.class);
                    if(myObjectToReturn != null) {
                        AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_USER);
                        listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_USER);
                    } else {
                        AuthenticatingException parseError = buildParsingError();
                        listener.onTaskComplete(parseError, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                        AuthenticatingAPICalls.printOutResponseJson(parseError, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                    }

                } catch (AuthenticatingException authE) {
                    listener.onTaskComplete(authE, AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    AuthenticatingAPICalls.printOutResponseJson(authE, AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onTaskComplete(buildErrorObject(e.getMessage()),
                            AuthenticatingConstants.TAG_ERROR_RESPONSE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }


    /**
     * Update the user object. Other than the usual accessCode, apikey, and clientId, other fields
     * are optional
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param firstName     First name of user (IE: John)
     * @param lastName      Last name of User (IE: Smith)
     * @param birthYear     Birth year of user (IE: 1985 or 2001)
     * @param birthMonth    Birth month of the user (IE: 12 or 4)
     * @param birthDay      Birth Day of the user (IE: 1 or 31)
     * @param address       Address of User (IE: 123 Fake St)
     * @param city          City of the User (IE: Los Angeles)
     * @param state         State Abbreviation of User (IE: CA or NY)
     * @param zipcode       5 digit zip code / postal code of user (IE: 90210 or 20500)
     * @param street        street, as used by Canadian users. (IE: Jones Ave)
     * @param province      province, as used by Canadian users. (IE: ON) 
     * @param buildingNumber buildingNumber, as used by Canadian users. (IE: 137)
     * @param email         Email (IE, email@email.com)
     * @param phoneNumber   Phone number, numbers only (IE: 2138675309)
     * @param ssn           Social Security Number, 9 digits (IE: 123456789)
     */
    public static void updateUser(@NonNull final OnTaskCompleteListener listener,
                                  @NonNull String companyAPIKey,
                                  @NonNull String accessCode, @Nullable String firstName,
                                  @Nullable String lastName, @Nullable Integer birthYear, @Nullable Integer birthMonth,
                                  @Nullable Integer birthDay, @Nullable String address, @Nullable String city,
                                  @Nullable String state, @Nullable String zipcode, @Nullable String street,
                                  @Nullable String province, @Nullable String buildingNumber, @Nullable String email,
                                  @Nullable String phoneNumber, @Nullable String ssn) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        User user = new User();
        user.setAccessCode(accessCode);
        if (!isNullOrEmpty(firstName))
            user.setFirstName(firstName);
        if (!isNullOrEmpty(lastName))
            user.setLastName(lastName);
        if (birthYear != null)
            user.setYear(birthYear);
        if (birthMonth != null)
            user.setMonth(birthMonth);
        if (birthDay != null)
            user.setDay(birthDay);
        if(!StringUtilities.isNullOrEmpty(province))
            user.setProvince(province);
        if(!StringUtilities.isNullOrEmpty(buildingNumber))
            user.setBuildingNumber(buildingNumber);
        if(!StringUtilities.isNullOrEmpty(street))
            user.setStreet(street);
        if (!isNullOrEmpty(address))
            user.setAddress(address);
        if (!isNullOrEmpty(city))
            user.setCity(city);
        if (!isNullOrEmpty(state))
            user.setState(state);
        if (!isNullOrEmpty(zipcode))
            user.setZipcode(zipcode);
        if (!isNullOrEmpty(ssn))
            user.setSsn(ssn);
        if (!isNullOrEmpty(email))
            user.setEmail(email);
        if (!isNullOrEmpty(keepNumbersOnly(phoneNumber)))
            user.setPhone(keepNumbersOnly(phoneNumber));
        updateUser(listener, companyAPIKey, accessCode, user);
    }

    /////////////////////////
    //Error Builder Methods//
    /////////////////////////

    private static AuthenticatingException buildGenericErrorObject() {
        AuthenticatingException e = new AuthenticatingException();
        e.setAuthErrorString(GENERIC_ERROR_STRING);
        return e;
    }

    private static AuthenticatingException buildErrorObject(int code, String errorMsg) {
        AuthenticatingException e = new AuthenticatingException();
        e.setAuthErrorString(errorMsg);
        return e;
    }

    private static AuthenticatingException buildErrorObject(String errorMsg) {
        AuthenticatingException e = new AuthenticatingException();
        e.setAuthErrorString(errorMsg);
        return e;
    }

    private static AuthenticatingException buildParsingError() {
        return buildErrorObject(PARSING_CONVERSION_ERROR);
    }

    private static AuthenticatingException buildMissingAccessCodeError() {
        return buildErrorObject(MUST_INCLUDE_ACCESS_CODE);
    }

    private static AuthenticatingException buildMissingAuthKeyError() {
        return buildErrorObject(MISSING_AUTH_KEY);
    }

    ///////////
    //Logging//
    ///////////

    /**
     * Print out the response in json format for the developer
     *
     * @param obj Object to convert and parse
     * @paramAPIConstants.TYPEAPIConstants.TYPE {@java.lang.reflect.Type} that is being printed out
     */
    private static void printOutResponseJson(Object obj, Type type) {
        try {
            if (WebCallsLogging.isJsonLogging()) {
                if (type == AuthenticatingConstants.TYPE_AUTHENTICATING_ERROR) {
                    AuthenticatingException authE = (AuthenticatingException) obj;
                    String message = authE.getAuthErrorString();
                    if(StringUtilities.isNullOrEmpty(message)){
                        message = authE.getAuthErrorStringDetails();
                    }
                    Logging.log("AuthenticatingException API Response: " + message);
                } else {
                    try {
                        Logging.log("Authenticating API Response: " + new Gson().toJson(obj, type));
                    } catch (Exception e) {
                        //e.printStackTrace();
                        Logging.log("Authenticating API Response: Response Received, but was not parseable: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
        }//Not bothering with logging for client side
    }

    /**
     * Print out the response in json format for the developer
     *
     * @param obj  Object to convert and parse
     * @param call Call being used {@link Call}
     * @param <T>
     * @paramAPIConstants.TYPEAPIConstants.TYPE {@java.lang.reflect.Type} that is being printed out
     */
    private static <T> void printOutRequestJson(Object obj, Type type, Call<T> call) {
        try {
            if (WebCallsLogging.isJsonLogging()) {
                Logging.m("Outbound Request");
                if (call != null) {
                    try {
                        okhttp3.Headers headers = call.request().headers();
                        Map<String, List<String>> headersMap = headers.toMultimap();
                        if (headersMap != null) {
                            if (headersMap.size() > 0) {
                                Logging.m("Headers:\n");
                                for (Map.Entry<String, List<String>> aMap : headersMap.entrySet()) {
                                    String key = aMap.getKey();
                                    List<String> strs = aMap.getValue();
                                    for (String str : strs) {
                                        Logging.log(key + " : " + str);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                try {
                    Logging.log("Body:");
                    Logging.log(new Gson().toJson(obj, type));
                } catch (Exception e) {
                    //e.printStackTrace();
                    Logging.log("Could not be printed, " + e.getMessage());
                }
            }
        } catch (Exception e) {
        }//Not bothering with logging for client side
    }

    //////////////////////////
    //Misc Utility Functions//
    //////////////////////////

    /**
     * Encode a Bitmap to a base 64 String
     *
     * @param bm
     * @return
     */
    private static String encodeImage(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
    }

    /**
     * Encode an image to a base 64 String
     *
     * @param file
     * @return
     */
    private static String encodeImage(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (bm == null) {
            return null;
        }
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        //Base64.de
        return encImage;

    }

    /**
     * Encode an image to a base 64 String
     *
     * @param path
     * @return
     */
    private static String encodeImage(String path) {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        //Base64.de
        return encImage;

    }

    private static boolean isBitmapTooLarge(@NonNull Bitmap bmp) {
        return isImageTooLarge(bmp, AuthenticatingConstants.MAX_SIZE_IMAGE_UPLOAD);
    }

    /**
     * Determine if a bitmap is too large as compared to passed param
     * @param bmp Bitmap to check
     * @param desiredSizeInBytes Desired size (in Bytes) to check against
     * @return boolean, if true, bitmap is larger than the desired size, else, it is not.
     */
    private static boolean isImageTooLarge(@NonNull Bitmap bmp, float desiredSizeInBytes){
        long bitmapSize = bmp.getByteCount();
        float shrinkFactor = desiredSizeInBytes / bitmapSize;
        if(shrinkFactor >= 1){
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine the float value needed to resize the image so that it is less in size (Bytes)
     * than the value passed
     * @param bmp Bitmap to check
     * @param desiredSizeInBytes Desired size in bytes of the image
     * @return float value to resize. IE, if 0.34 is returned, the bitmap in question needs
     *         to be shrunk down by 34% to reach the desired size
     */
    private static float getImageResizeFactor(@NonNull Bitmap bmp, float desiredSizeInBytes){
        long bitmapSize = bmp.getByteCount();
        float flt = (desiredSizeInBytes / bitmapSize);
        return flt;
    }

    /**
     * Resize a photo
     * @param bmp Bitmap to resize
     * @param factorToDivide Factor to divide by. if (IE) 2 is passed, it will cut the
     *                       image in half, 10 will cut it down 10x in size
     * @return Resized bitmap. If it fails, will send back original
     */
    private static Bitmap resizePhoto(@NonNull Bitmap bmp, int factorToDivide){
        if(factorToDivide <= 1){
            factorToDivide = 2;
        }
        try {
            return Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() / factorToDivide),
                    (int)(bmp.getHeight() / factorToDivide), true);
        } catch (Exception e){
            e.printStackTrace();
            return bmp;
        }
    }

    /**
     * Resize a photo
     * @param bmp Bitmap to resize
     * @return Resized bitmap. If it fails, will send back original
     */
    private static Bitmap resizePhoto(@NonNull Bitmap bmp){
        try {
            double height = Math.sqrt(AuthenticatingConstants.MAX_SIZE_IMAGE_UPLOAD /
                    (((double) bmp.getWidth()) / bmp.getHeight()));
            double width = (height / bmp.getHeight()) * bmp.getWidth();
            Bitmap bmp1 = Bitmap.createScaledBitmap(bmp, (int)(width),
                    (int)(height), true);
            return bmp1;
        } catch (Exception e){
            e.printStackTrace();
            return bmp;
        }
    }

    /////////////////////////////////////
    //Async Class for Photo Conversions//
    /////////////////////////////////////

    /**
     * This class is used for converting photos of various formats into the correct, usable format
     * This class will be implemented asap in order to make life easier for developers.
     * In the meantime, please convert your own images to bitmaps for comparePhotos()
     */
    protected static class ConvertPhotosAsync extends AsyncTask<Void, Integer, UploadPhotosObj> {

        //Input variables
        private String base64EncodedImage1, base64EncodedImage2;
        private Bitmap bitmap1OrIDFront, bitmap2OrIDBack;
        private File file1, file2;
        private Uri uri1, uri2;

        //Misc
        private ProgressBar progressBar;
        private OnTaskCompleteListener listener;
        private Bitmap resizedBitmap1, resizedBitmap2;
        private boolean isString, isBitmap, isFile, isUri;
        private AuthenticatingAPICalls.UploadIdTypes type;

        //Output variables
        private String stringOutput1, stringOutput2;

        //Error Objects
        private AuthenticatingException error;

        private ConvertPhotosAsync(@Nullable ProgressBar progressBar,
                                   @NonNull OnTaskCompleteListener listener,
                                   @NonNull String base64EncodedImage1, @NonNull String base64EncodedImage2,
                                   @NonNull AuthenticatingAPICalls.UploadIdTypes type) {
            this.base64EncodedImage1 = base64EncodedImage1;
            this.base64EncodedImage2 = base64EncodedImage2;
            this.progressBar = progressBar;
            this.listener = listener;
            this.isString = true;
            this.isBitmap = false;
            this.isFile = false;
            this.isUri = false;
            this.type = type;
        }

        private ConvertPhotosAsync(@Nullable ProgressBar progressBar,
                                   @NonNull OnTaskCompleteListener listener,
                                   @NonNull Bitmap bitmap1OrIDFront, @NonNull Bitmap bitmap2OrIDBack,
                                   @NonNull AuthenticatingAPICalls.UploadIdTypes type) {
            this.bitmap1OrIDFront = bitmap1OrIDFront;
            this.bitmap2OrIDBack = bitmap2OrIDBack;
            this.progressBar = progressBar;
            this.listener = listener;
            this.isString = false;
            this.isBitmap = true;
            this.isFile = false;
            this.isUri = false;
            this.type = type;
        }

        private ConvertPhotosAsync(@Nullable ProgressBar progressBar,
                                   @NonNull OnTaskCompleteListener listener,
                                   @NonNull File imageFile1, @NonNull File imageFile2,
                                   @NonNull AuthenticatingAPICalls.UploadIdTypes type) {
            this.file1 = imageFile1;
            this.file2 = imageFile2;
            this.progressBar = progressBar;
            this.listener = listener;
            this.isString = false;
            this.isBitmap = false;
            this.isFile = true;
            this.isUri = false;
            this.type = type;
        }

        private ConvertPhotosAsync(@Nullable ProgressBar progressBar,
                                   @NonNull OnTaskCompleteListener listener,
                                   @NonNull Uri imageUri1, @NonNull Uri imageUri2,
                                   @NonNull AuthenticatingAPICalls.UploadIdTypes type) {
            this.uri1 = imageUri1;
            this.uri2 = imageUri2;
            this.progressBar = progressBar;
            this.listener = listener;
            this.isString = false;
            this.isBitmap = false;
            this.isFile = false;
            this.isUri = true;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.error = null;
            this.stringOutput1 = null;
            this.stringOutput2 = null;
            if(this.type == null){
                this.type = UploadIdTypes.comparePhotos;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(progressBar != null){
                try {
                    progressBar.setProgress(values[0]);
                } catch (Exception e){}
            }
        }

        @Override
        protected UploadPhotosObj doInBackground(Void... params) {
            UploadPhotosObj uploadPhotosObj;
            //First check type:
            if(type == UploadIdTypes.uploadPassport){
                if (isString) {
                    Pattern pattern = Pattern.compile(BASE_64_ENCODED_STRING_REGEX);
                    Matcher m1 = pattern.matcher(this.base64EncodedImage1);
                    if (m1.matches()) {
                        //Both strings are already converted properly to base 64
                        stringOutput1 = base64EncodedImage1;
                        return null;
                    } else {
                        //Strings are not properly formatted
                        error = buildErrorObject("Improperly formatted base64Encoded Strings");
                        return null;
                    }

                } else if (isBitmap) {
                    if (bitmap1OrIDFront == null) {
                        error = buildErrorObject("One or both bitmaps were null");
                        return null;
                    }

                } else if (isUri) {
                    file1 = convertUriToFile(uri1);

                    if (file1 == null) {
                        error = buildErrorObject("One or both of URIs sent could not be converted to Files");
                        return null;
                    }

                    bitmap1OrIDFront = convertFileToBitmap(file1);
                    if (bitmap1OrIDFront == null) {
                        error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                        return null;
                    }

                } else if (isFile) {
                    if (file1 == null) {
                        error = buildErrorObject("One or both of the files passed were null");
                        return null;
                    }
                    bitmap1OrIDFront = convertFileToBitmap(file1);
                    if (bitmap1OrIDFront == null) {
                        error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                        return null;
                    }

                }

                try {
                    if (isBitmapTooLarge(bitmap1OrIDFront)) {
                        bitmap1OrIDFront = AuthenticatingAPICalls.resizePhoto(bitmap1OrIDFront);
                    }
                } catch (OutOfMemoryError oom) {
                    //File too large, resize to very small
                    bitmap1OrIDFront = shrinkPhoto(bitmap1OrIDFront, 8);
                }

                try {
                    stringOutput1 = encodeImage(bitmap1OrIDFront);
                } catch (Exception e) {
                    error = buildErrorObject("Could not convert images to base64: " + e.getMessage());
                    return null;
                }

                if (isNullOrEmpty(stringOutput1)) {
                    uploadPhotosObj = null;
                    error = buildErrorObject("Could not convert images to base64");
                } else {
                    uploadPhotosObj = new UploadPhotosObj();
                    switch (type) {
                        case uploadPassport:
                            uploadPhotosObj.setIdFront(stringOutput1);
                            break;
                    }
                }
            } else {
                if (isString) {
                    Pattern pattern = Pattern.compile(BASE_64_ENCODED_STRING_REGEX);
                    Matcher m1 = pattern.matcher(this.base64EncodedImage1);
                    Matcher m2 = pattern.matcher(this.base64EncodedImage2);
                    if (m1.matches() && m2.matches()) {
                        //Both strings are already converted properly to base 64
                        stringOutput1 = base64EncodedImage1;
                        stringOutput2 = base64EncodedImage2;
                        return null;
                    } else {
                        //Strings are not properly formatted
                        error = buildErrorObject("Improperly formatted base64Encoded Strings");
                        return null;
                    }

                } else if (isBitmap) {
                    if (bitmap1OrIDFront == null || bitmap2OrIDBack == null) {
                        error = buildErrorObject("One or both bitmaps were null");
                        return null;
                    }

                } else if (isUri) {
                    file1 = convertUriToFile(uri1);
                    file2 = convertUriToFile(uri2);

                    if (file1 == null || file2 == null) {
                        error = buildErrorObject("One or both of URIs sent could not be converted to Files");
                        return null;
                    }

                    bitmap1OrIDFront = convertFileToBitmap(file1);
                    bitmap2OrIDBack = convertFileToBitmap(file2);
                    if (bitmap1OrIDFront == null || bitmap2OrIDBack == null) {
                        error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                        return null;
                    }

                } else if (isFile) {
                    if (file1 == null || file2 == null) {
                        error = buildErrorObject("One or both of the files passed were null");
                        return null;
                    }
                    bitmap1OrIDFront = convertFileToBitmap(file1);
                    bitmap2OrIDBack = convertFileToBitmap(file2);
                    if (bitmap1OrIDFront == null || bitmap2OrIDBack == null) {
                        error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                        return null;
                    }

                }

                try {
                    if (isBitmapTooLarge(bitmap1OrIDFront)) {
                        bitmap1OrIDFront = AuthenticatingAPICalls.resizePhoto(bitmap1OrIDFront);
                    }
                } catch (OutOfMemoryError oom) {
                    //File too large, resize to very small
                    bitmap1OrIDFront = shrinkPhoto(bitmap1OrIDFront, 8);
                }

                try {
                    if (isBitmapTooLarge(bitmap2OrIDBack)) {
                        bitmap2OrIDBack = AuthenticatingAPICalls.resizePhoto(bitmap2OrIDBack);
                    }
//                while(isBitmapTooLarge(bitmap2OrIDBack)){
//                    bitmap2OrIDBack = shrinkPhoto(bitmap2OrIDBack, 2);
//                }
                } catch (OutOfMemoryError oom) {
                    //File too large, resize to very small
                    bitmap2OrIDBack = shrinkPhoto(bitmap2OrIDBack, 8);
                }
                try {
                    stringOutput1 = encodeImage(bitmap1OrIDFront);
                    stringOutput2 = encodeImage(bitmap2OrIDBack);
                } catch (Exception e) {
                    error = buildErrorObject("Could not convert images to base64: " + e.getMessage());
                    return null;
                }

                if (isNullOrEmpty(stringOutput1) || isNullOrEmpty(stringOutput2)) {
                    uploadPhotosObj = null;
                    error = buildErrorObject("Could not convert images to base64");
                } else {
                    uploadPhotosObj = new UploadPhotosObj();
                    switch (type) {
                        case uploadIdEnhanced:
                        case uploadId:
                            uploadPhotosObj.setIdFront(stringOutput1);
                            uploadPhotosObj.setIdBack(stringOutput2);
                            break;

                        case comparePhotos:
                            uploadPhotosObj.setImg1(stringOutput1);
                            uploadPhotosObj.setImg2(stringOutput2);
                            break;
                    }
                }
            }
            if(bitmap1OrIDFront != null) {
                bitmap1OrIDFront.recycle();
            }
            if(bitmap2OrIDBack != null) {
                bitmap2OrIDBack.recycle();
            }
            return uploadPhotosObj;
        }

        @Override
        protected void onPostExecute(UploadPhotosObj args) {
            if(error == null){
                if(args != null){
                    listener.onTaskComplete(args, AuthenticatingConstants.TAG_UPLOAD_PHOTO_OBJECT);
                    return;
                } else {
                    error = buildErrorObject("An unknown error has occurred");
                }
                listener.onTaskComplete(error, AuthenticatingConstants.TAG_ERROR_RESPONSE);
            } else {
                //Error, return and tag
                listener.onTaskComplete(error, AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        }
    }


    private static Bitmap shrinkPhoto(@NonNull Bitmap bmp, int factorToDivide){
        if(factorToDivide <= 1){
            factorToDivide = 2;
        }
        try {
            return Bitmap.createScaledBitmap(bmp, (bmp.getWidth() / factorToDivide),
                    (bmp.getHeight() / factorToDivide), true);
        } catch (Exception e){
            return bmp;
        }
    }

    private static Bitmap convertFileToBitmap(@NonNull File file){
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeFile(file.getPath());
            if(bmp == null){
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return bmp;
    }

    private static File convertUriToFile(@NonNull Uri uri){
        File file = null;
        try {
            file = new File(uri.getPath());
            if(file == null){
                file = new File(uri.getEncodedPath());
            }
            /*
            //To be implemented at a later date:
            // https://stackoverflow.com/questions/6935497/android-get-gallery-image-uri-path
            if(file == null){
                Context context = null;
                String[] projection = { MediaStore.Images.Media.DATA };
                CursorLoader cursor = new CursorLoader(context, uri, projection, null, null, null);
                cursor.startLoading();
                //cursor.loadInBackground();
                String str = cursor.getSelection();
                file = new File(str);
            }
            */
        } catch (Exception e){
            e.printStackTrace();
        }
        return file;
    }

}