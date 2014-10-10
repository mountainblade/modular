package net.mountainblade.modular.impl;

import net.mountainblade.modular.Module;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class MavenModuleInformation extends ModuleInformationImpl {
    private static final String PATH = "/META-INF/maven/" + Module.class.getPackage().getName() + "/pom.properties";


    MavenModuleInformation() {
        super(getVersionFromMaven());
    }

    private synchronized static String getVersionFromMaven() {
        String version = null;

        // try to load from maven properties first
        try (InputStream is = MavenModuleInformation.class.getResourceAsStream(PATH)) {
            Properties p = new Properties();

            if (is != null) {
                p.load(is);

                version = p.getProperty("version", null);
            }

        } catch (IOException ignore) {
            // Ignore
        }

        // Fallback to using Java API
        if (version == null) {
            Package aPackage = MavenModuleInformation.class.getPackage();

            if (aPackage != null) {
                version = aPackage.getImplementationVersion();

                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        return version == null ? "unknown" : version;
    }

}
