package net.mountainblade.modular;

import net.mountainblade.modular.example.ExampleModule;
import net.mountainblade.modular.example2.Example2Module;
import net.mountainblade.modular.example2.Example2ModuleImpl;
import net.mountainblade.modular.impl.ModuleManagerImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

/**
 * Represents the ModuleTest.
 *
 * @author spaceemotion
 * @version 1.0
 */
@RunWith(JUnit4.class)
public class ModuleTest {

    @Test
    public void testModules() throws Exception {
        // Create new manager
        ModuleManager moduleManager = new ModuleManagerImpl();

        // Load modules from our current package and "below"
        Collection<Module> modules = moduleManager.loadModules(UriHelper.nearAndBelow(ModuleManager.class));

        // Check if we got the right modules
        Assert.assertEquals("No modules loaded!", modules.size(), 2);
        Assert.assertTrue(modules.iterator().next() instanceof ExampleModule);

        // Check module metadata / information
        ModuleInformation information = moduleManager.getInformation(Example2Module.class).get();
        Assert.assertNotNull("Could not get information for module", information);
        Assert.assertEquals(information.getState(), ModuleState.READY);
        Assert.assertEquals(information.getVersion(), Example2ModuleImpl.VERSION);
        Assert.assertArrayEquals(information.getAuthors(), new String[]{Example2ModuleImpl.AUTHOR});

        // And shut down the systems
        moduleManager.shutdown();
    }

}
