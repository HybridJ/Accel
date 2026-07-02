package com.accel.api.ai;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.accel.api.video.Vehicle;

@Mapper
public interface AiDao {

	List<AiBrand> selectAllBrands();

	List<Vehicle> selectVehiclesByBrandId(@Param("brandId") int brandId);
}
