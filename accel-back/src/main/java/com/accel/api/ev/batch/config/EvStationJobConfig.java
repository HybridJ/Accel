package com.accel.api.ev.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.accel.api.ev.batch.tasklet.EvStationSaveTasklet;

@Configuration
public class EvStationJobConfig {

	@Bean
	public Step stationSaveStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			EvStationSaveTasklet stationSaveTasklet) {
		return new StepBuilder("stationSaveStep", jobRepository)
				.tasklet(stationSaveTasklet, transactionManager)
				.build();
	}

	@Bean
	public Job stationJob(
			JobRepository jobRepository,
			@Qualifier("stationSaveStep") Step stationSaveStep) {
		return new JobBuilder("stationJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(stationSaveStep)
				.build();
	}
}
