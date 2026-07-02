package com.accel.api.ai;

import lombok.Data;

@Data
public class EvScoreRequest {
	private Double latitude;
	private Double longitude;
	private String address;
}
