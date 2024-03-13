package com.may.simpleecommercesite.helpers;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class BiDirectionalMap<T extends Map, K, V> implements Map<K,V>{
    private final Map<K, V> keyToValueMap;
    private final Map<V, K> valueToKeyMap;
    public BiDirectionalMap(Class<T> type){
        try {
            this.keyToValueMap=type.getConstructor().newInstance();
            this.valueToKeyMap=type.getConstructor().newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public V put(Object key, Object value) {
        keyToValueMap.put((K) key, (V) value);
        valueToKeyMap.put((V) value, (K) key);
        return (V) value;
    }
    @Override
    public V remove(Object key) {
        V val= keyToValueMap.remove(key);
        valueToKeyMap.remove(val);
        return val;
    }
    public K reverseRemove(V val){
        K key= valueToKeyMap.remove(val);
        keyToValueMap.remove(key);
        return key;
    }
    @Override
    public void putAll(Map m) {
        for (Map.Entry<K, V> entry:((Map<K,V>) m).entrySet()){
            keyToValueMap.put( entry.getKey(), (V) entry.getValue());
            valueToKeyMap.put((V) entry.getValue(), (K) entry.getKey());
        }
    }
    @Override
    public void clear() {
        keyToValueMap.clear();
        valueToKeyMap.clear();
    }
    @Override
    public Set<K> keySet() {
        return keyToValueMap.keySet();
    }
    public Set<V> reverseKeySet(){
        return valueToKeyMap.keySet();
    }
    @Override
    public Collection<V> values() {
        return keyToValueMap.values();
    }
    public Collection<K> reverseValues(){
        return valueToKeyMap.values();
    }
    @Override
    public Set<Entry<K, V>> entrySet() {
        return keyToValueMap.entrySet();
    }
    public Set<Entry<V, K>> reverseEntrySet(){
        return valueToKeyMap.entrySet();
    }
    @Override
    public int size() {
        return keyToValueMap.size();
    }
    @Override
    public boolean isEmpty() {
        return keyToValueMap.isEmpty();
    }
    @Override
    public boolean containsKey(Object key) {
        return keyToValueMap.containsKey(key);
    }
    @Override
    public boolean containsValue(Object value) {
        return keyToValueMap.containsValue(value);
    }
    @Override
    public V get(Object key){
       return keyToValueMap.get(key);
    }
    public K reverseGet(V value) {
        return valueToKeyMap.get(value);
    }
}
