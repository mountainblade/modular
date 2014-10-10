package net.mountainblade.modular.example2;

import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;

/**
 * Represents an implementation for the second example module.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(
        version = Example2ModuleImpl.VERSION,
        authors = {Example2ModuleImpl.AUTHOR}
)
public class Example2ModuleImpl implements Example2Module {
    public static final String AUTHOR = "spaceemotion";
    public static final String VERSION = "1.0.2a";


    @Initialize
    public void init() {
        System.out.println("Hi from an interface-implementation module!");
    }

}
