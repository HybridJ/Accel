package com.accel.api.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
	private int videoId;
	private int vehicleId;
	private int displayOrder;
	private String url;
	private String channelName;
	private String title;
}
