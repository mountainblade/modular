package net.mountainblade.modular;

import com.google.common.base.Stopwatch;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import net.mountainblade.modular.examples.Example2Module;
import net.mountainblade.modular.examples.Example2ModuleImpl;
import net.mountainblade.modular.examples.ExampleModule;
import net.mountainblade.modular.impl.DefaultModuleManager;
import net.mountainblade.modular.impl.HierarchicModuleManager;
import net.mountainblade.modular.junit.Repeat;
import net.mountainblade.modular.junit.RepeatRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Represents the ModuleTest.
 *
 * @author spaceemotion
 * @version 1.0
 */
@RunWith(JUnit4.class)
public class ModuleTest {
    private static boolean ranOnce = false;

    @Rule
    public RepeatRule repeatRule = new RepeatRule();

    @Test
    @Repeat(3) // Repeat because the topological sort is kind of random
    public void testModules() throws Exception {
        final String packageName = ExampleModule.class.getPackage().getName();
        final Stopwatch stopwatch = Stopwatch.createStarted();

        // Create new manager
        final DefaultModuleManager manager = new DefaultModuleManager();

        // Load modules from our current package and "below"
        final Collection<Module> modules = manager.loadModules(packageName);

        // Stop the clock
        stopwatch.stop();
        System.out.println("Loading modules took " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

        // Check if we got the right modules
        Assert.assertTrue("No modules loaded!", modules.size() > 0);
        Assert.assertEquals("Not all modules got loaded successfully!", manager.getModules().size(), modules.size());
        Assert.assertEquals("Expected to see 2 modules loaded", 2, modules.size());

        // Check module metadata / information
        ModuleInformation information = manager.getInformation(Example2Module.class).get();
        Assert.assertNotNull("Could not get information for module", information);
        Assert.assertEquals(ModuleState.READY, information.getState());
        Assert.assertEquals(Example2ModuleImpl.VERSION, information.getVersion());
        Assert.assertArrayEquals(new String[]{Example2ModuleImpl.AUTHOR}, information.getAuthors());

        System.out.println("Random number of the moment: " + manager.getModule(Example2Module.class).get().getNumber());

        // Check if we didn't get any new modules this time (two instances are not allowed)
        final Collection<Module> newModules = manager.loadModules(packageName);
        Assert.assertEquals(modules.size(), newModules.size());

        final Iterator<Module> first = modules.iterator();
        final Iterator<Module> second = newModules.iterator();

        while (second.hasNext() || first.hasNext()) {
            Module nextFirst = first.hasNext() ? first.next() : null;
            Module nextSecond = second.hasNext() ? second.next() : null;

            Assert.assertNotNull("Next first is null", first);
            Assert.assertNotNull("Next second is null", second);
            Assert.assertEquals(nextFirst, nextSecond);
        }

        // Check if we can have multiple instances running
        final ModuleManager manager2 = new DefaultModuleManager();
        manager2.provideSimple(manager.getModule(Example2Module.class).orNull());
        Assert.assertNotNull(manager2.loadModule(ExampleModule.class));

        // Also check if the hierarchical / inherited management is working
        final HierarchicModuleManager hierarchicManager = new HierarchicModuleManager(manager);
        Assert.assertNotNull(hierarchicManager.loadModule(Example3Module.class));
        Assert.assertFalse("The parent knows about the children!", manager.getModule(Example3Module.class).isPresent());

        // And shut down the systems (hierarchic also shuts down the normal one)
        System.err.println("--- Shutting down " + (ranOnce ? "with" : "without") + " parent ---");
        hierarchicManager.shutdown(ranOnce);

        System.err.println("--- Shutting down default manager ---");
        manager2.shutdown();

        // Set state for the next time
        ranOnce = !ranOnce;
    }

    @Test
    public void testJars() throws Exception {
        final DefaultModuleManager manager = new DefaultModuleManager();

        final URL resource = this.getClass().getResource("/modular-demo-1.0-SNAPSHOT.jar");
        Assert.assertNotNull("couldn't find jar, be sure to run \"mvn package\" on the demo project first", resource);

        final Collection<Module> modules = manager.loadModules(UriHelper.folderOf(resource.getFile()), "net.");
        Assert.assertEquals(1, modules.size());

        manager.shutdown();
    }

    @Test
    public void testFilter() throws Exception {
        final DefaultModuleManager manager = new DefaultModuleManager();
        final Collection<Module> modules = manager.loadModules("", new Filter.AnnotationPresent(ItsAKeeper.class));

        for (Module module : modules) {
            Assert.assertTrue("The Module is a spy!", module.getClass().equals(Example3Module.class));
        }

        Assert.assertEquals(1, modules.size());
    }

    /**
     * The third example module, this time in-lined.
     *
     * @author spaceemotion
     * @version 1.0
     */
    @Implementation
    @ItsAKeeper
    public static class Example3Module implements Module {
        @Inject
        private Logger logger;


        @Initialize
        private void init() {
            logger.info("I have been loaded inside an inherited manager, the parent does not know about me!");
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ItsAKeeper {
        // yay
    }

}
