package com.accel.api.ev.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.accel.api.ev.dto.EvChargeInfo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * KEPCO 전기차 충전소 운영정보 API 호출 클라이언트.
 */
@Component
public class KepcoEvApiClient {

	@Value("${kepco.ev.api.url}")
	private String apiUrl;

	@Value("${kepco.ev.api.key}")
	private String apiKey;

	@Value("${kepco.ev.api.addr:}")
	private String addr;

	private final RestClient restClient = RestClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public List<EvChargeInfo> fetch() {
		System.out.println("[EV-FETCH]" + System.currentTimeMillis()
				+ " 충전소 운영정보 수집 시작 (addr=" + (addr.isBlank() ? "전체" : addr) + ")");

		if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("여기에")) {
			throw new IllegalStateException("[EV-FETCH] kepco.ev.api.key 가 설정되지 않았습니다. application.properties 확인");
		}

		URI uri = UriComponentsBuilder.fromUriString(apiUrl)
				.queryParam("apiKey", apiKey)
				.queryParam("returnType", "json")
				.queryParamIfPresent("addr", Optional.ofNullable(addr).filter(s -> !s.isBlank()))
				.encode(StandardCharsets.UTF_8)
				.build(true)
				.toUri();

		String json = restClient.get()
				.uri(uri)
				.accept(MediaType.ALL)
				.retrieve()
				.body(String.class);

		if (json == null || json.isBlank()) {
			throw new IllegalStateException("[EV-FETCH] 응답 바디가 비어있습니다. (URL/네트워크/인증키 확인)");
		}

		List<EvChargeInfo> list = parseData(json);
		if (list.isEmpty()) {
			throw new IllegalStateException("[EV-FETCH] data 배열이 비어있습니다. 응답=" + shrink(json));
		}

		System.out.println("[EV-FETCH] 총 " + list.size() + "건 수집 완료");
		for (int i = 0; i < Math.min(3, list.size()); i++) {
			System.out.println("[EV-FETCH] 샘플: " + list.get(i));
		}
		return list;
	}

	private List<EvChargeInfo> parseData(String json) {
		JsonNode root = objectMapper.readTree(json);
		JsonNode arr = root.isArray() ? root : root.get("data");

		if (arr == null || !arr.isArray()) {
			throw new IllegalStateException("[EV-FETCH] 응답에 data 배열이 없습니다. 응답=" + shrink(json));
		}

		List<EvChargeInfo> list = new ArrayList<>();
		for (JsonNode n : arr) {
			EvChargeInfo e = new EvChargeInfo();
			e.setCsId(text(n, "csId"));
			e.setCsNm(text(n, "csNm"));
			e.setAddr(text(n, "addr"));
			e.setLat(text(n, "lat"));
			e.setLongi(text(n, "longi"));
			e.setCpId(text(n, "cpId"));
			e.setCpNm(text(n, "cpNm"));
			e.setChargeTp(text(n, "chargeTp"));
			e.setCpTp(text(n, "cpTp"));
			e.setCpStat(text(n, "cpStat"));
			e.setStatUpdateDatetime(text(n, "statUpdateDatetime"));
			list.add(e);
		}
		return list;
	}

	private String text(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? "" : v.asString("");
	}

	private String shrink(String s) {
		int max = 500;
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
