package com.ridex.location.controller;
import com.ridex.location.config.SecurityUtils; import com.ridex.location.dto.AuthenticatedUser; import com.ridex.location.dto.request.UpdateLocationRequest; import com.ridex.location.service.DriverLocationService;
import org.junit.jupiter.api.*; import org.springframework.http.HttpStatus; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.context.SecurityContextHolder; import java.util.UUID; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class DriverLocationControllerTest {
 DriverLocationService service; DriverLocationController controller; UUID user;
 @BeforeEach void setup(){service=mock(DriverLocationService.class);controller=new DriverLocationController(service);user=UUID.randomUUID();SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user,"DRIVER"),null));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void validRequestUpdatesLocationAndReturns204(){var response=controller.updateMyLocation(new UpdateLocationRequest(12.5,77.6));assertEquals(HttpStatus.NO_CONTENT,response.getStatusCode());verify(service).updateDriverLocation(user,12.5,77.6);}
 @Test void serviceExceptionPropagates(){doThrow(new IllegalStateException()).when(service).updateDriverLocation(user,1,2);assertThrows(IllegalStateException.class,()->controller.updateMyLocation(new UpdateLocationRequest(1.0,2.0)));}
}
