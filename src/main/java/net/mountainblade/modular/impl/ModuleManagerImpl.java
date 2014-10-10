package net.mountainblade.modular.impl;

import com.google.common.base.Optional;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.extern.java.Log;
import net.mountainblade.modular.*;
import net.mountainblade.modular.annotations.Shutdown;
import net.mountainblade.modular.impl.locator.ClassLocator;
import net.mountainblade.modular.impl.locator.ClasspathLocator;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Represents the default implementation of a {@link net.mountainblade.modular.ModuleManager}.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Log
public final class ModuleManagerImpl implements ModuleManager {
    private final Collection<Destroyable> destroyables;

    @Getter
    private final ModuleRegistry registry;

    private final ModuleLoader loader;

    @Getter
    private final Collection<ClassLocator> locators;

    private final ClassWorld classWorld;

    private final JarCache jarCache;


    public ModuleManagerImpl() {
        destroyables = new LinkedList<>();

        // Stuff we need
        classWorld = new ClassWorld();

        registry   = new ModuleRegistry();
        loader     = new ModuleLoader(classWorld, registry);
        locators   = new THashSet<>();
        jarCache   = new JarCache();

        // Add defaults
        getLocators().add(new ClasspathLocator());

        // Also register ourselves so other modules can use this as implementation via injection
        registry.addGhostModule(ModuleManager.class, this, new MavenModuleInformation());

        // Add destroyable objects for our shutdown
        destroyables.add(registry);
        destroyables.add(loader);
    }

    @Override
    public Collection<Module> loadModules(URI uri) {
        // Locate stuff from URI - using different providers (class pat, file, ...)
        Collection<ClasspathLocation> located = new LinkedList<>();

        for (ClassLocator locator : locators) {
            if (!locator.handles(uri)) {
                continue;
            }

            // Register all found locations
            for (ClasspathLocation location : locator.discover(uri)) {
                registerLocation(location);
                located.add(location);
            }
        }

        // Collect a bunch of classes that are Modules, plus the interface they're implementing
        Collection<ModuleLoader.ClassEntry> candidates = loader.resolveCandidatesWithPattern(
                (UriHelper.everything().equals(uri)) ? null :
                Pattern.compile((uri.getAuthority() + uri.getPath()).replace("**", ".+").replace("*", "[^\\.]*")),
                located, jarCache);

        // Topologically sort the list so we don't have trouble with dependencies
        // TODO

        // Instantiate the classes and inject their dependencies
        Collection<Module> modules = new LinkedList<>();

        for (ModuleLoader.ClassEntry candidate : candidates) {
            Module module = loader.loadModule(candidate);

            if (module != null) {
                modules.add(module);
            }
        }

        return modules;
    }

    @Override
    public final <M extends Module> Optional<M> getModule(Class<M> module) {
        return Optional.fromNullable(registry.getModule(module));
    }

    @Override
    public Optional<ModuleInformation> getInformation(Class<? extends Module> module) {
        return Optional.fromNullable(registry.getInformation(module));
    }

    @Override
    public void shutdown() {
        // Send shut down signal to modules
        for (Module module : registry.getModules()) {
            try {
                AnnotationHelper.callMethodWithAnnotation(module, Shutdown.class);

            } catch (IllegalAccessException | InvocationTargetException e) {
                log.log(Level.WARNING, "Could not invoke shutdown method on module: " + module, e);
            }

            // Set state to shutdown
            ModuleRegistry.Entry entry = registry.getEntry(loader.getClassEntry(module.getClass()).getModule());

            if (entry != null) {
                ModuleInformation information = entry.getInformation();

                if (information instanceof ModuleInformationImpl) {
                    ((ModuleInformationImpl) information).setState(ModuleState.SHUTDOWN);
                }
                continue;
            }

            log.warning("Unable to set state to shut down: Could not find entry for module: " + module);
        }

        // And destroy what we can
        for (Destroyable destroyable : destroyables) {
            destroyable.destroy();
        }
    }

    public void registerLocation(ClasspathLocation location) {
        try {
            classWorld.newRealm(location.getRealm(), getClass().getClassLoader()).addURL(location.getUrl());

        } catch (DuplicateRealmException ignore) {
            // Happens for classpath realms ...
        }
    }

}
