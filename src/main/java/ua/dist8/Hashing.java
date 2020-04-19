package ua.dist8;

public class Hashing {

    /**
     * This function will return an Integer hash value, calculated from a String.
     * @param string the hash value will be calculated from this string.
     * @return hashvalue.
     */
    public static Integer createHash(String string){
        return java.lang.Math.abs(string.hashCode() % 32768);
    }
}
