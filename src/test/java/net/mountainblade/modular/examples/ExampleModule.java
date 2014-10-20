package net.mountainblade.modular.examples;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.*;

/**
 * Represents the ExampleModule.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation
public class ExampleModule implements Module {
    public static final String PREFIX = '[' + ExampleModule.class.getSimpleName() + "] ";

    @Inject
    private Example2Module example2Module;


    @Initialize
    public void init(ModuleManager moduleManager) {
        System.out.println(PREFIX + "Hello World, using a lightweight module system!");
        System.out.println(PREFIX + "Dynamically injected method parameter: " + moduleManager);
        System.out.println(PREFIX + "Dynamically injected field: " + example2Module);
    }

    @Shutdown
    public void destroy() {
        System.out.println(PREFIX + "Bye bye");
    }

}
