<template>
    <main class="ev-infra-page">
        <section class="map-wrap" aria-label="전기차 충전소 지도">
            <div ref="mapContainer" class="kakao-map" aria-label="카카오맵"></div>

            <aside class="control-panel" aria-label="전기차 충전소 검색">
                <div class="search-panel">
                    <p>EV Infra</p>
                    <h1>가까운 전기차 충전소</h1>

                    <form class="search-form" @submit.prevent="searchLocation">
                        <input
                            v-model.trim="searchKeyword"
                            type="search"
                            placeholder="주소 또는 장소명"
                            autocomplete="off"
                        />
                        <button type="submit" :disabled="store.isLoading">검색</button>
                    </form>

                    <button class="current-location-button" type="button" @click="searchCurrentLocation">
                        현재 위치 (PC 접속 시 위치가 정확하지 않습니다)
                    </button>

                    <p v-if="statusMessage" class="status-message">{{ statusMessage }}</p>
                    <p v-else-if="store.errorMessage" class="status-message">{{ store.errorMessage }}</p>
                </div>

                <div v-if="store.stations.length > 0" class="station-list" aria-label="가까운 충전소 목록">
                    <article
                        v-for="(station, index) in store.stations"
                        :key="station.csId"
                        class="station-item"
                        @click="moveToStation(station)"
                    >
                        <div class="station-rank">{{ index + 1 }}</div>
                        <div class="station-info">
                            <h2>{{ station.csName }}</h2>
                            <p>{{ station.addr }}</p>
                            <div class="station-meta">
                                <span>{{ formatDistance(station.distanceKm) }}</span>
                                <span>{{ availableChargerCount(station) }} / {{ station.chargers?.length ?? 0 }} 사용 가능</span>
                            </div>
                            <ul v-if="station.chargers?.length" class="charger-list">
                                <li v-for="charger in station.chargers" :key="charger.cpId">
                                    <span>{{ charger.cpNm || `충전기 ${charger.cpId}` }}</span>
                                    <strong :class="['charger-status', statusClass(charger.cpStat)]">
                                        {{ chargerStatusLabel(charger.cpStat) }}
                                    </strong>
                                </li>
                            </ul>
                        </div>
                    </article>
                </div>
            </aside>
        </section>

        <section v-if="selectedLocation" class="score-action-section" aria-label="전기차 스코어 계산">
            <button type="button" :disabled="store.isScoreLoading" @click="requestEvScore">
                {{ store.isScoreLoading ? 'AI가 우리 동네 전기차 환경을 분석하는 중입니다' : '우리 동네 전기차 스코어' }}
            </button>
            <p v-if="scoreStatusMessage" class="score-status-message">{{ scoreStatusMessage }}</p>
            <p v-else-if="store.scoreErrorMessage" class="score-status-message">{{ store.scoreErrorMessage }}</p>
        </section>

        <section v-if="store.scoreResult" class="score-report" aria-label="우리 동네 전기차 스코어 리포트">
            <div class="overall-gauge-card">
                <div class="overall-gauge" :style="overallGaugeStyle">
                    <div class="overall-gauge-inner">
                        <strong>{{ roundedOverallScore }}</strong>
                        <span>{{ store.scoreResult.grade }}</span>
                    </div>
                </div>
                <div class="overall-copy">
                    <p>종합 평균 점수</p>
                    <h2>{{ store.scoreResult.overallOpinion }}</h2>
                </div>
            </div>

            <div class="score-item-grid">
                <article
                    v-for="(item, index) in store.scoreResult.items"
                    :key="item.category"
                    class="score-item-card"
                >
                    <div class="item-card-head">
                        <h3>{{ item.category }}</h3>
                        <strong>{{ item.score }}</strong>
                    </div>

                    <div class="item-gauge" aria-hidden="true">
                        <span :style="{ width: `${animatedItemScores[index] ?? 0}%` }"></span>
                    </div>
                </article>
            </div>
        </section>
    </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useEvStore } from '@/stores/evStore'

