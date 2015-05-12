package net.mountainblade.modular;

import java.io.File;
import java.net.URI;

/**
 * Represents the UriHelper.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class UriHelper {

    private UriHelper() {
        // Private constructor, this is a helper class
    }

    public static String nearAndBelow(Class aClass) {
        return aClass.getPackage().getName() + ".";
    }

    public static String nearAndBelow(Package pkg) {
        return pkg.getName() + ".";
    }

    public static URI folderOf(String file) {
        return new File(file).getParentFile().toURI();
    }

}
