package net.mountainblade.modular;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Represents the UriHelper.
 *
 * @author spaceemotion
 * @version 1.0
 */
public final class UriHelper {
    public static final String PREFIX = "classpath://";


    private UriHelper() {
    }

    public static URI everything() {
        return of("**");
    }

    public static URI of(String pattern) {
        return URI.create(PREFIX + pattern);
    }

    public static URI of(Class<? extends Module> aClass) {
        return of(aClass.getCanonicalName());
    }

    public static URI nearAndBelow(Class aClass) {
        return of(aClass.getPackage().getName() + ".**");
    }

    public static boolean matchesScheme(URI uri, String scheme) {
        return uri != null && uri.getScheme().equals(scheme);
    }

    public static Pattern createPattern(URI uri) {
        return Pattern.compile((uri.getAuthority() + uri.getPath()).replace("**", ".+").replace("*", "[^\\.]*"));
    }

}