// Kakao 지도 SDK script 태그를 중복으로 만들지 않기 위해 고정 id로 관리합니다.
const KAKAO_MAP_SCRIPT_ID = 'kakao-map-sdk'
const KAKAO_MAP_LOAD_ERROR_MESSAGE =
    '카카오맵 SDK를 불러오지 못했습니다. 관리자에게 도메인 등록 상태 확인을 요청해주세요.'

// 사용자가 검색하기 전 지도에 보여줄 기본 위치입니다.
const DEFAULT_CENTER = {
    lat: 37.566826,
    longi: 126.9786567,
}

// EV 충전소 API 응답과 로딩 상태는 Pinia store에서 관리합니다.
const store = useEvStore()
const mapContainer = ref(null)
const searchKeyword = ref('')
const statusMessage = ref('')
const scoreStatusMessage = ref('')
const selectedLocation = ref(null)
const animatedOverallScore = ref(0)
const animatedItemScores = ref([])

// Kakao 지도 객체와 마커들은 여러 함수에서 재사용하므로 일반 변수로 보관합니다.
let map = null
let selectedMarker = null
let stationMarkers = []

const roundedOverallScore = computed(() => Math.round(store.scoreResult?.overallScore ?? 0))

const overallGaugeStyle = computed(() => ({
    '--score-angle': `${animatedOverallScore.value * 3.6}deg`,
}))

// Kakao Maps SDK와 services 라이브러리를 불러옵니다.
// services 라이브러리가 있어야 장소명 검색(keywordSearch)과 주소 검색(addressSearch)을 사용할 수 있습니다.
const createKakaoMapLoadError = () => {
    console.error(
        `[EV Infra] Kakao Maps SDK load failed for ${window.location.origin}. ` +
            'Check Kakao Developers > Platform > Web site domain settings.'
    )
    return new Error(KAKAO_MAP_LOAD_ERROR_MESSAGE)
}

const resolveKakaoMaps = () => {
    return new Promise((resolve, reject) => {
        if (!window.kakao?.maps?.load) {
            reject(createKakaoMapLoadError())
            return
        }

        window.kakao.maps.load(() => {
            if (!window.kakao?.maps?.services) {
                reject(new Error('카카오맵 services 라이브러리가 필요합니다.'))
                return
            }

            resolve(window.kakao)
        })
    })
}

