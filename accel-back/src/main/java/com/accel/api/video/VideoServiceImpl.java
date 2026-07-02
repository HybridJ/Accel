package com.accel.api.video;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class VideoServiceImpl implements VideoService {

	private final VideoDao videoDao;

	public VideoServiceImpl(VideoDao videoDao) {
		this.videoDao = videoDao;
	}

	@Override
	public List<String> selectAllCategory() {
		return videoDao.selectAllCategory();
	}

	@Override
	public List<Vehicle> searchVehicles(String category, List<String> segments, List<String> fuelTypes, Long minPrice, Long maxPrice, String keyword) {
		return videoDao.searchVehicles(category, segments, fuelTypes, minPrice, maxPrice, keyword);
	}

	@Override
	public Vehicle selectVehicle(int vehicleId) {
		return videoDao.selectVehicle(vehicleId);
	}
}
