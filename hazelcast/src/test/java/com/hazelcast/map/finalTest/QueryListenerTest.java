/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.finalTest;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.*;
import com.hazelcast.instance.StaticNodeFactory;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.SqlPredicate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

@RunWith(com.hazelcast.util.RandomBlockJUnit4ClassRunner.class)
public class QueryListenerTest {

    final String mapName = "map";

    @BeforeClass
    @AfterClass
    public static void cleanup() throws Exception {
        Hazelcast.shutdownAll();
    }

    @After
    public void after() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void testMapQueryListener() throws InterruptedException {

        StaticNodeFactory nodeFactory = new StaticNodeFactory(3);
        Config cfg = new Config();
        HazelcastInstance instance1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance instance2 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance instance3 = nodeFactory.newHazelcastInstance(cfg);

        final IMap<Object, Object> map = instance1.getMap("testMapQueryListener");
        final Object[] addedKey = new Object[1];
        final Object[] addedValue = new Object[1];
        final Object[] updatedKey = new Object[1];
        final Object[] oldValue = new Object[1];
        final Object[] newValue = new Object[1];
        final Object[] removedKey = new Object[1];
        final Object[] removedValue = new Object[1];

        EntryListener<Object, Object> listener = new EntryListener<Object, Object>() {
            public void entryAdded(EntryEvent<Object, Object> event) {
                addedKey[0] = event.getKey();
                addedValue[0] = event.getValue();
            }

            public void entryRemoved(EntryEvent<Object, Object> event) {
                removedKey[0] = event.getKey();
                removedValue[0] = event.getOldValue();
            }

            public void entryUpdated(EntryEvent<Object, Object> event) {
                updatedKey[0] = event.getKey();
                oldValue[0] = event.getOldValue();
                newValue[0] = event.getValue();
            }

            public void entryEvicted(EntryEvent<Object, Object> event) {
            }
        };

        map.addEntryListener(listener, new StartsWithPredicate("a"), null, true);
        map.put("key1", "abc");
        map.put("key2", "bcd");
        map.put("key2", "axyz");
        map.remove("key1");
        Thread.sleep(1000);

        assertEquals(addedKey[0], "key1");
        assertEquals(addedValue[0], "abc");
        assertEquals(updatedKey[0], "key2");
        assertEquals(oldValue[0], "bcd");
        assertEquals(newValue[0], "axyz");
        assertEquals(removedKey[0], "key1");
        assertEquals(removedValue[0], "abc");
    }

    static class StartsWithPredicate implements Predicate<Object, Object>, Serializable {
        String pref;

        StartsWithPredicate(String pref) {
            this.pref = pref;
        }

        public boolean apply(Map.Entry<Object, Object> mapEntry) {
            String val = (String) mapEntry.getValue();
            if (val == null)
                return false;
            if (val.startsWith(pref))
                return true;
            return false;
        }
    }

    @Test
    public void testMapQueryListener2() throws InterruptedException {

        StaticNodeFactory nodeFactory = new StaticNodeFactory(3);
        Config cfg = new Config();
        HazelcastInstance instance1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance instance2 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance instance3 = nodeFactory.newHazelcastInstance(cfg);

        final IMap<Object, Object> map = instance1.getMap("testMapQueryListener2");
        final AtomicInteger addCount = new AtomicInteger(0);


        EntryListener<Object, Object> listener = new EntryListener<Object, Object>() {
            public void entryAdded(EntryEvent<Object, Object> event) {
                addCount.incrementAndGet();
            }

            public void entryRemoved(EntryEvent<Object, Object> event) {
            }

            public void entryUpdated(EntryEvent<Object, Object> event) {
            }

            public void entryEvicted(EntryEvent<Object, Object> event) {
            }
        };

        Predicate predicate = new SqlPredicate("age >= 50");
        map.addEntryListener(listener, predicate, null, false);
        int size = 100;
        for (int i = 0; i < size; i++) {
            Person person = new Person("name", i);
            map.put(i, person);
        }
        Thread.sleep(1000);
        assertEquals(50, addCount.get());
    }


    static class Person implements Serializable {
        String name;
        int age;

        Person() {
        }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }


}