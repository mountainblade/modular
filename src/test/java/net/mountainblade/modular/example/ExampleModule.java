package net.mountainblade.modular.example;

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
    @Inject
    private ModuleManager moduleManager;


    @Initialize
    public void init() {
        System.out.println("Hello World, using a lightweight module system!");
        System.out.println(moduleManager);
    }

    @Shutdown
    public void destroy() {
        System.out.println("Bye bye");
    }

}
