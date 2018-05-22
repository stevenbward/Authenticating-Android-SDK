package hotb.pgmacdesign.authenticatingsdk.networking;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import hotb.pgmacdesign.authenticatingsdk.datamodels.AuthenticatingException;
import hotb.pgmacdesign.authenticatingsdk.datamodels.AvailableNetworks;
import hotb.pgmacdesign.authenticatingsdk.datamodels.CheckPhotoResults;
import hotb.pgmacdesign.authenticatingsdk.datamodels.PhoneVerification;
import hotb.pgmacdesign.authenticatingsdk.datamodels.QuizObject;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SimpleResponse;
import hotb.pgmacdesign.authenticatingsdk.datamodels.SocialNetworkObj;
import hotb.pgmacdesign.authenticatingsdk.datamodels.User;
import hotb.pgmacdesign.authenticatingsdk.datamodels.VerifyQuizObj;

/**
 * Created by pmacdowell on 2017-07-13.
 */

public class AuthenticatingConstants {

    public static final String BASE_URL = "https://api.authenticating.com/";

    ///////////////
    //Public Tags//
    ///////////////

    public static final int TAG_SIMPLE_RESPONSE = 19000;
    public static final int TAG_ERROR_RESPONSE = 19001;
    public static final int TAG_AVAILABLE_NETWORKS = 19002;
    public static final int TAG_USER = 19003;
    public static final int TAG_QUIZ_QUESTIONS = 19004;
    public static final int TAG_CHECK_PHOTO_RESULT = 19005;

    //////////////
    //Local Tags//
    //////////////

    static final int TAG_UPLOAD_PHOTO_OBJECT = 5050;

    /////////
    //Types//
    /////////

    //Custom type converters
    static final Type TYPE_USER = new TypeToken<User>() {}.getType();
    static final Type TYPE_SOCIAL_NETWORK_OBJ = new TypeToken<SocialNetworkObj>() {}.getType();
    static final Type TYPE_AUTHENTICATING_ERROR = new TypeToken<AuthenticatingException>() {}.getType();
    static final Type TYPE_SIMPLE_RESPONSE = new TypeToken<SimpleResponse>() {}.getType();
    static final Type TYPE_AVAILABLE_NETWORKS = new TypeToken<AvailableNetworks>() {}.getType();
    static final Type TYPE_PHONE_VERIFICATION = new TypeToken<PhoneVerification>() {}.getType();
    static final Type TYPE_VERIFY_QUIZ_OBJ = new TypeToken<VerifyQuizObj>() {}.getType();
    static final Type TYPE_QUIZ_QUESTIONS = new TypeToken<QuizObject>() {}.getType();
    static final Type TYPE_CHECK_PHOTO_RESULT = new TypeToken<CheckPhotoResults>() {}.getType();

    /**
     * 2 Megabyte cap on incoming images. Larger ones will be resized down. Anything <2mb will be
     * sent in without any image pre-processing
     */
    public static final float MAX_SIZE_IMAGE_UPLOAD = 2000000;

}
