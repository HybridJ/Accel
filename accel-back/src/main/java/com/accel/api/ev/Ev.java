package com.accel.api.ev;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ev {
	private String csId;
	private String csName;
	private String addr;
	private Double lat;
	private Double longi;
	private Double distanceKm;
	private List<Charger> chargers;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Charger {
		private String cpId;
		private String cpNm;
		private String chargeTp;
		private String cpTp;
		private String cpStat;
		private String statUpdateDatetime;
	}
}
