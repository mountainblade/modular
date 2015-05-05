package net.mountainblade.modular.demo;

import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;

import java.util.logging.Logger;

/**
 * Represents a module that is loaded using its jar file.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(authors = "spaceemotion")
public class JarModule {
    @Inject
    private Logger logger;


    @Initialize
    public void initialize() {
        logger.info("Hello world!");
    }

}
