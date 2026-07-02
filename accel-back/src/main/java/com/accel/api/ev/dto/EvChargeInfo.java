package com.accel.api.ev.dto;

import java.io.Serializable;

/**
 * KEPCO 전기차 충전소 운영정보 API(EVchargeManage.do) 응답 1건.
 *
 * API 응답은 충전기 1대 단위이며, 한 레코드에 충전소 정보와 충전기 정보가 함께 들어 있다.
 */
public class EvChargeInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	private String csId;
	private String csNm;
	private String addr;
	private String lat;
	private String longi;
	private String cpId;
	private String cpNm;
	private String chargeTp;
	private String cpTp;
	private String cpStat;
	private String statUpdateDatetime;

	public String getCsId() {
		return csId;
	}

	public void setCsId(String csId) {
		this.csId = csId;
	}

	public String getCsNm() {
		return csNm;
	}

	public void setCsNm(String csNm) {
		this.csNm = csNm;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLongi() {
		return longi;
	}

	public void setLongi(String longi) {
		this.longi = longi;
	}

	public String getCpId() {
		return cpId;
	}

	public void setCpId(String cpId) {
		this.cpId = cpId;
	}

	public String getCpNm() {
		return cpNm;
	}

	public void setCpNm(String cpNm) {
		this.cpNm = cpNm;
	}

	public String getChargeTp() {
		return chargeTp;
	}

	public void setChargeTp(String chargeTp) {
		this.chargeTp = chargeTp;
	}

	public String getCpTp() {
		return cpTp;
	}

	public void setCpTp(String cpTp) {
		this.cpTp = cpTp;
	}

	public String getCpStat() {
		return cpStat;
	}

	public void setCpStat(String cpStat) {
		this.cpStat = cpStat;
	}

	public String getStatUpdateDatetime() {
		return statUpdateDatetime;
	}

	public void setStatUpdateDatetime(String statUpdateDatetime) {
		this.statUpdateDatetime = statUpdateDatetime;
	}

	@Override
	public String toString() {
		return String.format("[%s/%s] %s (stat=%s @ %s)",
				csId, cpId, cpNm, cpStat, statUpdateDatetime);
	}
}
