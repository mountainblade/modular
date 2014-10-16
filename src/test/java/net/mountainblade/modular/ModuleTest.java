package net.mountainblade.modular;

import com.google.common.base.Stopwatch;
import net.mountainblade.modular.example2.Example2Module;
import net.mountainblade.modular.example2.Example2ModuleImpl;
import net.mountainblade.modular.impl.ModuleManagerImpl;
import net.mountainblade.modular.junit.Repeat;
import net.mountainblade.modular.junit.RepeatRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
    @Repeat(10)
    public void testModules() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();

        // Create new manager
        ModuleManager manager = new ModuleManagerImpl();

        // Load modules from our current package and "below"
        Collection<Module> modules = manager.loadModules(UriHelper.nearAndBelow(ModuleManager.class));

        // Stop the clock
        stopwatch.stop();
        System.out.println("Loading modules took " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

        // Check if we got the right modules
        Assert.assertTrue("No modules loaded!", modules.size() > 0);
        Assert.assertEquals("Not all modules got loaded successfully!", modules.size(), manager.getModules().size());
        Assert.assertEquals("Expected to see 2 modules loaded", modules.size(), 2);

        // Check module metadata / information
        ModuleInformation information = manager.getInformation(Example2Module.class).get();
        Assert.assertNotNull("Could not get information for module", information);
        Assert.assertEquals(information.getState(), ModuleState.READY);
        Assert.assertEquals(information.getVersion(), Example2ModuleImpl.VERSION);
        Assert.assertArrayEquals(information.getAuthors(), new String[]{Example2ModuleImpl.AUTHOR});

        System.out.println("Random number of the moment: " + manager.getModule(Example2Module.class).get()
                .calculateRandomNumber());

        // And shut down the systems
        manager.shutdown();
    }

}
