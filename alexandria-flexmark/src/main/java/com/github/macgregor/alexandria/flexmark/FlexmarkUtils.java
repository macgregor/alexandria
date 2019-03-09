package com.github.macgregor.alexandria.flexmark;

import com.vladsch.flexmark.util.sequence.Range;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class FlexmarkUtils {

    public static boolean isUrl(String link){
        try {
            URL url = new URL(link);
            return true;
        } catch (MalformedURLException e) {}
        return false;
    }

    public static boolean isAbsolute(String link){
        try{
            return Paths.get(link).isAbsolute();
        } catch (InvalidPathException | NullPointerException ex) {
            return true;
        }
    }

    public static Range range(String chars, String subSequence){
        int start = chars.indexOf(subSequence);
        int end = start + subSequence.length();
        return new Range(start, end);
    }
}
