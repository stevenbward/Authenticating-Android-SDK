package hotb.pgmacdesign.authenticatingsdk.datamodels;

import android.graphics.Bitmap;

import com.google.gson.annotations.SerializedName;

import hotb.pgmacdesign.authenticatingsdk.interfaces.OnTaskCompleteListener;

/**
 * Used in the comparePhotos() endpoint.
 * {@link hotb.pgmacdesign.authenticatingsdk.networking.AuthenticatingAPICalls#comparePhotos(OnTaskCompleteListener, String, String, Bitmap, Bitmap)}
 * Created by pmacdowell on 2017-08-14.
 */

public class UploadPhotosObj {

    @SerializedName("accessCode")
    private String accessCode;
    @SerializedName("img1")
    private String img1;
    @SerializedName("img2")
    private String img2;
    public void setImg1(String img1) {
        this.img1 = img1;
    }

    public void setImg2(String img2) {
        this.img2 = img2;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }


}
