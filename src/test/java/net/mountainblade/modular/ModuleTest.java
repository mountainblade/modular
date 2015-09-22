/**
 * Copyright (C) 2014-2015 MountainBlade (http://mountainblade.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mountainblade.modular;

import com.google.common.base.Stopwatch;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import net.mountainblade.modular.examples.Example2Module;
import net.mountainblade.modular.examples.Example2ModuleImpl;
import net.mountainblade.modular.examples.ExampleModule;
import net.mountainblade.modular.filters.AnnotationPresent;
import net.mountainblade.modular.filters.InstanceOf;
import net.mountainblade.modular.filters.Not;
import net.mountainblade.modular.impl.BaseModuleManager;
import net.mountainblade.modular.impl.DefaultModuleManager;
import net.mountainblade.modular.impl.HierarchicModuleManager;
import net.mountainblade.modular.junit.Repeat;
import net.mountainblade.modular.junit.RepeatRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
        BaseModuleManager.blacklist("demo");
        final DefaultModuleManager manager = new DefaultModuleManager();

        // Load modules from our current package and "below"
        final Collection<Module> modules = manager.loadModules(packageName);

        // Stop the clock
        stopwatch.stop();
        System.out.println("Loading modules took " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

        // Check if we got the right modules
        final int size = modules.size();
        Assert.assertTrue("No modules loaded!", size > 0);
        Assert.assertEquals("Not all modules got loaded successfully!", manager.getRegistry().getModules().size(), size);
        Assert.assertEquals("Expected to see 3 modules loaded", 3, size);

        // Check module metadata / information
        ModuleInformation information = manager.getInformation(Example2Module.class).get();
        Assert.assertNotNull("Could not get information for module", information);
        Assert.assertEquals(ModuleState.READY, information.getState());
        Assert.assertEquals(Example2ModuleImpl.VERSION, information.getVersion().toString());
        Assert.assertArrayEquals(new String[]{Example2ModuleImpl.AUTHOR}, information.getAuthors());

        final Example2Module example2Module = manager.getModule(Example2Module.class).get();
        System.out.println("Random number of the moment: " + example2Module.getNumber());
        Assert.assertTrue(example2Module instanceof Example2ModuleImpl);
        Assert.assertTrue(((Example2ModuleImpl) example2Module).wasSuccessful());

        // Check if we didn't get any new modules this time (two instances are not allowed)
        final Collection<Module> newModules = manager.loadModules(packageName);
        Assert.assertEquals(size, newModules.size());

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
        Assert.assertEquals(4, hierarchicManager.getRegistry().getModules(ModuleState.READY).size());

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
        final URL resource = getDemoJar();
        if (resource == null) {
            return;
        }

        final Collection<Module> modules = manager.loadModules(resource.toURI(), PathHelper.near(Module.class));
        Assert.assertEquals(1, modules.size());

        manager.shutdown();
    }

    @Test
    public void testJarsInFolder() throws Exception {
        final DefaultModuleManager manager = new DefaultModuleManager();
        final URL jar = getDemoJar();
        if (jar == null) {
            return;
        }

        final Collection<Module> modules = manager.loadModules(PathHelper.inside(jar), "net.");
        Assert.assertEquals(1, modules.size());

        manager.shutdown();
    }

    private URL getDemoJar() {
        final URL resource = getClass().getResource("/modular-demo-1.0-SNAPSHOT.jar");
        if (resource == null) {
            getLogger().warning("couldn't find jar, be sure to run \"mvn package\" on the demo first");
        }

        return resource;
    }

    @Test
    public void testFilter() throws Exception {
        final DefaultModuleManager manager = new DefaultModuleManager();
        final Collection<Module> modules = manager.loadModules("",
                new AnnotationPresent(ItsAKeeper.class),
                new Not(new InstanceOf(IgnoreMe.class)));

        for (Module module : modules) {
            Assert.assertTrue("The Module is a spy!", module.getClass().equals(Example3Module.class));
        }

        Assert.assertEquals(1, modules.size());
        manager.shutdown();
    }

    @Test
    public void testClassFiles() throws Exception {
        final File rootFolder = new File(getClass().getResource("/").getFile()).getParentFile().getParentFile();
        final File demoFolder = new File(rootFolder, "demo");
        final File targetFolder = new File(demoFolder, "target");
        if (!targetFolder.exists()) {
            getLogger().warning("Cannot test class files without having the modular demo compiled beforehand");
            return;
        }

        final DefaultModuleManager manager = new DefaultModuleManager();
        final File classesFolder = new File(targetFolder, "classes");
        final Collection<Module> modules = manager.loadModules(classesFolder);
        Assert.assertEquals(1, modules.size());
    }

    @Test
    public void testInjections() throws Exception {
        final DefaultModuleManager manager = new DefaultModuleManager();
        manager.getInjector().inject(String.class).marked(new Author() {
            @Override
            public String value() {
                return "John Doe";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Author.class;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Author)) {
                    return false;
                }

                Author other = (Author) o;
                return value().equals(other.value());
            }
        }).with("john@doe.net");

        final Example4Module module = manager.loadModule(Example4Module.class);
        Assert.assertNotNull(module.email);
    }

    private Logger getLogger() {
        return Logger.getLogger(getClass().getName());
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

    @Implementation
    public static class Example4Module implements Module {
        @Inject
        private Logger logger;

        @Inject
        @Author("John Doe")
        private String email;

        @Initialize
        private void init() {
            logger.info("Email address of John Doe is " + email);
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ItsAKeeper {
        // yay
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Author {
        String value();
    }

    public interface IgnoreMe {
        // boo
    }

}
