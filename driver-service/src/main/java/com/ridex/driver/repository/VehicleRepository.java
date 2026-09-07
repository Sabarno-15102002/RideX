package com.ridex.driver.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ridex.driver.entity.Vehicle;
import com.ridex.driver.utilities.VehicleType;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findFirstByDriverId(UUID driverId);

    List<Vehicle> findByDriverId(UUID driverId);

    List<Vehicle> findByVehicleType(VehicleType vehicleType);
}