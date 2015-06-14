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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VersionTest {

    @Test
    public void testVersions() throws Exception {
        final Version version = Version.parse("v1.0.0");
        Assert.assertEquals(1, version.getMajor());
        Assert.assertEquals(0, version.getMinor());
        Assert.assertEquals(0, version.getPatch());
        Assert.assertEquals(version, new Version(1));

        final Version snapshot = Version.parse("1.2.3-SNAPSHOT");
        Assert.assertEquals(1, snapshot.getMajor());
        Assert.assertEquals(2, snapshot.getMinor());
        Assert.assertEquals(3, snapshot.getPatch());
        Assert.assertTrue(snapshot.isSnapshot());
        Assert.assertEquals(snapshot, new Version(1, 2, 3, true));

        final String patch = "alpha.10.beta.0";
        final String build = "build-1.0.unicorn";
        final Version unicorn = Version.parse("3.2.1-" + patch + '+' + build);
        Assert.assertEquals(3, unicorn.getMajor());
        Assert.assertEquals(2, unicorn.getMinor());
        Assert.assertEquals(1, unicorn.getPatch());
        Assert.assertEquals(unicorn.getPreRelease(), patch);
        Assert.assertEquals(unicorn.getBuild(), build);
        Assert.assertFalse(unicorn.isSnapshot());
        Assert.assertEquals(unicorn, new Version(3, 2, 1, patch, build));

        Assert.assertEquals(1, new Version(2).compareTo(version));
        Assert.assertEquals(1, new Version(1, 1).compareTo(version));
        Assert.assertEquals(1, new Version(1, 0, 1).compareTo(version));
    }

    @Test
    public void testText() throws Exception {
        Version[] versions = Version.parseMulti("This app contains unicorn 1.3.7 and rainbow 4.2.9");
        Assert.assertEquals("unexpected number of found versions", 2, versions.length);

        Version unicorn = versions[0];
        Assert.assertEquals(1, unicorn.getMajor());
        Assert.assertEquals(3, unicorn.getMinor());
        Assert.assertEquals(7, unicorn.getPatch());

        Version rainbow = versions[1];
        Assert.assertEquals(4, rainbow.getMajor());
        Assert.assertEquals(2, rainbow.getMinor());
        Assert.assertEquals(9, rainbow.getPatch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFail() throws Exception {
        new Version("v1");
    }

}
