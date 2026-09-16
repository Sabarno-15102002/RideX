package com.ridex.matching.service;

import com.ridex.matching.event.TripRematchingEvent;
import com.ridex.matching.event.TripRequestedEvent;

public interface MatchingService {

    void matchTrip(TripRequestedEvent event);

    void rematch(TripRematchingEvent event);

}
