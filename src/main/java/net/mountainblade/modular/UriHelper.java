package net.mountainblade.modular;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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

    public static URI of(Package pkg) {
        return of(pkg.getName() + ".*");
    }

    public static URI nearAndBelow(Class aClass) {
        return of(aClass.getPackage().getName() + ".**");
    }

    public static URI nearAndBelow(Package pkg) {
        return of(pkg.getName() + ".**");
    }

    public static URI folderOf(String file, String... of) {
        return file(new File(file).getParentFile(), of);
    }

    public static URI file(String file, String... of) {
        return file(new File(file), of);
    }

    public static URI file(File file, String... of) {
        final URI uri = file.toURI();

        if (of.length > 0) {
            try {
                return new URI("file", null, uri.getSchemeSpecificPart(), of(of[0]).getSchemeSpecificPart());

            } catch (URISyntaxException ignore) {
                // Silently fail
            }
        }

        return uri;
    }

    public static boolean matchesScheme(URI uri, String scheme) {
        return uri != null && uri.getScheme().equals(scheme);
    }

    public static Pattern createPattern(URI uri) {
        String scheme = uri.getScheme().equalsIgnoreCase("file") ? uri.getFragment() : uri.getSchemeSpecificPart();
        return scheme == null ? null : Pattern.compile(
                // Account for fragmented stuff
                scheme.substring(scheme.startsWith("//") ? 2 : 0)

                // Translate classpath wildcards to regex
                .replace("**", ".+")
                .replace("*", "[^\\.]*"));
    }

}
