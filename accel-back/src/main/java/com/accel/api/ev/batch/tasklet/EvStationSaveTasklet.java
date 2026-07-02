package com.accel.api.ev.batch.tasklet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.accel.api.ev.client.KepcoEvApiClient;
import com.accel.api.ev.dao.EvChargerDao;
import com.accel.api.ev.dao.EvStationDao;
import com.accel.api.ev.dto.EvChargeInfo;

@Component
public class EvStationSaveTasklet implements Tasklet {

	private final KepcoEvApiClient kepcoEvApiClient;
	private final EvStationDao evStationDao;
	private final EvChargerDao evChargerDao;

	public EvStationSaveTasklet(
			KepcoEvApiClient kepcoEvApiClient,
			EvStationDao evStationDao,
			EvChargerDao evChargerDao) {
		this.kepcoEvApiClient = kepcoEvApiClient;
		this.evStationDao = evStationDao;
		this.evChargerDao = evChargerDao;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		List<EvChargeInfo> list = kepcoEvApiClient.fetch();

		Map<String, EvChargeInfo> stationMap = new LinkedHashMap<>();
		for (EvChargeInfo e : list) {
			if (e.getCsId() != null && !e.getCsId().isBlank()) {
				stationMap.put(e.getCsId(), e);
			}
		}
		List<EvChargeInfo> stations = new ArrayList<>(stationMap.values());

		int stationRows = evStationDao.upsertStationBatch(stations);
		System.out.println("[STATION-SAVE] ev_station upsert rows = " + stationRows
				+ " (충전소 " + stations.size() + "곳)");

		int chargerRows = evChargerDao.upsertChargerMasterBatch(list);
		System.out.println("[STATION-SAVE] ev_charger upsert rows = " + chargerRows
				+ " (충전기 " + list.size() + "대)");

		return RepeatStatus.FINISHED;
	}
}
