package hotb.pgmacdesign.authenticatingsdk.datamodels;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to return the list of networks options that are available for the
 * user to verify / pass their social network test.
 * Created by pmacdowell on 2017-07-24.
 */
public class AvailableNetworksHeader extends TopLevelObj{

    @SerializedName("data")
    private List<String> networkOptions;

    public List<String> getNetworkOptions() {
        if(networkOptions == null){
            networkOptions = new ArrayList<>();
        }
        return networkOptions;
    }

    public void setNetworkOptions(List<String> networkOptions) {
        this.networkOptions = networkOptions;
    }
}
