import axios from 'axios'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { API_BASE_URL } from '@/constants/api'

export const useEvStore = defineStore('ev', () => {
    const REST_URL = `${API_BASE_URL}/ev`
    const AI_EV_URL = `${API_BASE_URL}/ai/ev`

    const stations = ref([])
    const scoreResult = ref(null)
    const isLoading = ref(false)
    const isScoreLoading = ref(false)
    const errorMessage = ref('')
    const scoreErrorMessage = ref('')

    const fetchNearestStations = async ({ lat, longi }) => {
        isLoading.value = true
        errorMessage.value = ''

        try {
            const response = await axios.get(`${REST_URL}/nearest`, {
                params: { lat, longi },
            })
            stations.value = response.data
            return stations.value
        } catch (error) {
            stations.value = []
            errorMessage.value = '충전소 정보를 불러오지 못했습니다.'
            throw error
        } finally {
            isLoading.value = false
        }
    }

    const resetStations = () => {
        stations.value = []
        errorMessage.value = ''
    }

    const fetchEvScore = async ({ lat, longi, address = '' }) => {
        isScoreLoading.value = true
        scoreErrorMessage.value = ''

        try {
            const response = await axios.post(`${AI_EV_URL}/score`, {
                latitude: lat,
                longitude: longi,
                address,
            })
            scoreResult.value = response.data
            return scoreResult.value
        } catch (error) {
            scoreResult.value = null
            scoreErrorMessage.value = error.response?.data?.message || '전기차 스코어를 계산하지 못했습니다.'
            throw error
        } finally {
            isScoreLoading.value = false
        }
    }

    const resetScore = () => {
        scoreResult.value = null
        scoreErrorMessage.value = ''
    }

    return {
        stations,
        scoreResult,
        isLoading,
        isScoreLoading,
        errorMessage,
        scoreErrorMessage,
        fetchNearestStations,
        fetchEvScore,
        resetStations,
        resetScore,
    }
})
