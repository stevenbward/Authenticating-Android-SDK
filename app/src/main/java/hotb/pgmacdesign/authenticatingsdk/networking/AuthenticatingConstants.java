package hotb.pgmacdesign.authenticatingsdk.networking;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworksHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResultsHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObjectHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponseObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.UserHeader;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.AuthenticatingException;
import okhttp3.MediaType;

/**
 * Created by pmacdowell on 2017-07-13.
 */

public class AuthenticatingConstants {

    public static final String BASE_URL = "https://api.authenticating.com/";

    public static final int TAG_SIMPLE_RESPONSE_OBJ = 19000;
    public static final int TAG_ERROR_RESPONSE = 19001;
    public static final int TAG_AVAILABLE_NETWORKS = 19002;
    public static final int TAG_LIST_OF_STRINGS = 19003;
    public static final int TAG_USER_HEADER = 19004;
    public static final int TAG_QUIZ_QUESTIONS_HEADER = 19005;
    public static final int TAG_UPLOAD_PHOTO_OBJECT = 19006;
    public static final int TAG_CHECK_PHOTO_RESULT_OBJECT = 19007;



    /////////
    //Types//
    /////////

    //Custom type converters
    protected static final Type TYPE_USER_HEADER = new TypeToken<UserHeader>() {
    }.getType();
    protected static final Type TYPE_USER = new TypeToken<UserHeader.User>() {
    }.getType();
    protected static final Type TYPE_SOCIAL_NETWORK_OBJ = new TypeToken<SocialNetworkObj>() {
    }.getType();
    protected static final Type TYPE_AUTHENTICATING_ERROR = new TypeToken<AuthenticatingException>() {
    }.getType();
    protected static final Type TYPE_SIMPLE_RESPONSE = new TypeToken<SimpleResponseObj>() {
    }.getType();
    protected static final Type TYPE_AVAILABLE_NETWORKS = new TypeToken<AvailableNetworksHeader>() {
    }.getType();
    protected static final Type TYPE_PHONE_VERIFICATION = new TypeToken<PhoneVerification>() {
    }.getType();
    protected static final Type TYPE_VERIFY_QUIZ_OBJ = new TypeToken<VerifyQuizObj>() {
    }.getType();
    protected static final Type TYPE_QUIZ_QUESTIONS_HEADER = new TypeToken<QuizObjectHeader>() {
    }.getType();
    protected static final Type TYPE_CHECK_PHOTO_RESULTS_HEADER = new TypeToken<CheckPhotoResultsHeader>() {
    }.getType();

    //2 MB
    protected static final float MAX_SIZE_IMAGE_UPLOAD = 2000000;

    //////////////////////////////////////
    //MediaTypes (For Multipart Uploads)//
    //////////////////////////////////////

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/*");
    public static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain");
}
