package be.jorisg.ultrastarorganizer.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<File> getFilesByExtensions(File directory, String... extensions) {
        List<File> result = new ArrayList<>();
        for ( File file : directory.listFiles() ) {
            String ext = FilenameUtils.getExtension(file.getName());
            for ( String testExt : extensions ) {
                if ( ext.equalsIgnoreCase(testExt) ) {
                    result.add(file);
                    break;
                }
            }
        }
        return result;
    }

}
