package net.mountainblade.modular.example2;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Represents an implementation for the second example module.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(version = Example2ModuleImpl.VERSION, authors = {Example2ModuleImpl.AUTHOR})
public class Example2ModuleImpl implements Example2Module {
    public static final String AUTHOR = "spaceemotion";
    public static final String VERSION = "1.0.2a";

    @Inject
    private Logger log;

    @Inject
    private ModuleInformation information;


    @Initialize
    public void init(ModuleManager moduleManager) {
        log.info("Hi from an interface-implementation module!");
        log.info("This module was created by " + Arrays.toString(information.getAuthors()) + " and is running version "
                        + information.getVersion());
    }

    @Override
    public int getNumber() {
        return new Random().nextInt();
    }

}
