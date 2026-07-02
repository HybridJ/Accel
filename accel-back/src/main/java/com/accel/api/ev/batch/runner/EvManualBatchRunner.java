package com.accel.api.ev.batch.runner;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EvManualBatchRunner implements ApplicationRunner {

	private final JobOperator jobOperator;
	private final Job stationJob;
	private final JdbcTemplate jdbcTemplate;

	@Value("${ev.batch.manual-run.enabled:false}")
	private boolean manualRunEnabled;

	@Value("${ev.batch.manual-run.if-empty-only:false}")
	private boolean ifEmptyOnly;

	@Value("${ev.batch.manual-run.target-date:}")
	private String targetDate;

	public EvManualBatchRunner(
			JobOperator jobOperator,
			@Qualifier("stationJob") Job stationJob,
			JdbcTemplate jdbcTemplate) {
		this.jobOperator = jobOperator;
		this.stationJob = stationJob;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!manualRunEnabled) {
			return;
		}

		if (ifEmptyOnly && hasEvData()) {
			System.out.println("[EV-MANUAL-RUNNER] EV data already exists. stationJob skipped.");
			return;
		}

		try {
			JobParametersBuilder builder = new JobParametersBuilder()
					.addLong("time", System.currentTimeMillis());
			if (targetDate != null && !targetDate.isBlank()) {
				builder.addString("targetDate", targetDate);
			}
			JobParameters params = builder.toJobParameters();

			System.out.println("[EV-MANUAL-RUNNER] stationJob start once"
					+ " (targetDate=" + (targetDate == null || targetDate.isBlank() ? "none" : targetDate)
					+ ", ifEmptyOnly=" + ifEmptyOnly + ")");
			jobOperator.start(stationJob, params);
		} catch (Exception e) {
			System.err.println("[EV-MANUAL-RUNNER] stationJob failed.");
			e.printStackTrace();
		}
	}

	private boolean hasEvData() {
		long stationCount = countRows("ev_station");
		long chargerCount = countRows("ev_charger");
		return stationCount > 0 && chargerCount > 0;
	}

	private long countRows(String tableName) {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
		return count == null ? 0 : count;
	}
}
