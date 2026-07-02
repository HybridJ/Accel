package com.accel.api.ev;

import java.util.List;
import java.util.Map;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "EV", description = "EV charging station API")
@RestController
@RequestMapping("/ev")
@RequiredArgsConstructor
public class EvController {

	private final EvService evService;
	private final JobOperator jobOperator;

	@Qualifier("stationJob")
	private final Job stationJob;

	@Qualifier("chargerStatusJob")
	private final Job chargerStatusJob;

	@Operation(summary = "Find nearest EV charging stations", description = "Finds the nearest 3 stations by coordinates.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Nearest stations found"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@GetMapping("/nearest")
	public ResponseEntity<List<Ev>> nearestStations(
			@Parameter(description = "User latitude", example = "37.5012743")
			@RequestParam Double lat,
			@Parameter(description = "User longitude", example = "127.039585")
			@RequestParam Double longi) {
		try {
			return ResponseEntity.ok(evService.findNearestStationsByCoordinate(lat, longi));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping("/batch/stations")
	public ResponseEntity<Map<String, Object>> runStationBatch() {
		return startBatchJob(stationJob, "stationJob");
	}

	@PostMapping("/batch/chargers/status")
	public ResponseEntity<Map<String, Object>> runChargerStatusBatch() {
		return startBatchJob(chargerStatusJob, "chargerStatusJob");
	}

	private ResponseEntity<Map<String, Object>> startBatchJob(Job job, String jobName) {
		try {
			JobParameters params = new JobParametersBuilder()
					.addString("trigger", "manual-http")
					.addLong("time", System.currentTimeMillis())
					.toJobParameters();

			jobOperator.start(job, params);

			return ResponseEntity.accepted().body(Map.of(
					"message", jobName + " start requested",
					"jobName", jobName));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of(
					"message", jobName + " start request failed",
					"error", e.getMessage()));
		}
	}
}
