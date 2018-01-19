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
import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworksHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResultsHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObjectHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponseObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UploadPhotosObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UserHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;
import hotb.pgmacdesign.authenticatingsdk.interfaces.OnTaskCompleteListener;
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

    private static final String BASE_64_ENCODED_STRING_REGEX = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
    private static final String URL_BASE = AuthenticatingConstants.BASE_URL;

    private static APIService myService;
    private static AsyncTask<Void, Void, Void> uploadPhotosAsynctask;

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
     * @return {@link AvailableNetworksHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static AvailableNetworksHeader getAvailableNetworks(String companyAPIKey,
                                                               String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<AvailableNetworksHeader> call = myService.getAvailableNetworks(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        AvailableNetworksHeader toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (AvailableNetworksHeader) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_AVAILABLE_NETWORKS);
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
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj verifySocialNetworks(String companyAPIKey,
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
        Call<SimpleResponseObj> call = myService.verifySocialNetworks(companyAPIKey, s);
        AuthenticatingAPICalls.printOutRequestJson(s, AuthenticatingConstants.TYPE_SOCIAL_NETWORK_OBJ, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj verifyPhone(String companyAPIKey,
                                                String accessCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        Call<SimpleResponseObj> call = myService.verifyPhone(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj verifyPhoneCode(String companyAPIKey,
                                                    String accessCode, String smsCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        PhoneVerification p = new PhoneVerification();
        p.setAccessCode(accessCode);
        p.setSmsCode(smsCode);
        Call<SimpleResponseObj> call = myService.verifyPhoneCode(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj verifyEmail(String companyAPIKey,
                                                String accessCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);
        Call<SimpleResponseObj> call = myService.verifyEmail(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedImage1 First Photo File already converted to base64 encoded String
     * @param base64EncodeImage2  Second Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj comparePhotos(String companyAPIKey, String accessCode,
                                                  String base64EncodedImage1,
                                                  String base64EncodeImage2) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if (StringUtilities.isNullOrEmpty(base64EncodedImage1) ||
                StringUtilities.isNullOrEmpty(base64EncodeImage2)) {
            return null;
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        uploadPhotosObj.setImg1(base64EncodedImage1);
        uploadPhotosObj.setImg2(base64EncodeImage2);
        Call<SimpleResponseObj> call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param photo1Bitmap  First Photo File to parse.
     * @param photo2Bitmap  Second Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj comparePhotos(String companyAPIKey, String accessCode,
                                                  Bitmap photo1Bitmap, Bitmap photo2Bitmap) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if (photo1Bitmap == null || photo2Bitmap == null) {
            return null;
        }

        if (photo1Bitmap.getRowBytes() <= 0 || photo1Bitmap.getHeight() <= 0 ||
                photo2Bitmap.getRowBytes() <= 0 || photo2Bitmap.getHeight() <= 0) {
            return null;
        }


        if (isBitmapTooLarge(photo1Bitmap)) {
            try {
                photo1Bitmap = AuthenticatingAPICalls.resizePhoto(photo1Bitmap);
//                photo1Bitmap = Bitmap.createScaledBitmap(photo1Bitmap,
//                        (photo1Bitmap.getWidth() / 8), (photo1Bitmap.getHeight() / 8), true);
                //int size = (photo1Bitmap.getRowBytes() * photo1Bitmap.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isBitmapTooLarge(photo2Bitmap)) {
            try {
                photo2Bitmap = AuthenticatingAPICalls.resizePhoto(photo2Bitmap);
//                photo2Bitmap = Bitmap.createScaledBitmap(photo2Bitmap,
//                        (photo2Bitmap.getWidth() / 8), (photo2Bitmap.getHeight() / 8), true);
                //int size = (photo2Bitmap.getRowBytes() * photo2Bitmap.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        String base64Image1 = null, base64Image2 = null;
        try {
            base64Image1 = encodeImage(photo1Bitmap);
            base64Image2 = encodeImage(photo2Bitmap);
        } catch (Exception e) {
            return null;
        }

        uploadPhotosObj.setImg1(base64Image1);
        uploadPhotosObj.setImg2(base64Image2);
        Call<SimpleResponseObj> call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedIdFront First Photo File already converted to base64 encoded String
     * @param base64EncodeIdBack  Second Photo File already converted to base64 encoded String
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj uploadId(String companyAPIKey, String accessCode,
                                                  String base64EncodedIdFront,
                                                  String base64EncodeIdBack) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if (StringUtilities.isNullOrEmpty(base64EncodedIdFront) ||
                StringUtilities.isNullOrEmpty(base64EncodeIdBack)) {
            return null;
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        uploadPhotosObj.setIdFront(base64EncodedIdFront);
        uploadPhotosObj.setIdBack(base64EncodeIdBack);
        Call<SimpleResponseObj> call = myService.uploadId(companyAPIKey, uploadPhotosObj);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @param idFrontBitmap  First Photo File to parse.
     * @param idBackBitmap  Second Photo File to parse.
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj uploadId(String companyAPIKey, String accessCode,
                                                  Bitmap idFrontBitmap, Bitmap idBackBitmap) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            return null;
        }

        if (idFrontBitmap == null || idBackBitmap == null) {
            return null;
        }

        if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0 ||
                idBackBitmap.getRowBytes() <= 0 || idBackBitmap.getHeight() <= 0) {
            return null;
        }


        if (isBitmapTooLarge(idFrontBitmap)) {
            try {
                idFrontBitmap = AuthenticatingAPICalls.resizePhoto(idFrontBitmap);
//                photo1Bitmap = Bitmap.createScaledBitmap(photo1Bitmap,
//                        (photo1Bitmap.getWidth() / 8), (photo1Bitmap.getHeight() / 8), true);
                //int size = (photo1Bitmap.getRowBytes() * photo1Bitmap.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isBitmapTooLarge(idBackBitmap)) {
            try {
                idBackBitmap = AuthenticatingAPICalls.resizePhoto(idBackBitmap);
//                photo2Bitmap = Bitmap.createScaledBitmap(photo2Bitmap,
//                        (photo2Bitmap.getWidth() / 8), (photo2Bitmap.getHeight() / 8), true);
                //int size = (photo2Bitmap.getRowBytes() * photo2Bitmap.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        String idFront = null, idBack = null;
        try {
            idFront = encodeImage(idFrontBitmap);
            idBack = encodeImage(idBackBitmap);
        } catch (Exception e) {
            return null;
        }

        uploadPhotosObj.setIdFront(idFront);
        uploadPhotosObj.setIdBack(idBack);
        Call<SimpleResponseObj> call = myService.uploadId(companyAPIKey, uploadPhotosObj);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link CheckPhotoResultsHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static CheckPhotoResultsHeader checkUploadId(String companyAPIKey,
                                                 String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<CheckPhotoResultsHeader> call = myService.checkUploadId(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        CheckPhotoResultsHeader toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);
            toReturn = (CheckPhotoResultsHeader) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn,
                    AuthenticatingConstants.TYPE_CHECK_PHOTO_RESULTS_HEADER);
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
     * @return {@link QuizObjectHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static QuizObjectHeader getQuiz(String companyAPIKey,
                                           String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<QuizObjectHeader> call = myService.getQuiz(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        QuizObjectHeader toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (QuizObjectHeader) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_QUIZ_QUESTIONS_HEADER);
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
     * @param quizId           The quiz id. This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
     * @param transactionId    The quiz transaction id.
     *                         This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
     * @param responseUniqueId The quiz response unique id.
     *                         This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj verifyQuiz(String companyAPIKey,
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

        Call<SimpleResponseObj> call = myService.verifyQuiz(companyAPIKey, v);
        AuthenticatingAPICalls.printOutRequestJson(v, AuthenticatingConstants.TYPE_VERIFY_QUIZ_OBJ, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link SimpleResponseObj}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static SimpleResponseObj generateBackgroundReport(String companyAPIKey,
                                                             String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);

        Call<SimpleResponseObj> call = myService.generateCriminalReport(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
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
     * @return {@link UserHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static UserHeader getUser(String companyAPIKey,
                                     String accessCode) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);
        Call<UserHeader> call = myService.getUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        UserHeader toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (UserHeader) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_USER_HEADER);
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
     * @param user          {@link hotb.pgmacdesign.authenticatingsdk.datamodels.UserHeader.User}
     * @return {@link UserHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static UserHeader updateUser(@NonNull String companyAPIKey, @NonNull String accessCode,
                                        @NonNull UserHeader.User user) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        user.setAccessCode(accessCode);
        Call<UserHeader> call = myService.updateUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        UserHeader toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (UserHeader) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_USER_HEADER);
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
     * @return {@link UserHeader}
     * @throws AuthenticatingException {@link AuthenticatingException}
     */
    public static UserHeader updateUser(@NonNull String companyAPIKey,
                                        @NonNull String accessCode, @Nullable String firstName,
                                        @Nullable String lastName, @Nullable Integer birthYear, @Nullable Integer birthMonth,
                                        @Nullable Integer birthDay, @Nullable String address, @Nullable String city,
                                        @Nullable String state, @Nullable String zipcode, @Nullable String street,
                                        @Nullable String province, @Nullable String buildingNumber, @Nullable String email,
                                        @Nullable String phoneNumber, @Nullable String ssn) throws AuthenticatingException {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }

        UserHeader.User user = new UserHeader.User();
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

    /**
     * Authenticate a Canada / China Profile (Non-USA)
     *
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static SimpleResponseObj authenticateProfile(String companyAPIKey,
                                           String accessCode) throws AuthenticatingException {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            throw buildMissingAuthKeyError();
        }
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);

        Call<SimpleResponseObj> call = myService.authenticateProfile(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        SimpleResponseObj toReturn = null;
        try {
            Response response = call.execute();

            //Check the Error first
            ErrorHandler.checkForAuthenticatingError(response);

            toReturn = (SimpleResponseObj) response.body();
            AuthenticatingAPICalls.printOutResponseJson(toReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return toReturn;
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
        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<AvailableNetworksHeader> call = myService.getAvailableNetworks(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<AvailableNetworksHeader>() {
            @Override
            public void onResponse(Call<AvailableNetworksHeader> call, Response<AvailableNetworksHeader> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    AvailableNetworksHeader myObjectToReturn = (AvailableNetworksHeader) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_AVAILABLE_NETWORKS);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_AVAILABLE_NETWORKS);

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
            public void onFailure(Call<AvailableNetworksHeader> call, Throwable t) {
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
        Call<SimpleResponseObj> call = myService.verifySocialNetworks(companyAPIKey, s);
        AuthenticatingAPICalls.printOutRequestJson(s, AuthenticatingConstants.TYPE_SOCIAL_NETWORK_OBJ, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        Call<SimpleResponseObj> call = myService.verifyPhone(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        Call<SimpleResponseObj> call = myService.verifyPhoneCode(companyAPIKey, p);
        AuthenticatingAPICalls.printOutRequestJson(p, AuthenticatingConstants.TYPE_PHONE_VERIFICATION, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);
        Call<SimpleResponseObj> call = myService.verifyEmail(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (photo1Bitmap == null || photo2Bitmap == null) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (photo1Bitmap.getRowBytes() <= 0 || photo1Bitmap.getHeight() <= 0 ||
                photo2Bitmap.getRowBytes() <= 0 || photo2Bitmap.getHeight() <= 0) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        ConvertPhotosAsync async = new ConvertPhotosAsync(null, new OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(Object result, int customTag) {
                if(customTag == AuthenticatingConstants.TAG_UPLOAD_PHOTO_OBJECT) {
                    UploadPhotosObj uploadPhotosObj = (UploadPhotosObj) result;
                    if (uploadPhotosObj == null) {
                        listener.onTaskComplete(buildErrorObject("Could not convert images"),
                                AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    } else {
                        uploadPhotosObj.setAccessCode(accessCode);
                        Call<SimpleResponseObj> call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
                        call.enqueue(new Callback<SimpleResponseObj>() {
                            @Override
                            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                                try {
                                    ErrorHandler.checkForAuthenticatingError(response);
                                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
                            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        }, photo1Bitmap, photo2Bitmap, ConvertPhotosAsync.RequestedReturnType.COMPARE_PHOTOS);
        async.execute();
    }

    /**
     * Upload 2 photos to the endpoint for Photo proof.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
     *
     * @param listener            {@link OnTaskCompleteListener}
     * @param companyAPIKey       The company api key provided by Authenticating
     * @param accessCode          The identifier String given to a user. Obtained when creating the user
     * @param base64EncodedImage1 First Photo File already converted to base64 encoded String
     * @param base64EncodeImage2  Second Photo File already converted to base64 encoded String
     */
    public static void comparePhotos(@NonNull final OnTaskCompleteListener listener,
                                     String companyAPIKey, String accessCode,
                                     String base64EncodedImage1, String base64EncodeImage2) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (StringUtilities.isNullOrEmpty(base64EncodedImage1) ||
                StringUtilities.isNullOrEmpty(base64EncodeImage2)) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        uploadPhotosObj.setImg1(base64EncodedImage1);
        uploadPhotosObj.setImg2(base64EncodeImage2);
        Call<SimpleResponseObj> call = myService.comparePhotos(companyAPIKey, uploadPhotosObj);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
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

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (idFrontBitmap == null || idBackBitmap == null) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (idFrontBitmap.getRowBytes() <= 0 || idFrontBitmap.getHeight() <= 0 ||
                idBackBitmap.getRowBytes() <= 0 || idBackBitmap.getHeight() <= 0) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
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
                        Call<SimpleResponseObj> call = myService.uploadId(companyAPIKey, uploadPhotosObj);
                        call.enqueue(new Callback<SimpleResponseObj>() {
                            @Override
                            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                                try {
                                    ErrorHandler.checkForAuthenticatingError(response);
                                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
                            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        }, idFrontBitmap, idBackBitmap, ConvertPhotosAsync.RequestedReturnType.UPLOAD_ID);
        async.execute();
    }

    /**
     * Upload 2 photos to the endpoint for uploadId and identify verification.
     * I recommend using the other asynchronous method for this one due to the possibility of more errors
     * {@link AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
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

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        if (StringUtilities.isNullOrEmpty(base64EncodedIdFront) ||
                StringUtilities.isNullOrEmpty(base64EncodedIdBack)) {
            listener.onTaskComplete(buildErrorObject("Please pass in a valid photo"),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }

        UploadPhotosObj uploadPhotosObj = new UploadPhotosObj();
        uploadPhotosObj.setAccessCode(accessCode);
        uploadPhotosObj.setIdFront(base64EncodedIdFront);
        uploadPhotosObj.setIdBack(base64EncodedIdBack);
        Call<SimpleResponseObj> call = myService.uploadId(companyAPIKey, uploadPhotosObj);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
    }


    /**
     * Check the current status of the asynchronous image processing on the server
     *
     * @param listener {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     * @return {@link CheckPhotoResultsHeader.CheckPhotoResults}
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

        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<CheckPhotoResultsHeader> call = myService.checkUploadId(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<CheckPhotoResultsHeader>() {
            @Override
            public void onResponse(Call<CheckPhotoResultsHeader> call, Response<CheckPhotoResultsHeader> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    CheckPhotoResultsHeader myObjectToReturn = (CheckPhotoResultsHeader) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_CHECK_PHOTO_RESULTS_HEADER);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_CHECK_PHOTO_RESULT_OBJECT);
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
            public void onFailure(Call<CheckPhotoResultsHeader> call, Throwable t) {
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
        UserHeader.User u = new UserHeader.User();
        u.setAccessCode(accessCode);
        Call<QuizObjectHeader> call = myService.getQuiz(companyAPIKey, u);
        AuthenticatingAPICalls.printOutRequestJson(u, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<QuizObjectHeader>() {
            @Override
            public void onResponse(Call<QuizObjectHeader> call, Response<QuizObjectHeader> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    QuizObjectHeader myObjectToReturn = (QuizObjectHeader) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_QUIZ_QUESTIONS_HEADER);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_QUIZ_QUESTIONS_HEADER);

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
            public void onFailure(Call<QuizObjectHeader> call, Throwable t) {
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
     * @param quizId           The quiz id. This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
     * @param transactionId    The quiz transaction id.
     *                         This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
     * @param responseUniqueId The quiz response unique id.
     *                         This is obtained from the {@link QuizObjectHeader} obtained from getQuiz()
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

        Call<SimpleResponseObj> call = myService.verifyQuiz(companyAPIKey, v);
        AuthenticatingAPICalls.printOutRequestJson(v, AuthenticatingConstants.TYPE_VERIFY_QUIZ_OBJ, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);

        Call<SimpleResponseObj> call = myService.generateCriminalReport(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);

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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
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
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);
        Call<UserHeader> call = myService.getUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<UserHeader>() {
            @Override
            public void onResponse(Call<UserHeader> call, Response<UserHeader> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    UserHeader myObjectToReturn = (UserHeader) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_USER_HEADER);
                    if (myObjectToReturn == null) {
                        listener.onTaskComplete(buildGenericErrorObject(),
                                AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    } else {
                        listener.onTaskComplete(myObjectToReturn,
                                AuthenticatingConstants.TAG_USER_HEADER);
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
            public void onFailure(Call<UserHeader> call, Throwable t) {
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
     * @param user          {@link hotb.pgmacdesign.authenticatingsdk.datamodels.UserHeader.User}
     */
    public static void updateUser(@NonNull final OnTaskCompleteListener listener,
                                  @NonNull String companyAPIKey, @NonNull String accessCode,
                                  @NonNull UserHeader.User user) {
        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        user.setAccessCode(accessCode);
        Call<UserHeader> call = myService.updateUser(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<UserHeader>() {
            @Override
            public void onResponse(Call<UserHeader> call, Response<UserHeader> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    UserHeader myObjectToReturn = (UserHeader) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_USER_HEADER);
                    listener.onTaskComplete(myObjectToReturn, AuthenticatingConstants.TAG_USER_HEADER);

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
            public void onFailure(Call<UserHeader> call, Throwable t) {
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

        UserHeader.User user = new UserHeader.User();
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

    /**
     * Authenticate a Canada / China Profile (Non-USA)
     *
     * @param listener      {@link OnTaskCompleteListener}
     * @param companyAPIKey The company api key provided by Authenticating
     * @param accessCode    The identifier String given to a user. Obtained when creating the user
     */
    public static void authenticateProfile(@NonNull final OnTaskCompleteListener listener,
                                           @NonNull String companyAPIKey,
                                           @NonNull String accessCode) {

        if (StringUtilities.isNullOrEmpty(accessCode)) {
            listener.onTaskComplete(buildMissingAuthKeyError(),
                    AuthenticatingConstants.TAG_ERROR_RESPONSE);
            return;
        }
        UserHeader.User user = new UserHeader.User();
        user.setAccessCode(accessCode);
        Call<SimpleResponseObj> call = myService.authenticateProfile(companyAPIKey, user);
        AuthenticatingAPICalls.printOutRequestJson(user, AuthenticatingConstants.TYPE_USER, call);
        call.enqueue(new Callback<SimpleResponseObj>() {
            @Override
            public void onResponse(Call<SimpleResponseObj> call, Response<SimpleResponseObj> response) {
                try {
                    ErrorHandler.checkForAuthenticatingError(response);
                    SimpleResponseObj myObjectToReturn = (SimpleResponseObj) response.body();
                    AuthenticatingAPICalls.printOutResponseJson(myObjectToReturn, AuthenticatingConstants.TYPE_SIMPLE_RESPONSE);
                    if (myObjectToReturn == null) {
                        listener.onTaskComplete(buildGenericErrorObject(),
                                AuthenticatingConstants.TAG_ERROR_RESPONSE);
                    } else {
                        listener.onTaskComplete(myObjectToReturn,
                                AuthenticatingConstants.TAG_SIMPLE_RESPONSE_OBJ);
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
            public void onFailure(Call<SimpleResponseObj> call, Throwable t) {
                t.printStackTrace();
                listener.onTaskComplete(buildErrorObject(t.getMessage()), AuthenticatingConstants.TAG_ERROR_RESPONSE);
            }
        });
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

    private static AuthenticatingException buildMissingUserIDError() {
        AuthenticatingException e = new AuthenticatingException();
        return buildErrorObject("ddddddddddddddddd");
    }

    private static AuthenticatingException buildMissingAuthKeyError() {
        AuthenticatingException e = new AuthenticatingException();
        return buildErrorObject("You did not include your authKey. This is obtained when you register for an account. Calls will not function without this key");
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
                    String message = authE.getMessage();
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

        static enum RequestedReturnType {
            UPLOAD_ID, COMPARE_PHOTOS
        }

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
        private RequestedReturnType type;

        //Output variables
        private String stringOutput1, stringOutput2;

        //Error Objects
        private AuthenticatingException error;

        private ConvertPhotosAsync(@Nullable ProgressBar progressBar,
                                   @NonNull OnTaskCompleteListener listener,
                                   @NonNull String base64EncodedImage1, @NonNull String base64EncodedImage2,
                                   @NonNull RequestedReturnType type) {
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
                                   @NonNull RequestedReturnType type) {
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
                                   @NonNull RequestedReturnType type) {
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
                                   @NonNull RequestedReturnType type) {
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
                this.type = RequestedReturnType.COMPARE_PHOTOS;
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
            if(isString){
                Pattern pattern = Pattern.compile(BASE_64_ENCODED_STRING_REGEX);
                Matcher m1 = pattern.matcher(this.base64EncodedImage1);
                Matcher m2 = pattern.matcher(this.base64EncodedImage2);
                if(m1.matches() && m2.matches()){
                    //Both strings are already converted properly to base 64
                    stringOutput1 = base64EncodedImage1;
                    stringOutput2 = base64EncodedImage2;
                    return null;
                } else {
                    //Strings are not properly formatted
                    error = buildErrorObject("Improperly formatted base64Encoded Strings");
                    return null;
                }

            } else if (isBitmap){
                if(bitmap1OrIDFront == null || bitmap2OrIDBack == null){
                    error = buildErrorObject("One or both bitmaps were null");
                    return null;
                }

            }  else if (isUri){
                file1 = convertUriToFile(uri1);
                file2 = convertUriToFile(uri2);

                if(file1 == null || file2 == null){
                    error = buildErrorObject("One or both of URIs sent could not be converted to Files");
                    return null;
                }

                bitmap1OrIDFront = convertFileToBitmap(file1);
                bitmap2OrIDBack = convertFileToBitmap(file2);
                if(bitmap1OrIDFront == null || bitmap2OrIDBack == null){
                    error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                    return null;
                }

            } else if (isFile){
                if(file1 == null || file2 == null){
                    error = buildErrorObject("One or both of the files passed were null");
                    return null;
                }
                bitmap1OrIDFront = convertFileToBitmap(file1);
                bitmap2OrIDBack = convertFileToBitmap(file2);
                if(bitmap1OrIDFront == null || bitmap2OrIDBack == null){
                    error = buildErrorObject("One or both of the files could not be converted to bitmaps");
                    return null;
                }

            }

            try {
                if(isBitmapTooLarge(bitmap1OrIDFront)){
                    bitmap1OrIDFront = AuthenticatingAPICalls.resizePhoto(bitmap1OrIDFront);
                }
            } catch (OutOfMemoryError oom){
                //File too large, resize to very small
                bitmap1OrIDFront = shrinkPhoto(bitmap1OrIDFront, 8);
            }

            try {
                if(isBitmapTooLarge(bitmap2OrIDBack)){
                    bitmap2OrIDBack = AuthenticatingAPICalls.resizePhoto(bitmap2OrIDBack);
                }
//                while(isBitmapTooLarge(bitmap2OrIDBack)){
//                    bitmap2OrIDBack = shrinkPhoto(bitmap2OrIDBack, 2);
//                }
            } catch (OutOfMemoryError oom){
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

            if(isNullOrEmpty(stringOutput1) || isNullOrEmpty(stringOutput2)){
                uploadPhotosObj = null;
                error = buildErrorObject("Could not convert images to base64");
            } else {
                uploadPhotosObj = new UploadPhotosObj();
                switch (type){
                    case UPLOAD_ID:
                        uploadPhotosObj.setIdFront(stringOutput1);
                        uploadPhotosObj.setIdBack(stringOutput2);
                        break;

                    case COMPARE_PHOTOS:
                        uploadPhotosObj.setImg1(stringOutput1);
                        uploadPhotosObj.setImg2(stringOutput2);
                        break;
                }
            }
            bitmap1OrIDFront.recycle();
            bitmap2OrIDBack.recycle();
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
            //To be implemented at a later data:
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