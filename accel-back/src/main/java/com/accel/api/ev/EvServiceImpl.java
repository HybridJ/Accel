package com.accel.api.ev;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvServiceImpl implements EvService {

	private static final int NEAREST_STATION_LIMIT = 3;
	private static final Pattern NEIGHBORHOOD_PATTERN = Pattern.compile("[가-힣0-9]+(?:읍|면|동|가|리)");

	private final EvDao evDao;

	@Override
	public List<Ev> findNearestStationsByCoordinate(double lat, double longi) {
		validateCoordinate(lat, longi);
		return evDao.selectNearestStations(lat, longi, NEAREST_STATION_LIMIT);
	}

	@Override
	public List<Ev> findStationsInSameNeighborhood(double lat, double longi, String address) {
		validateCoordinate(lat, longi);

		Optional<String> neighborhood = extractNeighborhood(address);

		if (neighborhood.isEmpty()) {
			neighborhood = findNearestStationsByCoordinate(lat, longi).stream()
					.map(Ev::getAddr)
					.map(this::extractNeighborhood)
					.flatMap(Optional::stream)
					.findFirst();
		}

		return neighborhood
				.map(value -> evDao.selectStationsByNeighborhood(lat, longi, value))
				.filter(stations -> !stations.isEmpty())
				.orElseGet(() -> findNearestStationsByCoordinate(lat, longi));
	}

	private Optional<String> extractNeighborhood(String address) {
		if (!StringUtils.hasText(address)) {
			return Optional.empty();
		}

		Matcher matcher = NEIGHBORHOOD_PATTERN.matcher(address);
		String neighborhood = null;

		while (matcher.find()) {
			neighborhood = matcher.group();
		}

		return Optional.ofNullable(neighborhood);
	}

	private void validateCoordinate(double lat, double longi) {
		if (lat < -90 || lat > 90) {
			throw new IllegalArgumentException("lat must be between -90 and 90.");
		}
		if (longi < -180 || longi > 180) {
			throw new IllegalArgumentException("longi must be between -180 and 180.");
		}
	}
}
