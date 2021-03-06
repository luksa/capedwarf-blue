/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2011, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.capedwarf.memcache;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.memcache.*;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.capedwarf.common.infinispan.InfinispanUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class InfinispanMemcacheService implements MemcacheService {

    protected final Logger log = Logger.getLogger(getClass().getName());
    private static final SetPolicy DEFAULT_SET_POLICY = SetPolicy.SET_ALWAYS;

    protected final Cache<NamespacedKey, Object> cache;
    private String namespace;
    private ErrorHandler errorHandler;

    public InfinispanMemcacheService() {
        this(null);
    }

    public InfinispanMemcacheService(String namespace) {
        setNamespace(namespace);
        this.cache = getCache(getCacheName());
    }

    private Cache<NamespacedKey, Object> getCache(String cacheName) {
        EmbeddedCacheManager manager = InfinispanUtils.getCacheManager();
        return manager.getCache(cacheName, true);
    }

    private String getCacheName() {
        return "memcache"; // TODO
    }

    public <T> Map<T, IdentifiableValue> getIdentifiables(Collection<T> ts) {
        return null; // TODO
    }

    public <T> Set<T> putIfUntouched(Map<T, CasValues> tCasValuesMap) {
        return null; // TODO
    }

    public <T> Set<T> putIfUntouched(Map<T, CasValues> tCasValuesMap, Expiration expiration) {
        return null; // TODO
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    public Object get(Object key) {
        return cache.get(namespacedKey(key));
    }

    public IdentifiableValue getIdentifiable(final Object key) {
        return new MyIdentifiableValue(cache.get(namespacedKey(key)));
    }

    public boolean contains(Object key) {
        return cache.containsKey(namespacedKey(key));
    }

    public <T> Map<T, Object> getAll(Collection<T> keys) {
        Map<T, Object> map = new HashMap<T, Object>();
        for (T key : keys) {
            map.put(key, cache.get(namespacedKey(key)));
        }
        return map;
    }

    public void put(Object key, Object value) {
        put(key, value, null);
    }

    public void put(Object key, Object value, Expiration expiration) {
        put(key, value, expiration, DEFAULT_SET_POLICY);
    }

    public boolean put(Object key, Object value, Expiration expiration, SetPolicy policy) {
        NamespacedKey namespacedKey = namespacedKey(key);
        switch (policy) {
            case SET_ALWAYS: {
                cache.getAdvancedCache()
                        .withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP)
                        .put(namespacedKey, value, toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                return true;
            }
            case ADD_ONLY_IF_NOT_PRESENT: {
                Object previousValue = cache.putIfAbsent(namespacedKey, value, toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                return previousValue == null;
            }
            case REPLACE_ONLY_IF_PRESENT: {
                Object previousValue = cache.replace(namespacedKey, value, toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                return previousValue != null;
            }
            default:
                throw new IllegalArgumentException("Unsupported policy " + policy);
        }
    }

    public boolean putIfUntouched(Object key, IdentifiableValue oldValue, Object newValue) {
        return putIfUntouched(key, oldValue, newValue, null);
    }

    public boolean putIfUntouched(Object key, IdentifiableValue oldValue, Object newValue, Expiration expiration) {
        return cache.replace(namespacedKey(key), oldValue.getValue(), newValue, toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
    }

    public void putAll(Map<?, ?> map) {
        putAll(map, null);
    }

    public void putAll(Map<?, ?> map, Expiration expiration) {
        putAll(map, expiration, DEFAULT_SET_POLICY);
    }

    public <T> Set<T> putAll(Map<T, ?> map, Expiration expiration, SetPolicy policy) {
        //        TODO: cache.startBatch();
        switch (policy) {
            case SET_ALWAYS:
                cache.getAdvancedCache()
                        .withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP)
                        .putAll(toNamespacedMap(map), toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                return map.keySet();
            case ADD_ONLY_IF_NOT_PRESENT:
                Set<T> addedKeys = new HashSet<T>();
                for (Map.Entry<T, ?> entry : map.entrySet()) {
                    Object previousValue = cache.putIfAbsent(namespacedKey(entry.getKey()), entry.getValue(), toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                    if (previousValue == null) {
                        addedKeys.add(entry.getKey());
                    }
                }
                return addedKeys;
            case REPLACE_ONLY_IF_PRESENT:
                Set<T> replacedKeys = new HashSet<T>();
                for (Map.Entry<T, ?> entry : map.entrySet()) {
                    Object previousValue = cache.replace(namespacedKey(entry.getKey()), entry.getValue(), toLifespanMillis(expiration), TimeUnit.MILLISECONDS);
                    if (previousValue != null) {
                        replacedKeys.add(entry.getKey());
                    }
                }
                return replacedKeys;
            default:
                throw new IllegalArgumentException("Unsupported policy " + policy);
        }
    }

    public boolean delete(Object key) {
        Object removedObject = cache.remove(namespacedKey(key));
        return removedObject != null;
    }

    public boolean delete(Object key, long millisNoReAdd) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Set<T> deleteAll(Collection<T> keys) {
        Set<T> deletedKeys = new HashSet<T>();
        for (T key : keys) {
            Object previousValue = cache.remove(namespacedKey(key));
            if (previousValue != null) {
                deletedKeys.add(key);
            }
        }
        return deletedKeys;
    }

    public <T> Set<T> deleteAll(Collection<T> keys, long millisNoReAdd) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long increment(Object key, long delta) {
        return increment(key, delta, null);
    }


    public Long increment(Object key, long delta, Long initialValue) {
        Object value = get(key);
        if (value == null) {
            if (initialValue == null) {
                return null;
            } else {
                put(key, initialValue);
                return initialValue;
            }
        } else {
            long newValue = castToLong(value) + delta;
            put(key, newValue);
            return newValue;
        }
    }

    private long castToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else {
            throw new InvalidValueException("Cannot increment. Value was " + value);
        }
    }

    public <T> Map<T, Long> incrementAll(Collection<T> keys, long delta) {
        return incrementAll(keys, delta, null);
    }

    public <T> Map<T, Long> incrementAll(Collection<T> keys, long delta, Long initialValue) {
        Map<T, Long> map = new HashMap<T, Long>();
        for (T key : keys) {
            Long newValue = increment(key, delta, initialValue);
            map.put(key, newValue);
        }
        return map;
    }

    public <T> Map<T, Long> incrementAll(Map<T, Long> offsets) {
        return incrementAll(offsets, null);
    }

    public <T> Map<T, Long> incrementAll(Map<T, Long> offsets, Long initialValue) {
        Map<T, Long> map = new HashMap<T, Long>();
        for (Map.Entry<T, Long> entry : offsets.entrySet()) {
            T key = entry.getKey();
            Long delta = entry.getValue();
            Long newValue = increment(key, delta, initialValue);
            map.put(key, newValue);
        }
        return map;
    }

    public void clearAll() {
        cache.clear();
    }


    public Stats getStatistics() {
        return new InfinispanStatistics(cache.getAdvancedCache());
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    private long toLifespanMillis(Expiration expiration) {
        if (expiration == null) {
            return -1;
        } else {
            return expiration.getMillisecondsValue() - System.currentTimeMillis();
        }
    }

    private static class MyIdentifiableValue implements IdentifiableValue {
        private final Object value;

        public MyIdentifiableValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    private NamespacedKey namespacedKey(Object key) {
        String namespace = getNamespace() == null ? NamespaceManager.get() : getNamespace();
        return new NamespacedKey(namespace, key);
    }

    private <T> Map<? extends NamespacedKey, ?> toNamespacedMap(Map<T, ?> map) {
        HashMap<NamespacedKey, Object> namespacedKeyMap = new HashMap<NamespacedKey, Object>();
        for (Map.Entry<T, ?> entry : map.entrySet()) {
            namespacedKeyMap.put(namespacedKey(entry.getKey()), entry.getValue());
        }
        return namespacedKeyMap;
    }

    private static class NamespacedKey implements Serializable {
        private String namespace;
        private Object key;

        private NamespacedKey(String namespace, Object key) {
            this.namespace = namespace;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NamespacedKey that = (NamespacedKey) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = namespace != null ? namespace.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }
    }
}
