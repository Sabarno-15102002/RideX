package com.ridex.trip.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateTripRequestValidationTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll static void closeValidator() { factory.close(); }

    @Test void validCoordinates_shouldHaveNoViolations() {
        CreateTripRequest request = new CreateTripRequest(12.9716, 77.5946, 12.9352, 77.6245);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test void latitudeOutsideRange_shouldBeRejected() {
        CreateTripRequest request = new CreateTripRequest(91.0, 77.5946, 12.9352, 77.6245);
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("pickupLatitude")));
    }

    @Test void longitudeOutsideRange_shouldBeRejected() {
        CreateTripRequest request = new CreateTripRequest(12.9716, 181.0, 12.9352, 77.6245);
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("pickupLongitude")));
    }

    @Test void nullCoordinate_shouldBeRejected() {
        CreateTripRequest request = new CreateTripRequest(null, 77.5946, 12.9352, 77.6245);
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("pickupLatitude")));
    }
}
