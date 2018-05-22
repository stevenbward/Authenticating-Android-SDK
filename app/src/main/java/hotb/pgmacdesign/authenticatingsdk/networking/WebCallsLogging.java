package hotb.pgmacdesign.authenticatingsdk.networking;

/**
 * Logging class for printing JSON logging data.
 * Created by Patrick MacDowell (PGMacDesign) on 2016-11-16.
 */

public class WebCallsLogging {

    protected static boolean LOG_JSON_DATA;
    protected static boolean USER_CHANGED_LOGGING;

    private static WebCallsLogging instance = null;

    private WebCallsLogging() {
        LOG_JSON_DATA = false;
    }

    private static WebCallsLogging getInstance(){
        if(instance == null) {
            instance = new WebCallsLogging();
        }
        return instance;
    }

    /**
     * Set the logging data
     * @param setLogging True means logging will take place, false will mean no logging
     * @return
     */
    public static boolean setJsonLogging(boolean setLogging) {
        instance = getInstance();
        LOG_JSON_DATA = setLogging;
        USER_CHANGED_LOGGING = true;
        return setLogging;
    }

    public static boolean isJsonLogging(){
        return LOG_JSON_DATA;
    }

    static boolean didUserChangeLogging(){
        return USER_CHANGED_LOGGING;
    }
}
