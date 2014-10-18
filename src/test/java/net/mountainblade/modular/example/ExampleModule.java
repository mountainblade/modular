package net.mountainblade.modular.example;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import net.mountainblade.modular.annotations.Shutdown;
import net.mountainblade.modular.example2.Example2Module;

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
