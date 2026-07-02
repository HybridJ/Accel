package com.accel.api.ai;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum RecommendedCarModel {
	HYUNDAI_CASPER("Hyundai", 1, "Casper", 1014, "micro", "작지만 어디서든 먼저 말을 거는 골목길의 장난꾸러기"),
	KIA_EV6("Kia", 2, "EV6", 2004, "suv", "조용히 등장하지만 모두의 시선을 바꾸는 미래형 얼리어답터"),
	GENESIS_G80("Genesis", 3, "G80", 3002, "sedan", "말보다 분위기로 설득하는 품격 있는 전략가"),
	CHEVROLET_COLORADO("Chevrolet", 4, "Colorado", 4001, "truck", "흙먼지를 묻혀도 더 멋있어지는 거친 해결사"),
	LAND_ROVER_DEFENDER("Land Rover", 22, "Defender", 22001, "suv", "정장을 입은 탐험가, 언제든 문명 밖으로 떠날 준비가 되어 있는 개척자"),
	MERCEDES_BENZ_E_CLASS("Mercedes-Benz", 7, "E-Class", 7006, "sedan", "흔들림 없는 태도로 일상을 고급스럽게 정리하는 완벽주의자"),
	BMW_3_SERIES("BMW", 8, "3 Series", 8004, "sedan", "출근길도 코너링처럼 즐기는 민첩한 승부사"),
	AUDI_A7("Audi", 9, "A7", 9004, "sport", "차분한 표정 뒤에 날렵한 감각을 숨긴 세련된 감성파"),
	VOLKSWAGEN_GOLF_GTI("Volkswagen", 10, "Golf GTI", 10003, "hatchback", "평범한 얼굴로 가장 재밌는 길을 찾아내는 반전 캐릭터"),
	PORSCHE_911("Porsche", 11, "911", 11002, "sport", "완벽한 정장을 입었지만 속에는 뜨거운 심장을 품은 베테랑 운동선수"),
	TESLA_CYBERTRUCK("Tesla", 12, "Cybertruck", 12001, "truck", "상식적인 디자인을 거부하고 미래에서 먼저 도착한 괴짜"),
	FORD_MUSTANG("Ford", 13, "Mustang", 13003, "sport", "규칙보다 본능을 믿고 직선으로 질주하는 자유로운 반항아"),
	JEEP_WRANGLER("Jeep", 14, "Wrangler", 14005, "suv", "길이 없으면 직접 길을 만들어버리는 야생형 개척자"),
	MINI_COOPER("MINI", 20, "Cooper", 20002, "hatchback", "작은 체구로 분위기를 뒤집는 유쾌한 무드메이커"),
	FERRARI_296("Ferrari", 25, "296", 25002, "sport", "최첨단 감각과 예술성을 동시에 폭발시키는 천재 퍼포머"),
	LAMBORGHINI_REVUELTO("Lamborghini", 26, "Revuelto", 26001, "sport", "등장만으로 분위기를 압도하는 타고난 슈퍼스타");

	private final String brand;
	private final int brandId;
	private final String model;
	private final int vehicleId;
	private final String category;
	private final String characteristic;

	RecommendedCarModel(String brand, int brandId, String model, int vehicleId, String category, String characteristic) {
		this.brand = brand;
		this.brandId = brandId;
		this.model = model;
		this.vehicleId = vehicleId;
		this.category = category;
		this.characteristic = characteristic; 
	}

	public String brand() {
		return brand;
	}

	public int brandId() {
		return brandId;
	}

	public String model() {
		return model;
	}

	public int vehicleId() {
		return vehicleId;
	}

	public String category() {
		return category;
	}
	
	public String characteristic() {
		return characteristic;
	}

	public String displayName() {
		return brand + " " + model;
	}

	public static String toPromptList() {
		return Arrays.stream(values())
				.map(car -> "{\"brandId\":%d,\"vehicleId\":%d,\"brandName\":%s,\"vehicleName\":%s,\"displayName\":%s,\"category\":%s,\"characteristic\":%s}"
						.formatted(
								car.brandId(),
								car.vehicleId(),
								jsonString(car.brand()),
								jsonString(car.model()),
								jsonString(car.displayName()),
								jsonString(car.category()),
								jsonString(car.characteristic())))
				.collect(Collectors.joining(
						"," + System.lineSeparator(),
						"[" + System.lineSeparator(),
						System.lineSeparator() + "]"));
	}

	private static String jsonString(String value) {
		return "\"" + value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "\\r")
				.replace("\n", "\\n") + "\"";
	}
}
