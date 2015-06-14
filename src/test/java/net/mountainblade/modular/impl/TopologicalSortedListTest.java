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
package net.mountainblade.modular.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class TopologicalSortedListTest extends TopologicalSortedList<String> {

    @Test
    public void testSingle() throws CycleException {
        Node<String> depOne = addNode("Dependency 1");
        Node<String> original = new Node<>("Original");

        depOne.isRequiredBefore(original);
        addNode("Dependency 2").isRequiredBefore(depOne);
        addNode("Unimportant").isRequiredBefore(original);
        original.isRequiredBefore("After");

        sort();

        System.out.println(this);
    }

    @Test
    public void testDouble() throws CycleException {
        Node<String> node3 = addNode("3");
        addNode("1").isRequiredBefore(node3);
        addNode("2").isRequiredBefore(node3);

        sort();

        String[] result = new String[size()];
        System.out.println(this);

        for (int i = 0; i < this.size(); i++) {
            Node<String> node = this.get(i);

            result[i] = node.getValue();
        }

        Assert.assertTrue("Result is not correct!",
                Arrays.equals(result, new String[]{"2", "1", "3"}) || Arrays.equals(result, new String[]{"1", "2", "3"})
        );
    }

    @Test(expected = CycleException.class)
    public void testCycle() throws CycleException {
        Node<String> node1 = addNode("1");
        Node<String> node2 = addNode("2");
        Node<String> node3 = addNode("3");

        node3.isRequiredBefore(node1);
        node1.isRequiredBefore(node3);

        node2.isRequiredBefore(node1);
        node2.isRequiredBefore(node3);

        sort();

        System.out.println(this);
    }

}
