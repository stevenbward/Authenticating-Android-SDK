package hotb.pgmacdesign.authenticatingsdk.networking;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworksHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResultsHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObjectHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponseObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.TopLevelObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UploadPhotosObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UserHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by pmacdowell on 2017-07-13.
 */

public interface APIService {

    //Link to documentation:
    //www.???????????????????????????.com

    //Endpoint + Version Strings
    public static final String API = "/api";
    public static final String VERSION = "/v1";

    /**
     * Send an SMS/ Text MSG to the phone number attached to the user accessCode being sent.
     *
     * @param authKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode
     * @return {@link TopLevelObj}
     */
    @POST(API + VERSION + "/verifyPhone ")
    Call<SimpleResponseObj> verifyPhone(@Header("authKey") String authKey,
                                        @Body PhoneVerification phoneInfo
    );

    /**
     * Send an SMS/ Text MSG to the phone number attached to the user accessCode being sent.
     *
     * @param authKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode, smsCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyPhoneCode ")
    Call<SimpleResponseObj> verifyPhoneCode(@Header("authKey") String authKey,
                                            @Body PhoneVerification phoneInfo
    );

    /**
     * Verify a User's email
     *
     * @param authKey
     * @param user          Only requires accessCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyEmail ")
    Call<SimpleResponseObj> verifyEmail(@Header("authKey") String authKey,
                                        @Body UserHeader.User user
    );

    /**
     * Used for passing the verification of social networks
     *
     * @param authKey    API Key needed for calls
     * @param socialNetworkObj Only required fields include: accessCode, network,
     *                         socialMediaAccessToken, socialMediaUserId
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifySocialNetworks")
    Call<SimpleResponseObj> verifySocialNetworks(@Header("authKey") String authKey,
                                                 @Body SocialNetworkObj socialNetworkObj
    );

    /**
     * Get the available networks for use in Social Networking test
     *
     * @param authKey API Key needed for calls
     * @param user          Only required fields include: accessCode
     * @return {@link AvailableNetworksHeader}
     */
    @POST(API + VERSION + "/getAvailableNetworks")
    Call<AvailableNetworksHeader> getAvailableNetworks(@Header("authKey") String authKey,
                                                       @Body UserHeader.User user
    );

    /**
     * Get the quiz for the Identity Proof Test
     *
     * @param authKey
     * @param user          only required field here is the accessCode
     * @return {@link QuizObjectHeader}
     */
    @POST(API + VERSION + "/getQuiz")
    Call<QuizObjectHeader> getQuiz(@Header("authKey") String authKey,
                                   @Body UserHeader.User user
    );

    /**
     * Verify the quiz results to complete the Identity test quiz
     *
     * @param authKey
     * @param verifyQuizObj Required fields include: accessCode, quizId, transactionId,
     *                      responseUniqueId, and an array of Answers objects.
     *                      {@link hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj.Answer}
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyQuiz ")
    Call<SimpleResponseObj> verifyQuiz(@Header("authKey") String authKey,
                            @Body VerifyQuizObj verifyQuizObj
    );

    /**
     * Background Proof Step, this generates a criminal background report.
     *
     * @param authKey
     * @param userObj       only required value is accessCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/generateCriminalReport ")
    Call<SimpleResponseObj> generateCriminalReport(@Header("authKey") String authKey,
                                        @Body UserHeader.User userObj
    );

    /**
     * @param authKey
     * @param userObj       Access code is required. Other fields will be updated if included.
     *                      Fields that can be updated include: email, phone, firstName, lastName,
     *                      address, city, state, zipCode, state, month, day, year, ssn
     * @return {@link UserHeader}
     */
    @POST(API + VERSION + "/updateUser ")
    Call<UserHeader> updateUser(@Header("authKey") String authKey,
                                @Body UserHeader.User userObj
    );

    /**
     * @param authKey
     * @param userObj       Access code is required.
     * @return {@link UserHeader}
     */
    @POST(API + VERSION + "/getUser ")
    Call<UserHeader> getUser(@Header("authKey") String authKey,
                             @Body UserHeader.User userObj
    );

    /**
     * Upload and compare 2 photos.
     * @param authKey
     * @param uploadPhotosObj Required fields here are: accessCode and both img1 / img2. Note that
     *                        both of the images are baseEncoded64 strings.
     * @return
     */
    @POST(API + VERSION + "/comparePhotos")
    Call<SimpleResponseObj> comparePhotos(@Header("authKey") String authKey,
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
    Call<SimpleResponseObj> uploadId(@Header("authKey") String authKey,
                                     @Body UploadPhotosObj uploadPhotosObj
    );


    /**
     * Upload a front and back of an ID for identity proof verification
     * @param authKey
     * @param user Required field is accessCode
     * @return {@link CheckPhotoResultsHeader}
     */
    @POST(API + VERSION + "/checkUploadId")
    Call<CheckPhotoResultsHeader> checkUploadId(@Header("authKey") String authKey,
                                                @Body UserHeader.User user
    );

}
