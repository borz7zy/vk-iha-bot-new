package com.fsoft.vktest.Utils;

/**
 * Created by Dr. Failov on 01.03.2015.
 */

import java.io.Serializable;
import java.util.Map;

/**
    * A class which implements Map.Entry. It is shared by HashMap, TreeMap,
    * Hashtable, and Collections. It is not specified by the JDK, but makes
    * life much easier.
    *
    * @author Jon Zeppieri
    * @author Eric Blake (ebb9@email.byu.edu)
    * 
    * @since 1.6
    */
   public class SimpleEntry<K, V> implements Map.Entry<K, V>, Serializable
   {

   /**
    * Compare two objects according to Collection semantics.
    *
    * @param o1 the first object
    * @param o2 the second object
    * @return o1 == o2 || (o1 != null && o1.equals(o2))
    */
   // Package visible for use throughout java.util.
   // It may be inlined since it is final.
   static final boolean equals(Object o1, Object o2)
   {
     return o1 == o2 || (o1 != null && o1.equals(o2));
   }

   /**
    * Hash an object according to Collection semantics.
    *
    * @param o the object to hash
    * @return o1 == null ? 0 : o1.hashCode()
    */
   // Package visible for use throughout java.util.
   // It may be inlined since it is final.
   static final int hashCode(Object o)
   {
     return o == null ? 0 : o.hashCode();
   }


       

     /**
      * Compatible with JDK 1.6
      */
     private static final long serialVersionUID = -8499721149061103585L;

     /**
      * The key. Package visible for direct manipulation.
      */
     K key;

     /**
      * The value. Package visible for direct manipulation.
      */
     V value;

     /**
      * Basic constructor initializes the fields.
      * @param newKey the key
      * @param newValue the value
      */
     public SimpleEntry(K newKey, V newValue)
     {
       key = newKey;
       value = newValue;
     }

     public SimpleEntry(Map.Entry<? extends K, ? extends V> entry)
     {
       this(entry.getKey(), entry.getValue());
     }

     /**
      * Compares the specified object with this entry. Returns true only if
      * the object is a mapping of identical key and value. In other words,
      * this must be:<br>
      * <pre>(o instanceof Map.Entry)
      *       && (getKey() == null ? ((HashMap) o).getKey() == null
      *           : getKey().equals(((HashMap) o).getKey()))
      *       && (getValue() == null ? ((HashMap) o).getValue() == null
      *           : getValue().equals(((HashMap) o).getValue()))</pre>
      *
      * @param o the object to compare
      * @return <code>true</code> if it is equal
      */
     public boolean equals(Object o)
     {
       if (! (o instanceof Map.Entry))
         return false;
       // Optimize for our own entries.
       if (o instanceof SimpleEntry)
         {
           SimpleEntry e = (SimpleEntry) o;
           return (equals(key, e.key)
                   && equals(value, e.value));
         }
       Map.Entry e = (Map.Entry) o;
       return (equals(key, e.getKey())
               && equals(value, e.getValue()));
     }

     /**
      * Get the key corresponding to this entry.
      *
      * @return the key
      */
     public K getKey()
     {
       return key;
     }

     /**
      * Get the value corresponding to this entry. If you already called
      * Iterator.remove(), the behavior undefined, but in this case it works.
      *
      * @return the value
      */
     public V getValue()
     {
       return value;
     }

     /**
      * Returns the hash code of the entry.  This is defined as the exclusive-or
      * of the hashcodes of the key and value (using 0 for null). In other
      * words, this must be:<br>
      * <pre>(getKey() == null ? 0 : getKey().hashCode())
      *       ^ (getValue() == null ? 0 : getValue().hashCode())</pre>
      *
      * @return the hash code
      */
     public int hashCode()
     {
       return (hashCode(key) ^ hashCode(value));
     }

     /**
      * Replaces the value with the specified object. This writes through
      * to the map, unless you have already called Iterator.remove(). It
      * may be overridden to restrict a null value.
      *
      * @param newVal the new value to store
      * @return the old value
      * @throws NullPointerException if the map forbids null values.
      * @throws UnsupportedOperationException if the map doesn't support
      *          <code>put()</code>.
      * @throws ClassCastException if the value is of a type unsupported
      *         by the map.
      * @throws IllegalArgumentException if something else about this
      *         value prevents it being stored in the map.
      */
     public V setValue(V newVal)
     {
       V r = value;
       value = newVal;
       return r;
     }

     /**
      * This provides a string representation of the entry. It is of the form
      * "key=value", where string concatenation is used on key and value.
      *
      * @return the string representation
      */
     public String toString()
     {
       return key + "=" + value;
     }
   } // class SimpleEntry
