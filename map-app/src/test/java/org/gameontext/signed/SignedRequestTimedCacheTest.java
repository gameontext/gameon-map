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
package org.gameontext.signed;

import java.time.Duration;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.gameontext.signed.SignedRequestHmac;
import org.gameontext.signed.SignedRequestTimedCache;
import org.gameontext.signed.TimestampedKey;
import org.junit.Assert;
import org.junit.Test;

import mockit.Mocked;
import mockit.Verifications;

public class SignedRequestTimedCacheTest {

    final static Duration oneMs = Duration.ofMillis(1);

    @Test
    public void testIsDuplicate(@Mocked ManagedExecutorService executor) {

        SignedRequestTimedCache cache = new SignedRequestTimedCache();

        cache.isDuplicate("george", oneMs);
        Assert.assertEquals("Requests should containe one element", 1, cache.requests.size());
        Assert.assertEquals("Requests should containe one element", 1, cache.triggerCount.get());
        cache.isDuplicate("george", oneMs);
        Assert.assertEquals("Requests should containe one element", 1, cache.requests.size());
        Assert.assertEquals("Requests should containe one element", 1, cache.triggerCount.get());

        cache.isDuplicate("judy", oneMs);
        Assert.assertEquals("Requests should containe one element", 2, cache.requests.size());
        Assert.assertEquals("Requests should containe one element", 2, cache.triggerCount.get());
    }

    @Test
    public void testTrigger(@Mocked ManagedExecutorService executor) {

        SignedRequestTimedCache cache = new SignedRequestTimedCache();
        cache.managedExecutorService = executor;

        // executor not called for this one
        cache.triggerCount.set(1);
        cache.isDuplicate("fred", SignedRequestHmac.EXPIRES_REPLAY_MS);

        // executor triggered to clean up for this one
        cache.triggerCount.set(SignedRequestTimedCache.TRIGGER_CLEANUP_DEPTH);
        cache.isDuplicate("fred", SignedRequestHmac.EXPIRES_REPLAY_MS);

        // executor triggered to clean up for this one
        cache.triggerCount.set(SignedRequestTimedCache.TRIGGER_CLEANUP_DEPTH + 1);
        cache.isDuplicate("fred", SignedRequestHmac.EXPIRES_REPLAY_MS);

        new Verifications() {{
            executor.execute(cache); times = 2;
        }};
    }

    @Test
    public void testCacheExpiration(@Mocked ManagedExecutorService executor) {
        SignedRequestTimedCache cache = new SignedRequestTimedCache();
        cache.managedExecutorService = executor;

        cache.requests.put("A", new TimestampedKey("A", Duration.ofMillis(1)));
        cache.requests.put("B", new TimestampedKey("B", SignedRequestHmac.EXPIRES_REPLAY_MS));
        Assert.assertEquals(2, cache.requests.size());

        snooze(3); // make sure "A" has expired
        Assert.assertTrue("'A' should be expired", cache.requests.get("A").hasExpired());

        cache.run();
        Assert.assertEquals(1, cache.requests.size());
        Assert.assertNull("'A' should have been deleted: " + cache.requests, cache.requests.get("A"));
        Assert.assertNotNull("'B' should remain: " + cache.requests, cache.requests.get("B"));
    }

    public void snooze(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
