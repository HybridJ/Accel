package com.accel.api.ev.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.accel.api.ev.dto.EvChargeInfo;

@Mapper
public interface EvChargerDao {

	int upsertChargerMasterBatch(List<EvChargeInfo> list);

	int updateChargerStatusBatch(List<EvChargeInfo> list);
}
