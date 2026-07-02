import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { carCategories } from '@/constants/categories'
import axios from 'axios'
import { API_BASE_URL } from '@/constants/api'

export const useVideoStore = defineStore('video', () => {
  const REST_URL = `${API_BASE_URL}/videos`
  
  // 차량 카테고리 상수를 별도의 js 파일에 정의하여 사용 객체 배열 형태로 사용한다.
  const categories = ref(carCategories)
  // 차량 카테고리 별 영상들을 담는 배열
  const videos = ref([])
  const video = ref(null)
  // 상세 페이지 '같은 카테고리 추천 차량' 목록 (videos 와 분리)
  const recommendations = ref([])
  const category = ref('')
  const videoId = ref(-1)

  const setCategory = function(selectedCategory){
    category.value = selectedCategory
  }

  const setVideoId = function(selectedVideoId){
    videoId.value = selectedVideoId
  }

  const fetchShowroom = async (category) => {
    const response = await axios.get(`${REST_URL}/categories/${category}`)

    videos.value = response.data
  }

  const fetchVideo = async (videoId) => {
    const response = await axios.get(`${REST_URL}/${videoId}`)

    video.value = response.data
  }

  // 같은 카테고리 차량 목록을 추천용으로 조회 (showroom 의 videos 와 별도 보관)
  const fetchRecommendations = async (category) => {
    const response = await axios.get(`${REST_URL}/categories/${category}`)

    recommendations.value = response.data
  }

  // 유튜브 임베드/일반 URL에서 영상 ID(11자)를 추출해 썸네일 이미지 URL 생성
  // 목록 카드·상세 등 video 도메인 전역에서 공유해 사용한다.
  // quality: hqdefault | mqdefault | sddefault | maxresdefault
  const youtubeThumbnail = (url, quality = 'hqdefault') => {
    if (!url) return null
    const match = url.match(/(?:embed\/|v=|youtu\.be\/)([\w-]{11})/)
    return match ? `https://img.youtube.com/vi/${match[1]}/${quality}.jpg` : null
  }

  return {
    categories,
    videos,
    video,
    recommendations,
    category,
    videoId,
    setCategory,
    setVideoId,
    fetchShowroom,
    fetchVideo,
    fetchRecommendations,
    youtubeThumbnail,
  }
})
