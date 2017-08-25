package hotb.pgmacdesign.authenticatingsdk.networking;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworksHeader;
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
     * @param companyAPIKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode
     * @return {@link TopLevelObj}
     */
    @POST(API + VERSION + "/verifyPhone ")
    Call<SimpleResponseObj> verifyPhone(@Header("authKey") String companyAPIKey,
                                        @Body PhoneVerification phoneInfo
    );

    /**
     * Send an SMS/ Text MSG to the phone number attached to the user accessCode being sent.
     *
     * @param companyAPIKey API Key needed for calls
     * @param phoneInfo     Only required fields include: accessCode, smsCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyPhoneCode ")
    Call<SimpleResponseObj> verifyPhoneCode(@Header("authKey") String companyAPIKey,
                                            @Body PhoneVerification phoneInfo
    );

    /**
     * Verify a User's email
     *
     * @param companyAPIKey
     * @param user          Only requires accessCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyEmail ")
    Call<SimpleResponseObj> verifyEmail(@Header("authKey") String companyAPIKey,
                                        @Body UserHeader.User user
    );

    /**
     * Used for passing the verification of social networks
     *
     * @param companyAPIKey    API Key needed for calls
     * @param socialNetworkObj Only required fields include: accessCode, network,
     *                         socialMediaAccessToken, socialMediaUserId
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifySocialNetworks")
    Call<SimpleResponseObj> verifySocialNetworks(@Header("authKey") String companyAPIKey,
                                                 @Body SocialNetworkObj socialNetworkObj
    );

    /**
     * Get the available networks for use in Social Networking test
     *
     * @param companyAPIKey API Key needed for calls
     * @param user          Only required fields include: accessCode
     * @return {@link AvailableNetworksHeader}
     */
    @POST(API + VERSION + "/getAvailableNetworks")
    Call<AvailableNetworksHeader> getAvailableNetworks(@Header("authKey") String companyAPIKey,
                                                       @Body UserHeader.User user
    );

    /**
     * Get the quiz for the Identity Proof Test
     *
     * @param companyAPIKey
     * @param user          only required field here is the accessCode
     * @return {@link QuizObjectHeader}
     */
    @POST(API + VERSION + "/getQuiz")
    Call<QuizObjectHeader> getQuiz(@Header("authKey") String companyAPIKey,
                                   @Body UserHeader.User user
    );

    /**
     * Verify the quiz results to complete the Identity test quiz
     *
     * @param companyAPIKey
     * @param verifyQuizObj Required fields include: accessCode, quizId, transactionId,
     *                      responseUniqueId, and an array of Answers objects.
     *                      {@link hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj.Answer}
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/verifyQuiz ")
    Call<SimpleResponseObj> verifyQuiz(@Header("authKey") String companyAPIKey,
                            @Body VerifyQuizObj verifyQuizObj
    );

    /**
     * Background Proof Step, this generates a criminal background report.
     *
     * @param companyAPIKey
     * @param userObj       only required value is accessCode
     * @return {@link SimpleResponseObj}
     */
    @POST(API + VERSION + "/generateCriminalReport ")
    Call<SimpleResponseObj> generateCriminalReport(@Header("authKey") String companyAPIKey,
                                        @Body UserHeader.User userObj
    );

    /**
     * @param companyAPIKey
     * @param userObj       Access code is required. Other fields will be updated if included.
     *                      Fields that can be updated include: email, phone, firstName, lastName,
     *                      address, city, state, zipCode, state, month, day, year, ssn
     * @return {@link UserHeader}
     */
    @POST(API + VERSION + "/updateUser ")
    Call<UserHeader> updateUser(@Header("authKey") String companyAPIKey,
                                @Body UserHeader.User userObj
    );

    /**
     * @param companyAPIKey
     * @param userObj       Access code is required.
     * @return {@link UserHeader}
     */
    @POST(API + VERSION + "/getUser ")
    Call<UserHeader> getUser(@Header("authKey") String companyAPIKey,
                             @Body UserHeader.User userObj
    );

    /**
     * Upload and compare 2 photos.
     * @param companyAPIKey
     * @param uploadPhotosObj Required fields here are: accessCode and both img1 / img2. Note that
     *                        both of the images are baseEncoded64 strings.
     * @return
     */
    @POST(API + VERSION + "/comparePhotos")
    Call<SimpleResponseObj> comparePhotos(@Header("authKey") String companyAPIKey,
                                           @Body UploadPhotosObj uploadPhotosObj
    );

}
