package com.accel.api.ev.batch.scheduler;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EvBatchScheduler {

	private final JobOperator jobOperator;
	private final Job stationJob;
	private final Job chargerStatusJob;

	public EvBatchScheduler(
			JobOperator jobOperator,
			@Qualifier("stationJob") Job stationJob,
			@Qualifier("chargerStatusJob") Job chargerStatusJob) {
		this.jobOperator = jobOperator;
		this.stationJob = stationJob;
		this.chargerStatusJob = chargerStatusJob;
	}

	@Scheduled(cron = "${ev.station.cron}", zone = "Asia/Seoul")
	public void runStationJob() {
		try {
			JobParameters params = new JobParametersBuilder()
					.addLong("time", System.currentTimeMillis())
					.toJobParameters();

			System.out.println("[스케줄러] stationJob 실행 시작");
			jobOperator.start(stationJob, params);
		} catch (Exception e) {
			System.err.println("[스케줄러] stationJob 실행 중 오류");
			e.printStackTrace();
		}
	}

	@Scheduled(
			initialDelayString = "${ev.charger.status.initial-delay-ms:15000}",
			fixedDelayString = "${ev.charger.status.interval-ms:300000}")
	public void runChargerStatusJob() {
		try {
			JobParameters params = new JobParametersBuilder()
					.addLong("time", System.currentTimeMillis())
					.toJobParameters();

			System.out.println("[스케줄러] chargerStatusJob 실행 시작");
			jobOperator.start(chargerStatusJob, params);
		} catch (Exception e) {
			System.err.println("[스케줄러] chargerStatusJob 실행 중 오류");
			e.printStackTrace();
		}
	}
}
