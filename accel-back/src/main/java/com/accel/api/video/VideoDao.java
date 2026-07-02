package com.accel.api.video;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoDao {

	List<String> selectAllCategory();

	List<Vehicle> searchVehicles(
		@Param("category") String category,
		@Param("segments") List<String> segments,
		@Param("fuelTypes") List<String> fuelTypes,
		@Param("minPrice") Long minPrice,
		@Param("maxPrice") Long maxPrice,
		@Param("keyword") String keyword
	);

	Vehicle selectVehicle(@Param("vehicleId") int vehicleId);

}
