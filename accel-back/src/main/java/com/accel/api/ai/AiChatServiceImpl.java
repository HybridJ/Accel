package com.accel.api.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.accel.api.ev.Ev;
import com.accel.api.ev.EvService;
import com.accel.api.video.Vehicle;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

	private static final String DEFAULT_MESSAGE = "No Answer Included.";
	private static final List<String> EV_SCORE_CATEGORIES = List.of(
			"충전 인프라 충분도",
			"급속 충전 접근성",
			"충전소 접근성과 분산도",
			"주거 및 생활 편의 충전 환경",
			"지역 전기차 지원 환경");
	private static final Set<String> EV_SCORE_ROOT_REQUIRED_KEYS = Set.of("items", "overallOpinion");
	private static final Set<String> EV_SCORE_ITEM_REQUIRED_KEYS = Set.of("category", "score");

	private final ChatClient.Builder chatClientBuilder;
	private final ObjectProvider<ToolCallbackProvider> mcpToolCallbackProvider;
	private final ObjectMapper objectMapper;
	private final AiDao aiDao;
	private final EvService evService;

	@Override
	public AiResponse carbti(AiRequest request) {
		String message = normalizeMessage(request);

		String systemInstruction = """
				You are ACCEL's playful CarBTI vehicle personality analyst.
				Based on the user's survey choices, choose exactly one vehicle from the service vehicle list.

				[Service vehicle list]
				%s

				[Response rules]
				- Return only one valid JSON object. No markdown, code fences, comments, or text outside JSON.
				- The JSON object must contain exactly these keys: "brandId", "vehicleId", "model", "summary", "resaon".
				- brandId and vehicleId must be JSON numbers copied exactly from the selected service vehicle.
				- model must be the selected vehicle's brandName and vehicleName joined with one space.
				- summary must be exactly the selected vehicle's characteristic text from the service vehicle list.
				- resaon must be a playful Korean CarBTI explanation that connects the survey answers to the vehicle personality.
				- Do not recommend purchase, judge price, or mention hidden reasoning.
				""".formatted(RecommendedCarModel.toPromptList());

		String userMessage = """
				[Survey answers]
				%s
				""".formatted(message);

		String answer = chatClientBuilder.build()
				.prompt()
				.system(systemInstruction)
				.user(userMessage)
				.call()
				.content();

		return new AiResponse(stripMarkdownCodeBlock(answer));
	}

	@Override
	public AiResponse carsulting(AiRequest request) {
		String message = normalizeMessage(request);
		CarsultingTools carsultingTools = new CarsultingTools(aiDao);

		String systemInstruction = """
				You are ACCEL's vehicle purchase consulting AI.
				Recommend one vehicle that exists in ACCEL service vehicle information.

				[Tool usage rules]
				- First call getServiceBrands to inspect available brands.
				- Choose one promising brand from the user's budget, usage, lifestyle, driving environment, and EV preference.
				- Then call getVehiclesByBrandId with that brandId.
				- Recommend exactly one vehicle from the returned vehicles.
				- You may use any MCP tools made available by the application only when they help the recommendation, but never invent service vehicles.

				[Recommendation rules]
				- If a normal recommendation is possible, brandId and vehicleId must be copied exactly from the selected vehicle.
				- model must be "brandName vehicleName".
				- summary must be copied from the selected vehicle's summary.
				- resaon must be a friendly Korean explanation connecting the consultation answers to the selected vehicle.
				- If recommendation is not possible, return brandId 0, vehicleId 0, model "ACCEL Recommended Car",
				  summary "추천을 완료하지 못했습니다.", and explain what information should be supplemented.
				- Do not mention DB, database, Tool, MCP, or implementation details to the user.

				[JSON only response rules]
				- Return only one valid JSON object. No markdown, code fences, comments, or text outside JSON.
				- The JSON object must contain exactly these keys: "brandId", "vehicleId", "model", "summary", "resaon".
				- brandId and vehicleId must be JSON numbers, not strings.
				""";

		String userMessage = """
				[Consultation answers]
				%s
				""".formatted(message);

		Object[] tools = carsultingToolObjects(carsultingTools);
		String answer = chatClientBuilder.build()
				.prompt()
				.system(systemInstruction)
				.user(userMessage)
				.tools(tools)
				.call()
				.content();

		return new AiResponse(stripMarkdownCodeBlock(answer));
	}

	@Override
	public EvScoreResponse evScore(EvScoreRequest request) {
		validateEvScoreRequest(request);

		List<Ev> neighborhoodStations = evService.findStationsInSameNeighborhood(
				request.getLatitude(),
				request.getLongitude(),
				request.getAddress());

		return buildDeterministicEvScore(request, neighborhoodStations);
	}

	private EvScoreResponse buildDeterministicEvScore(EvScoreRequest request, List<Ev> neighborhoodStations) {
		EvScoreResponse response = new EvScoreResponse();
		EvScoreResponse.Location location = new EvScoreResponse.Location();
		location.setLatitude(request.getLatitude());
		location.setLongitude(request.getLongitude());
		response.setLocation(location);

		int totalChargers = countTotalChargers(neighborhoodStations);
		int availableChargers = countChargersByStatus(neighborhoodStations, "1");
		int fastChargers = countFastChargers(neighborhoodStations);
		double nearestDistanceKm = nearestDistanceKm(neighborhoodStations);
		double availabilityRatio = totalChargers == 0 ? 0 : (double) availableChargers / totalChargers;

		List<Integer> scores = List.of(
				scoreInfrastructure(neighborhoodStations.size(), totalChargers, availabilityRatio),
				scoreFastChargingAccess(fastChargers, totalChargers, nearestDistanceKm),
				scoreDistance(nearestDistanceKm),
				scoreResidentialConvenience(nearestDistanceKm, availabilityRatio),
				scoreLocalReadiness(neighborhoodStations.size(), totalChargers, availableChargers, fastChargers));

		for (int i = 0; i < EV_SCORE_CATEGORIES.size(); i++) {
			EvScoreResponse.EvaluationItem item = new EvScoreResponse.EvaluationItem();
			item.setCategory(EV_SCORE_CATEGORIES.get(i));
			item.setScore(scores.get(i));
			response.getItems().add(item);
		}

		double averageScore = scores.stream()
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0);

		response.setOverallScore(Math.round(averageScore * 10) / 10.0);
		response.setGrade(toEvScoreGrade(averageScore));
		response.setOverallOpinion(buildEvScoreOpinion(
				neighborhoodStations.size(),
				totalChargers,
				availableChargers,
				nearestDistanceKm));

		return response;
	}

	private int countTotalChargers(List<Ev> stations) {
		return stations.stream()
				.filter(station -> station.getChargers() != null)
				.mapToInt(station -> station.getChargers().size())
				.sum();
	}

	private int countChargersByStatus(List<Ev> stations, String status) {
		return stations.stream()
				.filter(station -> station.getChargers() != null)
				.mapToInt(station -> (int) station.getChargers().stream()
						.filter(charger -> status.equals(StringUtils.trimWhitespace(charger.getCpStat())))
						.count())
				.sum();
	}

	private int countFastChargers(List<Ev> stations) {
		return stations.stream()
				.filter(station -> station.getChargers() != null)
				.mapToInt(station -> (int) station.getChargers().stream()
						.filter(charger -> "1".equals(StringUtils.trimWhitespace(charger.getChargeTp())))
						.count())
				.sum();
	}

	private double nearestDistanceKm(List<Ev> stations) {
		return stations.stream()
				.map(Ev::getDistanceKm)
				.filter(distance -> distance != null && distance >= 0)
				.mapToDouble(Double::doubleValue)
				.min()
				.orElse(99);
	}

	private int scoreInfrastructure(int stationCount, int totalChargers, double availabilityRatio) {
		return clampScore(20 + stationCount * 7 + totalChargers * 3 + (int) Math.round(availabilityRatio * 25));
	}

	private int scoreFastChargingAccess(int fastChargers, int totalChargers, double nearestDistanceKm) {
		int distanceScore = scoreDistance(nearestDistanceKm);
		double fastRatio = totalChargers == 0 ? 0 : (double) fastChargers / totalChargers;
		return clampScore(distanceScore - 20 + fastChargers * 5 + (int) Math.round(fastRatio * 25));
	}

	private int scoreDistance(double nearestDistanceKm) {
		if (nearestDistanceKm <= 0.5) {
			return 95;
		}
		if (nearestDistanceKm <= 1.0) {
			return 88;
		}
		if (nearestDistanceKm <= 2.0) {
			return 76;
		}
		if (nearestDistanceKm <= 3.0) {
			return 65;
		}
		if (nearestDistanceKm <= 5.0) {
			return 52;
		}
		return 35;
	}

	private int scoreResidentialConvenience(double nearestDistanceKm, double availabilityRatio) {
		return clampScore(scoreDistance(nearestDistanceKm) - 15 + (int) Math.round(availabilityRatio * 35));
	}

	private int scoreLocalReadiness(int stationCount, int totalChargers, int availableChargers, int fastChargers) {
		return clampScore(25 + stationCount * 6 + totalChargers * 2 + availableChargers * 3 + fastChargers * 2);
	}

	private int clampScore(int score) {
		return Math.max(0, Math.min(100, score));
	}

	private String buildEvScoreOpinion(int stationCount, int totalChargers, int availableChargers, double nearestDistanceKm) {
		if (stationCount == 0) {
			return "주변에서 사용 가능한 충전소를 찾지 못해 전기차 이용 편의성이 낮게 평가되었습니다.";
		}

		return "같은 동네의 충전소 %d곳과 충전기 %d기 중 사용 가능 %d기를 기준으로 평가했습니다. 가장 가까운 충전소는 약 %.1fkm 거리에 있습니다."
				.formatted(stationCount, totalChargers, availableChargers, nearestDistanceKm);
	}

	@SuppressWarnings("unused")
	private EvScoreResponse evScoreWithOpenAi(EvScoreRequest request) {
		String systemInstruction = """
				Return only minified JSON. No markdown. No explanation.
				Schema:
				{"items":[{"category":"충전 인프라 충분도","score":0},{"category":"급속 충전 접근성","score":0},{"category":"충전소 접근성과 분산도","score":0},{"category":"주거 및 생활 편의 충전 환경","score":0},{"category":"지역 전기차 지원 환경","score":0}],"overallOpinion":"60자 이내 한국어 한 문장"}
				Each score must be an integer from 0 to 100. Do not include average or grade.
				""";

		String userMessage = """
				좌표 주변 EV 충전 환경을 간단히 평가해 주세요.
				lat=%.6f,lng=%.6f
				""".formatted(request.getLatitude(), request.getLongitude());

		String answer;
		try {
			answer = chatClientBuilder.build()
					.prompt()
					.system(systemInstruction)
					.user(userMessage)
					.call()
					.content();
		} catch (RuntimeException e) {
			throw new EvScoreException(
					"OpenAI API request failed while scoring EV environment. exception=%s message=%s"
							.formatted(e.getClass().getSimpleName(), e.getMessage()),
					e);
		}

		try {
			return parseAndValidateEvScore(answer, request);
		} catch (JacksonException | IllegalArgumentException e) {
			throw new EvScoreException("Failed to parse OpenAI EV score response: " + e.getMessage(), e);
		}
	}

	private String normalizeMessage(AiRequest request) {
		String message = request == null ? null : request.message();
		return StringUtils.hasText(message) ? message : DEFAULT_MESSAGE;
	}

	private Object[] carsultingToolObjects(CarsultingTools carsultingTools) {
		ToolCallbackProvider provider = mcpToolCallbackProvider.getIfAvailable();

		if (provider == null) {
			return new Object[] { carsultingTools };
		}

		return new Object[] { carsultingTools, provider };
	}

	private EvScoreResponse parseAndValidateEvScore(String rawAnswer, EvScoreRequest request) throws JacksonException {
		String json = stripMarkdownCodeBlock(rawAnswer);
		validateNoDuplicateKeys(json);

		JsonNode root = objectMapper.readTree(json);
		validateRequiredRootKeys(root);
		validateRequiredItemKeys(root.path("items"));

		EvScoreResponse response = objectMapper.readValue(json, EvScoreResponse.class);
		validateEvScoreResponse(response);
		recalculateEvScore(response, request);

		return response;
	}

	private String stripMarkdownCodeBlock(String rawAnswer) {
		if (!StringUtils.hasText(rawAnswer)) {
			throw new IllegalArgumentException("AI response is empty.");
		}

		String text = rawAnswer.trim();

		if (text.startsWith("```")) {
			int firstLineEnd = text.indexOf('\n');
			int lastFence = text.lastIndexOf("```");

			if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
				text = text.substring(firstLineEnd + 1, lastFence).trim();
			}
		}

		return text;
	}

	private void validateNoDuplicateKeys(String json) throws JacksonException {
		Deque<Set<String>> keyStack = new ArrayDeque<>();

		try (JsonParser parser = objectMapper.createParser(json)) {
			JsonToken token;

			while ((token = parser.nextToken()) != null) {
				if (token == JsonToken.START_OBJECT) {
					keyStack.push(new HashSet<>());
					continue;
				}

				if (token == JsonToken.END_OBJECT) {
					keyStack.pop();
					continue;
				}

				if (token == JsonToken.PROPERTY_NAME && !keyStack.isEmpty()) {
					String key = parser.currentName();

					if (!keyStack.peek().add(key)) {
						throw new IllegalArgumentException("Duplicate JSON key: " + key);
					}
				}
			}
		}
	}

	private void validateRequiredRootKeys(JsonNode root) {
		if (!root.isObject()) {
			throw new IllegalArgumentException("AI response must be a JSON object.");
		}

		for (String key : EV_SCORE_ROOT_REQUIRED_KEYS) {
			if (!root.has(key) || root.path(key).isNull()) {
				throw new IllegalArgumentException("Missing required root key: " + key);
			}
		}
	}

	private void validateRequiredItemKeys(JsonNode items) {
		if (!items.isArray()) {
			throw new IllegalArgumentException("items must be an array.");
		}

		if (items.size() != EV_SCORE_CATEGORIES.size()) {
			throw new IllegalArgumentException("items must contain exactly 5 evaluation items.");
		}

		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.path(i);

			if (!item.isObject()) {
				throw new IllegalArgumentException("items[" + i + "] must be an object.");
			}

			for (String key : EV_SCORE_ITEM_REQUIRED_KEYS) {
				if (!item.has(key) || item.path(key).isNull()) {
					throw new IllegalArgumentException("Missing required item key: " + key);
				}
			}
		}
	}

	private void validateEvScoreResponse(EvScoreResponse response) {
		if (response.getItems() == null || response.getItems().size() != EV_SCORE_CATEGORIES.size()) {
			throw new IllegalArgumentException("Evaluation item count must be exactly 5.");
		}

		for (int i = 0; i < response.getItems().size(); i++) {
			EvScoreResponse.EvaluationItem item = response.getItems().get(i);

			if (item == null) {
				throw new IllegalArgumentException("Evaluation item must not be null.");
			}

			String expectedCategory = EV_SCORE_CATEGORIES.get(i);

			if (!expectedCategory.equals(item.getCategory())) {
				throw new IllegalArgumentException("Invalid evaluation category order: " + item.getCategory());
			}

			validateScoreRange("score", item.getScore());
		}

		if (!StringUtils.hasText(response.getOverallOpinion())) {
			throw new IllegalArgumentException("Response requires overallOpinion.");
		}
	}

	private void validateScoreRange(String fieldName, int score) {
		if (score < 0 || score > 100) {
			throw new IllegalArgumentException(fieldName + " must be between 0 and 100.");
		}
	}

	private void recalculateEvScore(EvScoreResponse response, EvScoreRequest request) {
		double averageScore = response.getItems().stream()
				.mapToInt(EvScoreResponse.EvaluationItem::getScore)
				.average()
				.orElse(0);

		EvScoreResponse.Location location = new EvScoreResponse.Location();
		location.setLatitude(request.getLatitude());
		location.setLongitude(request.getLongitude());

		response.setLocation(location);
		response.setOverallScore(Math.round(averageScore * 10) / 10.0);
		response.setGrade(toEvScoreGrade(averageScore));
	}

	private String toEvScoreGrade(double score) {
		if (score >= 90) {
			return "S";
		}

		if (score >= 80) {
			return "A";
		}

		if (score >= 70) {
			return "B";
		}

		if (score >= 60) {
			return "C";
		}

		return "D";
	}

	private void validateEvScoreRequest(EvScoreRequest request) {
		if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
			throw new IllegalArgumentException("latitude and longitude are required.");
		}

		double latitude = request.getLatitude();
		double longitude = request.getLongitude();

		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("latitude must be between -90 and 90.");
		}

		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("longitude must be between -180 and 180.");
		}
	}

	private static final class CarsultingTools {

		private final AiDao aiDao;

		private CarsultingTools(AiDao aiDao) {
			this.aiDao = aiDao;
		}

		@Tool(name = "getServiceBrands", description = "Return all vehicle brands currently available in ACCEL service vehicle information. Call this before selecting a brand.")
		public Map<String, Object> getServiceBrands() {
			List<Map<String, Object>> brands = aiDao.selectAllBrands().stream()
					.map(brand -> Map.<String, Object>of(
							"brandId", brand.getBrandId(),
							"brandName", brand.getBrandName()))
					.toList();

			return Map.of("brands", brands);
		}

		@Tool(name = "getVehiclesByBrandId", description = "Return ACCEL service vehicle models for the given brandId.")
		public Map<String, Object> getVehiclesByBrandId(
				@ToolParam(description = "brandId returned by getServiceBrands") int brandId) {

			if (brandId <= 0) {
				return Map.of("error", "brandId must be a positive integer.");
			}

			List<Map<String, Object>> vehicles = aiDao.selectVehiclesByBrandId(brandId).stream()
					.map(CarsultingTools::vehicleToolResponse)
					.toList();

			return Map.of(
					"brandId", brandId,
					"vehicles", vehicles);
		}

		private static Map<String, Object> vehicleToolResponse(Vehicle vehicle) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("vehicleId", vehicle.getVehicleId());
			item.put("brandId", vehicle.getBrandId());
			item.put("brandName", vehicle.getBrandName());
			item.put("vehicleName", vehicle.getVehicleName());
			item.put("category", vehicle.getCategory());
			item.put("segment", vehicle.getSegment());
			item.put("bodyType", vehicle.getBodyType());
			item.put("seating", vehicle.getSeating());
			item.put("minPrice", vehicle.getMinPrice());
			item.put("maxPrice", vehicle.getMaxPrice());
			item.put("fuelTypes", vehicle.getFuelTypes());
			item.put("summary", vehicle.getSummary());
			return item;
		}
	}
}
