package net.mountainblade.modular.impl;

import lombok.extern.java.Log;
import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.Implementation;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

/**
 * Represents a loader for modules.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
public class ModuleLoader {
    private final ClassWorld classWorld;
    private final Injector injector;


    public ModuleLoader(ClassWorld classWorld, ModuleRegistry registry) {
        this.classWorld = classWorld;
        this.injector = new Injector(registry);
    }


    public Module loadModule(Class<? extends Module> moduleClass) {
        // Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            // Instantiate module
            Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            Module module = constructor.newInstance();

            // Set to loading
            // TODO

            // Inject dependencies
            injector.inject(module);

            // Set to loaded
            // TODO

            return module;

        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, "Could not find module constructor", e);

        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.log(Level.WARNING, "Could not instantiate module implementation", e);

        } catch (InjectFailedException e) {
            log.log(Level.WARNING, "Could not load module implementation", e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Module> loadModuleClass(ClasspathLocation location, String className) {
        try {
            Class<?> aClass = loadClass(location, className);
            Implementation annotation = aClass.getAnnotation(Implementation.class);

            if (annotation == null) {
                return null;
            }

            // TODO some more stuff in the future

            return (Class<? extends Module>) aClass;

        } catch (ClassNotFoundException e) {
            log.log(Level.WARNING, "Could not load class properly", e);
        }

        return null;
    }

    public Class<?> loadClass(ClasspathLocation location, String className) throws ClassNotFoundException {
        if (location != null) {
            try {
                return classWorld.getRealm(location.getRealm()).loadClass(className);

            } catch (ClassNotFoundException | NoSuchRealmException e) {
                log.log(Level.WARNING, "Could not load class from realm properly", e);
            }
        }

        return getClass().getClassLoader().loadClass(className);
    }

}
