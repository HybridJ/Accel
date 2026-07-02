package com.accel.api.video;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Video", description = "영상 관련 API")
@RestController
@RequestMapping("/videos")
public class VideoController {

	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	/**
	 * Video-01 // 카테고리 목록 조회
	 * GET /videos/categories
	 */
	@Operation(summary = "카테고리 목록 조회", description = "차량 카테고리 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/categories")
	public ResponseEntity<List<String>> categoryList() {
		List<String> categories = videoService.selectAllCategory();
		return ResponseEntity.ok(categories);
	}

	/**
	 * Video-02 // 카테고리별 영상 목록 조회 (필터 지원)
	 * GET /videos/categories/{category}?fuelType=EV&segment=소형&minPrice=20000000&maxPrice=50000000
	 */
	@Operation(summary = "카테고리별 차량 목록 조회", description = "카테고리 기반 조회. fuelType·segment·가격 범위 필터 선택 가능.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "결과 없음")
	})
	@GetMapping("/categories/{category}")
	public ResponseEntity<List<Vehicle>> videoList(
			@Parameter(description = "카테고리", example = "sedan") @PathVariable String category,
			@Parameter(description = "연료 타입 (다중 선택)", example = "EV") @RequestParam(name = "fuelType", required = false) List<String> fuelTypes,
			@Parameter(description = "세그먼트 (다중 선택)", example = "준중형") @RequestParam(name = "segment", required = false) List<String> segments,
			@Parameter(description = "최저 가격 (원)", example = "20000000") @RequestParam(required = false) Long minPrice,
			@Parameter(description = "최고 가격 (원)", example = "50000000") @RequestParam(required = false) Long maxPrice) {
		List<Vehicle> vehicles = videoService.searchVehicles(category, segments, fuelTypes, minPrice, maxPrice, null);
		if (vehicles != null && !vehicles.isEmpty()) {
			return ResponseEntity.ok(vehicles);
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * Video-04 // 차량 검색
	 * GET /videos/search?keyword=그랜저&category=sedan&fuelType=EV&segment=준중형&minPrice=X&maxPrice=Y
	 */
	@Operation(summary = "차량 검색", description = "차량명 키워드 및 카테고리·연료·세그먼트·가격 범위로 검색합니다. 모든 파라미터 선택 사항.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "결과 없음")
	})
	@GetMapping("/search")
	public ResponseEntity<List<Vehicle>> search(
			@Parameter(description = "차량명 키워드", example = "그랜저") @RequestParam(required = false) String keyword,
			@Parameter(description = "카테고리", example = "sedan") @RequestParam(required = false) String category,
			@Parameter(description = "연료 타입 (다중 선택)", example = "하이브리드") @RequestParam(name = "fuelType", required = false) List<String> fuelTypes,
			@Parameter(description = "세그먼트 (다중 선택)", example = "준대형") @RequestParam(name = "segment", required = false) List<String> segments,
			@Parameter(description = "최저 가격 (원)", example = "30000000") @RequestParam(required = false) Long minPrice,
			@Parameter(description = "최고 가격 (원)", example = "60000000") @RequestParam(required = false) Long maxPrice) {
		List<Vehicle> vehicles = videoService.searchVehicles(category, segments, fuelTypes, minPrice, maxPrice, keyword);
		if (vehicles != null && !vehicles.isEmpty()) {
			return ResponseEntity.ok(vehicles);
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * Video-03 // 영상 상세 조회
	 * GET /videos/{vehicleId}
	 */
	@Operation(summary = "차량·영상 상세 조회", description = "차량 ID로 차량 및 영상 정보를 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "차량을 찾을 수 없음")
	})
	@GetMapping("/{vehicleId}")
	public ResponseEntity<Vehicle> detail(
			@Parameter(description = "차량 ID", example = "1001") @PathVariable int vehicleId) {
		Vehicle vehicle = videoService.selectVehicle(vehicleId);
		if (vehicle != null) {
			return ResponseEntity.ok(vehicle);
		}
		return ResponseEntity.notFound().build();
	}
}
