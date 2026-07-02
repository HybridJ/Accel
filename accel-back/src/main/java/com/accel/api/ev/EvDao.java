package com.accel.api.ev;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvDao {

	List<Ev> selectNearestStations(@Param("lat") double lat, @Param("longi") double longi, @Param("limit") int limit);

	List<Ev> selectStationsByNeighborhood(@Param("lat") double lat, @Param("longi") double longi,
			@Param("neighborhood") String neighborhood);

}
