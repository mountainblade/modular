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
import java.net.URI;
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
    @Rule
    public RepeatRule repeatRule = new RepeatRule();


    @Test
    @Repeat(3) // Repeat because the topological sort is kind of random
    public void testModules() throws Exception {
        URI exampleUri = UriHelper.of(ExampleModule.class.getPackage());
        Stopwatch stopwatch = Stopwatch.createStarted();

        // Create new manager
        DefaultModuleManager manager = new DefaultModuleManager();

        // Load modules from our current package and "below"
        Collection<Module> modules = manager.loadModules(exampleUri);

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
        Collection<Module> newModules = manager.loadModules(exampleUri);
        Assert.assertEquals(modules.size(), newModules.size());

        Iterator<Module> first = modules.iterator();
        Iterator<Module> second = newModules.iterator();

        while (second.hasNext() || first.hasNext()) {
            Module nextFirst = first.hasNext() ? first.next() : null;
            Module nextSecond = second.hasNext() ? second.next() : null;

            Assert.assertNotNull("Next first is null", first);
            Assert.assertNotNull("Next second is null", second);
            Assert.assertEquals(nextFirst, nextSecond);
        }

        // Check if we can have multiple instances running
        ModuleManager manager2 = new DefaultModuleManager();
        manager2.provideSimple(manager.getModule(Example2Module.class).orNull());
        Assert.assertEquals(2, manager2.loadModules(UriHelper.of(ExampleModule.class)).size());

        // Also check if the hierarchical / inherited management is working
        HierarchicModuleManager hierarchicManager = new HierarchicModuleManager(manager);
        hierarchicManager.loadModules(UriHelper.of(Example3Module.class));

        Assert.assertFalse("The parent knows about the children!", manager.getModule(Example3Module.class).isPresent());

        // And shut down the systems (hierarchic also shuts down the normal one)
        hierarchicManager.shutdown(true);
        manager2.shutdown();
    }

    @Test
    public void testFilter() throws Exception {
        Collection<Module> modules = new DefaultModuleManager().loadModules(UriHelper.everything(),
                new Filter.AnnotationPresent(ItsAKeeper.class));

        for (Module module : modules) {
            Assert.assertTrue("The Module is a spy!", module.getClass().equals(Example3Module.class));
        }
    }

    /**
     * The third example module.
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
    public static @interface ItsAKeeper {
        // yay
    }

}
