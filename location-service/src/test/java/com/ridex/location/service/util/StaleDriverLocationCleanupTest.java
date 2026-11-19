package com.ridex.location.service.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class StaleDriverLocationCleanupTest {
    RedisTemplate<String,String> redis;
    ZSetOperations<String,String> zset;
    GeoOperations<String,String> geo;
    SetOperations<String,String> set;
    StaleDriverLocationCleanup cleanup;
    @SuppressWarnings("unchecked") @BeforeEach void setup(){
        redis=mock(RedisTemplate.class); zset=mock(ZSetOperations.class); geo=mock(GeoOperations.class); set=mock(SetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset); when(redis.opsForGeo()).thenReturn(geo); when(redis.opsForSet()).thenReturn(set);
        cleanup=new StaleDriverLocationCleanup(redis); ReflectionTestUtils.setField(cleanup,"staleAfterSeconds",30L);
    }
    @Test void noStaleDriversDoesNothing(){ when(zset.rangeByScore(eq("ridex:drivers:last-seen"),eq(0.0),anyDouble())).thenReturn(Set.of()); cleanup.removeStaleDrivers(); verifyNoInteractions(geo,set); }
    @Test void nullStaleSetDoesNothing(){ when(zset.rangeByScore(eq("ridex:drivers:last-seen"),eq(0.0),anyDouble())).thenReturn(null); assertDoesNotThrow(()->cleanup.removeStaleDrivers()); verifyNoInteractions(geo,set); }
    @Test void removesStaleDriverFromAllIndexes(){ String id="driver-1"; when(zset.rangeByScore(eq("ridex:drivers:last-seen"),eq(0.0),anyDouble())).thenReturn(Set.of(id)); cleanup.removeStaleDrivers(); verify(geo).remove("ridex:drivers:locations",id); verify(set).remove("ridex:drivers:available",id); verify(zset).remove("ridex:drivers:last-seen",id); }
    @Test void removesEveryStaleDriver(){ when(zset.rangeByScore(eq("ridex:drivers:last-seen"),eq(0.0),anyDouble())).thenReturn(Set.of("d1","d2")); cleanup.removeStaleDrivers(); verify(geo,times(2)).remove(eq("ridex:drivers:locations"),anyString()); verify(set,times(2)).remove(eq("ridex:drivers:available"),anyString()); verify(zset,times(2)).remove(eq("ridex:drivers:last-seen"),anyString()); }
}
