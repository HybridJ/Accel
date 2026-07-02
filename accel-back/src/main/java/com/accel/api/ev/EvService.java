package com.accel.api.ev;

import java.util.List;

public interface EvService {

	List<Ev> findNearestStationsByCoordinate(double lat, double longi);

	List<Ev> findStationsInSameNeighborhood(double lat, double longi, String address);

}
