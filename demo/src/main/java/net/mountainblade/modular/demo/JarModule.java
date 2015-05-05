package net.mountainblade.modular.demo;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.*;

import java.util.logging.Logger;

/**
 * Represents a module that is loaded using its jar file.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(authors = "spaceemotion", version = "1.0.2")
public class JarModule implements Module {
    @Inject
    private Logger logger;


    @Initialize
    public void initialize() {
        logger.info("Hello world!");
    }

    @Shutdown
    public void shutdown() {
        logger.info("Bye world!");
    }

}
