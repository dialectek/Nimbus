// Bidirectional map.
// See: https://www.geeksforgeeks.org/java/how-to-implement-a-bidirectional-map-using-two-hashsets-in-java/

package com.dialectek.nimbus.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BidirectionalMap<K, V> {
   private final Map<K, V> keyToValueMap = new HashMap<>();
   private final Map<V, K> valueToKeyMap = new HashMap<>();

   // method to put a key-value pair into the bidirectional map
   public void put(K key, V value)
   {
      keyToValueMap.put(key, value);
      valueToKeyMap.put(value, key);
   }


   // method to get a value based on the key
   public V getValueByKey(K key)
   {
      return(keyToValueMap.get(key));
   }


   // method to get a key based on the value
   public K getKeyByValue(V value)
   {
      return(valueToKeyMap.get(value));
   }


   // method to check if a key exists in the map
   public boolean containsKey(K key)
   {
      return(keyToValueMap.containsKey(key));
   }


   // method to check if a value exists in the map
   public boolean containsValue(V value)
   {
      return(valueToKeyMap.containsKey(value));
   }


   // method to remove a key-value pair based on the key
   public void removeByKey(K key)
   {
      V value = keyToValueMap.remove(key);

      valueToKeyMap.remove(value);
   }


   // method to remove all key-value pairs from the bidirectional map
   public void removeAll()
   {
      keyToValueMap.clear();
      valueToKeyMap.clear();
   }


   // method to get a set of all keys in the bidirectional map
   public Set<K> getAllKeys()
   {
      return(keyToValueMap.keySet());
   }


   // method to get a set of all values in the bidirectional map
   public Set<V> getAllValues()
   {
      return(valueToKeyMap.keySet());
   }


   // Main method to demonstrate the usage of the BidirectionalMap class
   public static void main(String[] args)
   {
      // Create a BidirectionalMap with Integer keys and String values
      BidirectionalMap<Integer, String> biMap = new BidirectionalMap<>();

      // Add key-value pairs
      biMap.put(1, "One");
      biMap.put(2, "Two");
      biMap.put(3, "Three");

      // Retrieve values by key
      System.out.println("Value for key 2: " + biMap.getValueByKey(2));

      // Retrieve keys by value
      System.out.println("Key for value 'Three': " + biMap.getKeyByValue("Three"));

      // checks if a key or value exists
      System.out.println("Contains key 4: " + biMap.containsKey(4));
      System.out.println("Contains value 'One': " + biMap.containsValue("One"));

      // Remove a key-value pair
      biMap.removeByKey(1);

      // Display all keys and values
      System.out.println("All keys: " + biMap.getAllKeys());
      System.out.println("All values: " + biMap.getAllValues());

      // Clear the map
      biMap.removeAll();

      // checks if the map is empty after clearing
      System.out.println("Is the map empty? " + (biMap.getAllKeys().isEmpty() && biMap.getAllValues().isEmpty()));
   }
}
