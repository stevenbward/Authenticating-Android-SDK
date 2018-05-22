package hotb.pgmacdesign.authenticatingsdk.networking;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworks;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResults;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObject;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponse;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.TopLevelObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UploadPhotosObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.User;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Link to web documentation: https://docs.authenticating.com
 * Created by pmacdowell on 2017-07-13.
 */

public interface APIService {

    //Endpoint + Version Strings
    public static final String API = "/api";
    public static final String VERSION = "/v2";

    /**
     * Send an SMS/ Text MSG to the phone number attached to the user accessCode being sent.
     *
     * @param authKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode
     * @return {@link TopLevelObj}
     */
    @POST(API + VERSION + "/verifyPhone ")
    Call<ResponseBody> verifyPhone(@Header("authKey") String authKey,
                                        @Body PhoneVerification phoneInfo
    );

    /**
     * Send an SMS/ Text MSG to the phone number attached to the user accessCode being sent.
     *
     * @param authKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode, smsCode
     * @return {@link SimpleResponse}
     */
    @POST(API + VERSION + "/verifyPhoneCode ")
    Call<ResponseBody> verifyPhoneCode(@Header("authKey") String authKey,
                                            @Body PhoneVerification phoneInfo
    );

    /**
     * Verify a User's email
     *
     * @param authKey
     * @param user          Only requires accessCode
     * @return {@link SimpleResponse}
     */
    @POST(API + VERSION + "/verifyEmail ")
    Call<ResponseBody> verifyEmail(@Header("authKey") String authKey,
                                        @Body User user
    );

    /**
     * Used for passing the verification of social networks
     *
     * @param authKey    API Key needed for calls
     * @param socialNetworkObj Only required fields include: accessCode, network,
     *                         socialMediaAccessToken, socialMediaUserId
     * @return {@link SimpleResponse}
     */
    @POST(API + VERSION + "/verifySocialNetworks")
    Call<ResponseBody> verifySocialNetworks(@Header("authKey") String authKey,
                                                 @Body SocialNetworkObj socialNetworkObj
    );

    /**
     * Get the available networks for use in Social Networking test
     *
     * @param authKey API Key needed for calls
     * @param user          Only required fields include: accessCode
     * @return {@link AvailableNetworks}
     */
    @POST(API + VERSION + "/getAvailableNetworks")
    Call<ResponseBody> getAvailableNetworks(@Header("authKey") String authKey,
                                                       @Body User user
    );

    /**
     * Get the quiz for the Identity Proof Test
     *
     * @param authKey
     * @param user          only required field here is the accessCode
     * @return {@link QuizObject}
     */
    @POST(API + VERSION + "/getQuiz")
    Call<ResponseBody> getQuiz(@Header("authKey") String authKey,
                                   @Body User user
    );

    /**
     * Verify the quiz results to complete the Identity test quiz
     *
     * @param authKey
     * @param verifyQuizObj Required fields include: accessCode, quizId, transactionId,
     *                      responseUniqueId, and an array of Answers objects.
     *                      {@link hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj.Answer}
     * @return {@link SimpleResponse}
     */
    @POST(API + VERSION + "/verifyQuiz ")
    Call<ResponseBody> verifyQuiz(@Header("authKey") String authKey,
                            @Body VerifyQuizObj verifyQuizObj
    );

    /**
     * Background Proof Step, this generates a criminal background report.
     *
     * @param authKey
     * @param userObj       only required value is accessCode
     * @return {@link SimpleResponse}
     */
    @POST(API + VERSION + "/generateCriminalReport ")
    Call<ResponseBody> generateCriminalReport(@Header("authKey") String authKey,
                                        @Body User userObj
    );

    /**
     * @param authKey
     * @param userObj       Access code is required. Other fields will be updated if included.
     *                      Fields that can be updated include: email, phone, firstName, lastName,
     *                      address, city, state, zipCode, state, month, day, year, ssn
     * @return {@link User}
     */
    @POST(API + VERSION + "/updateUser ")
    Call<ResponseBody> updateUser(@Header("authKey") String authKey,
                                @Body User userObj
    );

    /**
     * @param authKey
     * @param userObj       Access code is required.
     * @return {@link User}
     */
    @POST(API + VERSION + "/getUser ")
    Call<ResponseBody> getUser(@Header("authKey") String authKey,
                             @Body User userObj
    );

    /**
     * Upload and compare 2 photos.
     * @param authKey
     * @param uploadPhotosObj Required fields here are: accessCode and both img1 / img2. Note that
     *                        both of the images are baseEncoded64 strings.
     * @return
     */
    @POST(API + VERSION + "/comparePhotos")
    Call<ResponseBody> comparePhotos(@Header("authKey") String authKey,
                                           @Body UploadPhotosObj uploadPhotosObj
    );

    /**
     * Upload a front and back of an ID for identity proof verification
     * @param authKey
     * @param uploadPhotosObj Required fields here are: accessCode and both idFront / idBack. Note that
     *                        both of the images are baseEncoded64 strings.
     * @return
     */
    @POST(API + VERSION + "/uploadId")
    Call<ResponseBody> uploadId(@Header("authKey") String authKey,
                                     @Body UploadPhotosObj uploadPhotosObj
    );

    /**
     * Upload a picture of a passport for the verification process. Note that only the front (The
     * portion with the data, usually on the first or second page) is required.
     * @param authKey
     * @param uploadPhotosObj Required fields here are: accessCode and idFront. Note that
     *                        the image is in a baseEncoded64 string format.
     * @return
     */
    @POST(API + VERSION + "/uploadPassport")
    Call<ResponseBody> uploadPassport(@Header("authKey") String authKey,
                                     @Body UploadPhotosObj uploadPhotosObj
    );


    /**
     * Upload a front and back of an ID for identity proof verification (enhanced, see
     * {https://docs.authenticating.com} docs for details)
     * @param authKey
     * @param uploadPhotosObj Required fields here are: accessCode and both idFront / idBack. Note that
     *                        both of the images are baseEncoded64 strings.
     * @return
     */
    @POST(API + VERSION + "/uploadIdEnhanced")
    Call<ResponseBody> uploadIdEnhanced(@Header("authKey") String authKey,
                                     @Body UploadPhotosObj uploadPhotosObj
    );

    /**
     * Check the status of the uploadId endpoint background operation.
     * @param authKey
     * @param user Required field is accessCode
     * @return {@link CheckPhotoResults}
     */
    @POST(API + VERSION + "/checkUploadId")
    Call<ResponseBody> checkUploadId(@Header("authKey") String authKey,
                                                @Body User user
    );

    /**
     * Check the status of the uploadPassport endpoint background operation.
     * @param authKey
     * @param user Required field is accessCode
     * @return {@link CheckPhotoResults}
     */
    @POST(API + VERSION + "/checkUploadPassport")
    Call<ResponseBody> checkUploadPassport(@Header("authKey") String authKey,
                                           @Body User user
    );

}
