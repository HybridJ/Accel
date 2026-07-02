package com.accel.api.ai;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class EvScoreResponse {
	private Location location;
	private double overallScore;
	private String grade;
	private List<EvaluationItem> items = new ArrayList<>();
	private String overallOpinion;

	@Data
	public static class Location {
		private Double latitude;
		private Double longitude;
	}

	@Data
	public static class EvaluationItem {
		private String category;
		private int score;
	}
}
