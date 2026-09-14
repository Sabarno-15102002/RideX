package com.ridex.matching.service;

import com.ridex.matching.event.TripRequestedEvent;

public interface MatchingService {

    void matchTrip(TripRequestedEvent event);

}
