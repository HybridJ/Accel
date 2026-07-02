package com.accel.api.ev.batch.tasklet;

import java.util.List;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.accel.api.ev.client.KepcoEvApiClient;
import com.accel.api.ev.dao.EvChargerDao;
import com.accel.api.ev.dto.EvChargeInfo;

@Component
public class EvChargerStatusUpdateTasklet implements Tasklet {

	private final KepcoEvApiClient kepcoEvApiClient;
	private final EvChargerDao evChargerDao;

	public EvChargerStatusUpdateTasklet(KepcoEvApiClient kepcoEvApiClient, EvChargerDao evChargerDao) {
		this.kepcoEvApiClient = kepcoEvApiClient;
		this.evChargerDao = evChargerDao;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		List<EvChargeInfo> list = kepcoEvApiClient.fetch();

		int rows = evChargerDao.updateChargerStatusBatch(list);
		System.out.println("[CHARGER-STATUS]" + System.currentTimeMillis()
				+ " ev_charger 상태 갱신 rows = " + rows
				+ " (충전기 " + list.size() + "대)");

		return RepeatStatus.FINISHED;
	}
}
