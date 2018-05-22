package hotb.pgmacdesign.authenticatingsdk.interfaces;

/**
 * Listener for sending back data. The int custom tag is used for identifying
 * what is being sent back. For the list of used tags see
 * {@link hotb.pgmacdesign.authenticatingsdk.networking.AuthenticatingConstants}
 * Created by pmacdowell on 8/12/2016.
 */
public interface OnTaskCompleteListener {
    public void onTaskComplete(Object result, int customTag);
}
