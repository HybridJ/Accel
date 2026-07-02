package com.accel.api.video;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
	// vehicle 테이블
	private int vehicleId;
	private String vehicleName;
	private String vehicleNameKo;
	private String category;
	private String segment;
	private String bodyType;
	private int seating;
	private long minPrice;
	private long maxPrice;
	private String description;
	private String summary;
	private int brandId;
	private String brandName;
	private String brandNameKo;

	// vehicle_type 테이블 (1:N)
	private List<String> fuelTypes;

	// video 테이블 (nullable)
	private String url;
	private String channelName;
	private String title;
	private List<Video> videos;
}
