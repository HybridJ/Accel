package com.accel.api.video;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface VideoService {

	List<String> selectAllCategory();

	List<Vehicle> searchVehicles(String category, List<String> segments, List<String> fuelTypes, Long minPrice, Long maxPrice, String keyword);

	Vehicle selectVehicle(int vehicleId);
}
