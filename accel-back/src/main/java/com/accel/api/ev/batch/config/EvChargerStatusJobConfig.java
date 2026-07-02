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

import com.accel.api.ev.batch.tasklet.EvChargerStatusUpdateTasklet;

@Configuration
public class EvChargerStatusJobConfig {

	@Bean
	public Step chargerStatusStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			EvChargerStatusUpdateTasklet chargerStatusUpdateTasklet) {
		return new StepBuilder("chargerStatusStep", jobRepository)
				.tasklet(chargerStatusUpdateTasklet, transactionManager)
				.build();
	}

	@Bean
	public Job chargerStatusJob(
			JobRepository jobRepository,
			@Qualifier("chargerStatusStep") Step chargerStatusStep) {
		return new JobBuilder("chargerStatusJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(chargerStatusStep)
				.build();
	}
}
