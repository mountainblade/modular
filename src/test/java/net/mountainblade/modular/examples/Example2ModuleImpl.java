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
package net.mountainblade.modular.examples;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleManager;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Represents an implementation for the second example module.
 *
 * @author spaceemotion
 * @version 1.0
 */
@Implementation(version = Example2ModuleImpl.VERSION, authors = {Example2ModuleImpl.AUTHOR})
public class Example2ModuleImpl implements Example2Module {
    public static final String AUTHOR = "spaceemotion";
    public static final String VERSION = "1.0.2-alpha";

    @Inject
    private Logger log;

    @Inject
    private ModuleInformation local;

    @Inject(from = Example3Module.class)
    private ModuleInformation remote;

    private boolean successfulStart;


    @Initialize
    public void init(ModuleManager moduleManager) {
        Assert.assertNotNull(log);
        log.info("Hi from an interface-implementation module!");

        Assert.assertNotNull(local);
        Assert.assertNotNull(remote);
        Assert.assertNotEquals(local, remote);
        Assert.assertArrayEquals(local.getAuthors(), new String[]{Example2ModuleImpl.AUTHOR});
        Assert.assertEquals(local.getVersion().toString(), Example2ModuleImpl.VERSION);

        log.info("This module is currently " + local.getState() + ", was created by " +
                Arrays.toString(local.getAuthors()) + " and is running version " + local.getVersion());
        log.info("The first module is currently " + remote.getState() +
                ", was created by " + Arrays.toString(remote.getAuthors()) +
                " and is running version " + remote.getVersion());

        successfulStart = true;
    }

    @Override
    public int getNumber() {
        return new Random().nextInt();
    }

    public boolean wasSuccessful() {
        return successfulStart;
    }

}
