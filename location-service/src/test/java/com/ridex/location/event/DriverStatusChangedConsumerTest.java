package com.ridex.location.event;
import com.ridex.location.service.DriverLocationService;
import org.junit.jupiter.api.Test;
import java.time.Instant; import java.util.UUID;
import static org.mockito.Mockito.*;
class DriverStatusChangedConsumerTest {
 @Test void delegatesIdentityStatusAndAvailabilityInOrder(){ DriverLocationService service=mock(DriverLocationService.class); DriverStatusChangedConsumer consumer=new DriverStatusChangedConsumer(service); UUID event=UUID.randomUUID(),driver=UUID.randomUUID(),user=UUID.randomUUID(); DriverStatusChangedEvent payload=new DriverStatusChangedEvent(event,driver,user,"AVAILABLE",Instant.now()); consumer.consume(payload); var order=inOrder(service); order.verify(service).updateDriverIdentity(user,driver); order.verify(service).updateDriverStatus(driver,"AVAILABLE"); order.verify(service).updateDriverAvailability(payload); }
}
