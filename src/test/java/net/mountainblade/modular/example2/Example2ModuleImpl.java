package net.mountainblade.modular.example2;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;

import java.util.Arrays;
import java.util.Random;

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

    @Inject
    private ModuleManager moduleManager;


    @Initialize
    public void init() {
        System.out.println("Hi from an interface-implementation module!");

        ModuleInformation information = moduleManager.getInformation(Example2Module.class).get();
        System.out.println(
                "This module was created by " + Arrays.toString(information.getAuthors()) +
                " and has is running version " + information.getVersion()
        );
    }

    @Override
    public int calculateRandomNumber() {
        return new Random().nextInt();
    }

}
