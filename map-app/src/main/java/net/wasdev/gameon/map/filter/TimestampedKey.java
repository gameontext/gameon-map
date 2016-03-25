/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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