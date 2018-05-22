package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to return the list of networks options that are available for the
 * user to verify / pass their social network test.
 * Created by pmacdowell on 2017-07-24.
 */
public class AvailableNetworks extends TopLevelObj{

    @SerializedName("availableNetworks")
    private List<String> availableNetworks;

    public List<String> getAvailableNetworks() {
        if(availableNetworks == null){
            availableNetworks = new ArrayList<>();
        }
        return availableNetworks;
    }

    public void setAvailableNetworks(List<String> availableNetworks) {
        this.availableNetworks = availableNetworks;
    }
}