const loadKakaoMapScript = () => {
    const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY

    if (!appKey) {
        return Promise.reject(new Error('VITE_KAKAO_MAP_APP_KEY가 설정되지 않았습니다.'))
    }

    if (window.kakao?.maps?.services) {
        return Promise.resolve(window.kakao)
    }

    if (window.kakao?.maps?.load) {
        return resolveKakaoMaps()
    }

    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID)

    if (existingScript) {
        return new Promise((resolve, reject) => {
            existingScript.addEventListener('load', () => resolveKakaoMaps().then(resolve).catch(reject), { once: true })
            existingScript.addEventListener('error', () => reject(createKakaoMapLoadError()), { once: true })
        })
    }

    return new Promise((resolve, reject) => {
        const script = document.createElement('script')
        script.id = KAKAO_MAP_SCRIPT_ID
        script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&libraries=services&autoload=false`
        script.onload = () => resolveKakaoMaps().then(resolve).catch(reject)
        script.onerror = () => reject(createKakaoMapLoadError())
        document.head.appendChild(script)
    })
}

// 페이지 진입 시 지도를 최초 생성하고 확대/축소 컨트롤을 붙입니다.
const initMap = async () => {
    const kakao = await loadKakaoMapScript()
    const center = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.longi)

    map = new kakao.maps.Map(mapContainer.value, {
        center,
        level: 5,
    })

    const zoomControl = new kakao.maps.ZoomControl()
    map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT)
}

// 사용자가 입력한 주소 또는 장소명을 좌표로 변환한 뒤 충전소 검색까지 이어갑니다.
const searchLocation = async () => {
    const keyword = searchKeyword.value.trim()

    if (!keyword) {
        statusMessage.value = '검색어를 입력해주세요.'
        return
    }

    await searchByLocation(await findLocation(keyword))
}

// 브라우저 현재 위치 권한을 이용해 사용자의 현재 좌표 기준으로 충전소를 조회합니다.
const searchCurrentLocation = () => {
    if (!navigator.geolocation) {
        statusMessage.value = '현재 위치를 사용할 수 없습니다.'
        return
    }

    statusMessage.value = '현재 위치를 확인하는 중입니다.'

    navigator.geolocation.getCurrentPosition(
        async (position) => {
            await searchByLocation({
                name: '현재 위치',
                address: '',
                lat: position.coords.latitude,
                longi: position.coords.longitude,
            })
        },
        () => {
            statusMessage.value = '현재 위치를 확인하지 못했습니다.'
        },
        {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 5000,
        }
    )
}

// 사용자가 건물명, 상호명, 주소 중 무엇을 입력할지 알 수 없으므로 장소 검색을 먼저 시도하고,
// 장소 검색 결과가 없으면 도로명/지번 주소 검색으로 한 번 더 찾습니다.
const findLocation = async (keyword) => {
    statusMessage.value = '위치를 검색하는 중입니다.'

    try {
        return await searchPlace(keyword)
    } catch {
        return await searchAddress(keyword)
    }
}

// Kakao 장소 검색입니다. 건물명, 상호명, 역 이름 같은 키워드 입력을 처리합니다.
const searchPlace = (keyword) => {
    const kakao = window.kakao
    const places = new kakao.maps.services.Places()

    return new Promise((resolve, reject) => {
        places.keywordSearch(keyword, (results, status) => {
            if (status !== kakao.maps.services.Status.OK || results.length === 0) {
                reject(new Error('장소 검색 결과가 없습니다.'))
                return
            }

            const place = results[0]
            resolve({
                name: place.place_name,
                address: place.road_address_name || place.address_name,
                lat: Number(place.y),
                longi: Number(place.x),
            })
        })
    })
}

// Kakao 주소 검색입니다. 도로명 주소나 지번 주소 입력을 처리합니다.
const searchAddress = (keyword) => {
    const kakao = window.kakao
    const geocoder = new kakao.maps.services.Geocoder()

    return new Promise((resolve, reject) => {
        geocoder.addressSearch(keyword, (results, status) => {
            if (status !== kakao.maps.services.Status.OK || results.length === 0) {
                reject(new Error('위치를 찾지 못했습니다.'))
                return
            }

            const address = results[0]
            resolve({
                name: address.address_name,
                address: address.road_address?.address_name || address.address_name,
                lat: Number(address.y),
                longi: Number(address.x),
            })
        })
    })
}

// 프론트에서 확보한 좌표를 백엔드로 보내 가까운 충전소 3개를 받아오고,
// 지도 마커와 결과 목록이 함께 갱신되도록 후처리합니다.
const searchByLocation = async (location) => {
    if (!map) {
        await initMap()
    }

    try {
        statusMessage.value = '충전소 정보를 불러오는 중입니다.'
        selectedLocation.value = location
        scoreStatusMessage.value = ''
        store.resetScore()
        resetGaugeAnimation()
        setSelectedMarker(location)

        const stations = await store.fetchNearestStations({
            lat: location.lat,
            longi: location.longi,
        })

        setStationMarkers(stations)
        fitBounds(location, stations)
        statusMessage.value = stations.length > 0 ? '' : '가까운 충전소가 없습니다.'
    } catch (error) {
        statusMessage.value = error.message || '충전소 정보를 불러오지 못했습니다.'
    }
}

const requestEvScore = async () => {
    if (!selectedLocation.value) {
        scoreStatusMessage.value = '먼저 위치를 선택해주세요.'
        return
    }

    if (store.isScoreLoading) {
        return
    }

    try {
        scoreStatusMessage.value = 'AI가 주변 전기차 환경을 분석하는 중입니다.'
        resetGaugeAnimation()
        const result = await store.fetchEvScore({
            lat: selectedLocation.value.lat,
            longi: selectedLocation.value.longi,
            address: selectedLocation.value.address,
        })
        scoreStatusMessage.value = ''
        animateScore(result)
    } catch {
        scoreStatusMessage.value = store.scoreErrorMessage || '전기차 스코어 계산에 실패했습니다.'
    }
}

const resetGaugeAnimation = () => {
    animatedOverallScore.value = 0
    animatedItemScores.value = []
}

const animateScore = async (result) => {
    await nextTick()

    requestAnimationFrame(() => {
        animatedOverallScore.value = Number(result.overallScore) || 0
        animatedItemScores.value = result.items?.map((item) => Number(item.score) || 0) ?? []
    })
}

// 사용자가 검색한 위치를 지도 위에 하나의 기준 마커로 표시합니다.
const setSelectedMarker = (location) => {
    const kakao = window.kakao
    const position = new kakao.maps.LatLng(location.lat, location.longi)

    if (!selectedMarker) {
        selectedMarker = new kakao.maps.Marker({
            map,
            position,
            title: location.name,
        })
        return
    }

    selectedMarker.setPosition(position)
    selectedMarker.setTitle(location.name)
}

// 백엔드에서 받은 충전소 목록을 지도 마커로 표시합니다.
const setStationMarkers = (stations) => {
    const kakao = window.kakao
    clearStationMarkers()

    stationMarkers = stations
        .map((station) => {
            const lat = Number(station.lat)
            const longi = Number(station.longi)

            if (!Number.isFinite(lat) || !Number.isFinite(longi)) {
                return null
            }

            return new kakao.maps.Marker({
                map,
                position: new kakao.maps.LatLng(lat, longi),
                title: station.csName,
            })
        })
        .filter(Boolean)
}

// 새 검색 결과를 표시하기 전에 이전 충전소 마커들을 지도에서 제거합니다.
const clearStationMarkers = () => {
    stationMarkers.forEach((marker) => marker.setMap(null))
    stationMarkers = []
}

// 검색 기준 위치와 가까운 충전소 3개가 한 화면에 들어오도록 지도 범위를 조정합니다.
const fitBounds = (location, stations) => {
    const kakao = window.kakao
    const bounds = new kakao.maps.LatLngBounds()

    bounds.extend(new kakao.maps.LatLng(location.lat, location.longi))

    stations.forEach((station) => {
        const lat = Number(station.lat)
        const longi = Number(station.longi)

        if (Number.isFinite(lat) && Number.isFinite(longi)) {
            bounds.extend(new kakao.maps.LatLng(lat, longi))
        }
    })

    map.setBounds(bounds)
}

// 결과 목록에서 충전소를 클릭하면 해당 충전소 위치로 지도를 이동시킵니다.
const moveToStation = (station) => {
    const kakao = window.kakao
    const lat = Number(station.lat)
    const longi = Number(station.longi)

    if (!map || !Number.isFinite(lat) || !Number.isFinite(longi)) {
        return
    }

    map.panTo(new kakao.maps.LatLng(lat, longi))
}

// 백엔드가 내려준 km 단위 거리를 화면에 보기 좋은 m/km 단위로 변환합니다.
const formatDistance = (distanceKm) => {
    const distance = Number(distanceKm)

    if (!Number.isFinite(distance)) {
        return ''
    }

    if (distance < 1) {
        return `${Math.round(distance * 1000)}m`
    }

    return `${distance.toFixed(1)}km`
}

// 공공데이터 상태 코드 기준으로 사용 가능한 충전기 수를 계산합니다.
const availableChargerCount = (station) => {
    return station.chargers?.filter((charger) => charger.cpStat === '1').length ?? 0
}

// 공공데이터 충전기 상태 코드를 화면에 표시할 문구로 변환합니다.
const chargerStatusLabel = (status) => {
    const labels = {
        1: '사용 가능',
        2: '충전 중',
        3: '점검',
        4: '통신 장애',
        5: '미연결',
        6: '충전 종료',
        7: '예약',
    }

    return labels[status] || '상태 미상'
}

// 충전기 상태에 따라 목록에서 다른 색상으로 보이도록 CSS 클래스를 반환합니다.
const statusClass = (status) => {
    if (status === '1') {
        return 'is-available'
    }

    if (status === '2') {
        return 'is-busy'
    }

    return 'is-unavailable'
}

// 페이지에 들어오면 검색 전이라도 지도를 먼저 준비합니다.
onMounted(async () => {
    try {
        await initMap()
        statusMessage.value = ''
    } catch (error) {
        statusMessage.value = error.message
    }
})
</script>

<style scoped>
@property --score-angle {
    syntax: '<angle>';
    inherits: false;
    initial-value: 0deg;
}

.ev-infra-page {
    width: 100%;
    min-height: 100vh;
    box-sizing: border-box;
    padding: 128px 24px 56px;
    background-color: #000000;
}

.map-wrap {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(320px, 400px);
    gap: 18px;
    width: min(100%, 1180px);
    min-height: 680px;
    margin: 0 auto;
    align-items: stretch;
}

.search-panel,
.station-list {
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    background-color: rgba(0, 0, 0, 0.84);
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(8px);
}

.search-panel {
    box-sizing: border-box;
    padding: 18px;
}

.control-panel {
    display: flex;
    flex-direction: column;
    min-height: 680px;
    max-height: 680px;
    gap: 14px;
}

.search-panel p {
    margin: 0 0 6px;
    color: var(--neon-cyan);
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0;
    text-transform: uppercase;
}

.search-panel h1 {
    margin: 0 0 16px;
    color: var(--neon-text);
    font-size: 26px;
    font-weight: 800;
    line-height: 1.25;
}

.search-form {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 72px;
    gap: 8px;
}

.search-form input {
    min-width: 0;
    height: 42px;
    box-sizing: border-box;
    padding: 0 12px;
    border: 1px solid rgba(4, 217, 255, 0.42);
    border-radius: 6px;
    background-color: rgba(0, 0, 0, 0.72);
    color: var(--neon-text);
    font-size: 15px;
    outline: none;
}

.search-form input:focus {
    border-color: var(--neon-cyan);
    box-shadow: 0 0 14px rgba(4, 217, 255, 0.28);
}

.search-form button,
.current-location-button {
    height: 42px;
    border: 1px solid var(--neon-cyan);
    border-radius: 6px;
    background-color: rgba(4, 217, 255, 0.12);
    color: var(--neon-text);
    font-weight: 800;
    cursor: pointer;
}

.search-form button:disabled {
    cursor: wait;
    opacity: 0.6;
}

.current-location-button {
    width: 100%;
    margin-top: 8px;
}

.status-message {
    margin: 12px 0 0;
    color: var(--muted-blue);
    font-size: 14px;
    line-height: 1.45;
}

.kakao-map {
    width: 100%;
    min-height: 680px;
    overflow: hidden;
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    background-color: rgba(4, 217, 255, 0.08);
    box-shadow: var(--glow-blue);
}

.station-list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 10px;
}

.station-item {
    display: grid;
    grid-template-columns: 32px minmax(0, 1fr);
    gap: 12px;
    padding: 12px;
    border-bottom: 1px solid rgba(4, 217, 255, 0.18);
    cursor: pointer;
}

.station-item:last-child {
    border-bottom: 0;
}

.station-item:hover {
    background-color: rgba(4, 217, 255, 0.08);
}

.station-rank {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: 1px solid var(--neon-cyan);
    border-radius: 50%;
    color: var(--neon-cyan);
    font-weight: 900;
}

.station-info {
    min-width: 0;
}

.station-info h2 {
    margin: 0;
    color: var(--neon-text);
    font-size: 17px;
    line-height: 1.35;
}

.station-info p {
    margin: 5px 0 0;
    color: var(--muted-blue);
    font-size: 13px;
    line-height: 1.4;
}

.station-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
    color: var(--neon-cyan);
    font-size: 13px;
    font-weight: 800;
}

.charger-list {
    display: grid;
    gap: 5px;
    margin: 10px 0 0;
    padding: 0;
    list-style: none;
}

.charger-list li {
    display: flex;
    justify-content: space-between;
    gap: 10px;
    color: var(--muted-blue);
    font-size: 12px;
}

.charger-status {
    flex: 0 0 auto;
}

.charger-status.is-available {
    color: #5cffb0;
}

.charger-status.is-busy {
    color: #ffd166;
}

.charger-status.is-unavailable {
    color: #ff7a90;
}

.score-action-section {
    width: min(100%, 1180px);
    margin: 28px auto 0;
    text-align: center;
}

.score-action-section button {
    width: 100%;
    height: 56px;
    border: 1px solid var(--neon-cyan);
    border-radius: 8px;
    background: linear-gradient(135deg, rgba(35, 35, 255, 0.9), rgba(4, 217, 255, 0.22));
    color: var(--neon-text);
    font-size: 18px;
    font-weight: 900;
    cursor: pointer;
    box-shadow: var(--glow-blue);
}

.score-action-section button:disabled {
    cursor: wait;
    opacity: 0.62;
}

.score-status-message {
    margin: 14px 0 0;
    color: var(--muted-blue);
    font-size: 14px;
    line-height: 1.45;
}

.score-report {
    width: min(100%, 1180px);
    margin: 42px auto 0;
}

.overall-gauge-card,
.score-item-card {
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    background-color: rgba(0, 0, 0, 0.84);
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(8px);
}

.overall-gauge-card {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 42px;
    padding: 34px;
}

.overall-gauge {
    width: 210px;
    aspect-ratio: 1;
    display: grid;
    place-items: center;
    flex: 0 0 auto;
    border-radius: 50%;
    background: conic-gradient(var(--neon-cyan) var(--score-angle), rgba(4, 217, 255, 0.12) 0deg);
    box-shadow: 0 0 34px rgba(4, 217, 255, 0.22);
    transition: --score-angle 1.1s ease;
}

.overall-gauge-inner {
    width: 148px;
    aspect-ratio: 1;
    display: grid;
    place-items: center;
    border-radius: 50%;
    background: #000000;
    border: 1px solid rgba(4, 217, 255, 0.22);
}

.overall-gauge-inner strong {
    align-self: end;
    color: #ffffff;
    font-size: 48px;
    line-height: 1;
}

.overall-gauge-inner span {
    align-self: start;
    color: var(--neon-cyan);
    font-size: 28px;
    font-weight: 900;
}

.overall-copy {
    max-width: 620px;
    text-align: left;
}

.overall-copy p {
    margin: 0 0 8px;
    color: var(--neon-cyan);
    font-weight: 800;
}

.overall-copy h2 {
    margin: 0;
    color: var(--neon-text);
    font-size: 26px;
    line-height: 1.45;
}

.score-item-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
    margin-top: 24px;
}

.score-item-card {
    padding: 24px;
}

.item-card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.item-card-head h3 {
    margin: 0;
    color: var(--neon-text);
    font-size: 20px;
    line-height: 1.35;
}

.item-card-head strong {
    color: var(--neon-cyan);
    font-size: 30px;
}

.item-gauge {
    height: 12px;
    margin: 18px 0;
    overflow: hidden;
    border-radius: 999px;
    background-color: rgba(4, 217, 255, 0.12);
}

.item-gauge span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--neon-blue), var(--neon-cyan));
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.36);
    transition: width 1s ease;
}

@media (max-width: 760px) {
    .ev-infra-page {
        padding: 112px 16px 40px;
    }

    .map-wrap {
        grid-template-columns: 1fr;
        gap: 14px;
        min-height: 0;
    }

    .kakao-map {
        min-height: 460px;
    }

    .control-panel {
        min-height: 0;
        max-height: none;
    }

    .search-panel h1 {
        font-size: 22px;
    }

    .station-list {
        max-height: 360px;
    }

    .overall-gauge-card {
        flex-direction: column;
        gap: 24px;
        padding: 28px 20px;
        text-align: center;
    }

    .overall-copy {
        text-align: center;
    }

    .overall-copy h2 {
        font-size: 22px;
    }

    .score-item-grid {
        grid-template-columns: 1fr;
    }
}
</style>
