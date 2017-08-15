package hotb.pgmacdesign.authenticatingsdk.networking;

/**
 * Created by pmacdowell on 8/12/2016.
 */
class StringUtilities {
    /**
     * Returns just the numbers of a String
     * @param s Charsequence to analyze
     * @return String, containing only numbers
     */
     static String keepNumbersOnly(CharSequence s) {
        try {
            return s.toString().replaceAll("[^0-9]", "");
        } catch (Exception e){
            return null;
        }
    }

    /**
     * Checks if the passed String is null or empty
     * @param t object to check
     * @return boolean, true if it is null or empty, false if it is not.
     */
     static <T> boolean isNullOrEmpty(T t){
        if(t == null){
            return true;
        }
        String str = t.toString();
        if(str.isEmpty()){
            return true;
        }
        if(str.length() == 0){
            return true;
        }
        return false;
    }

    /**
     * Checks if the passed String is null or empty
     * @param str String to check
     * @return Boolean, true if it is null or empty, false if it is not.
     */
     static boolean isNullOrEmpty(String str){
        if(str == null){
            return true;
        }
        if(str.isEmpty()){
            return true;
        }
        if(str.length() == 0){
            return true;
        }
        if(str.equalsIgnoreCase(" ")){
            return true;
        }
        return false;
    }
}

