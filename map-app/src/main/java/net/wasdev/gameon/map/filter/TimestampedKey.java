package net.wasdev.gameon.map.filter;

import java.util.logging.Level;

import net.wasdev.gameon.map.Log;

/**
 * Timestamped Key
 * Equality / Hashcode is determined by key string alone.
 * Sort order is provided by key timestamp.
 */
final class TimestampedKey implements Comparable<TimestampedKey> {
    private String key;
    private final Long time = System.currentTimeMillis();
    private final Long expiresAfter;
    
    public TimestampedKey(Long expiresAfter){
        this.expiresAfter = expiresAfter;
    }
    
    public TimestampedKey(String a, Long expiresAfter){
        this.key=a; 
        this.expiresAfter = expiresAfter;
    }
    
    @Override
    public int compareTo(TimestampedKey o) {
        return o.time.compareTo(time);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimestampedKey other = (TimestampedKey) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }
    
    public boolean hasExpired() {
        //cache hit, but is the key still valid?
        long current = System.currentTimeMillis();
        long delta = current - time;      
        Log.log(Level.FINER, this, "comparing key with stamp {0} with current time delta {1}",time,current);
        //if the key is older than this time period.. we'll consider it dead.
        return delta > expiresAfter;    
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    
    
}