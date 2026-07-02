import HomeView from '@/views/HomeView.vue'
import AILoadingView from '@/views/ai/AILoadingView.vue'
import AIResultView from '@/views/ai/AIResultView.vue'
import AISurveyView from '@/views/ai/AISurveyView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'
import BoardArticleView from '@/views/boards/BoardArticleView.vue'
import BoardEditView from '@/views/boards/BoardEditView.vue'
import BoardWriteView from '@/views/boards/BoardWriteView.vue'
import BrandBoardView from '@/views/boards/BrandBoardView.vue'
import BrandCategoryView from '@/views/boards/BrandCategoryView.vue'
import EVInfraView from '@/views/ev/EVInfraView.vue'
import MyActivityView from '@/views/user/MyActivityView.vue'
import MyPageView from '@/views/user/MyPageView.vue'
import UpdateInfo from '@/views/user/UpdateInfo.vue'
import VideoArticleView from '@/views/videos/VideoArticleView.vue'
import VideoCategoryView from '@/views/videos/VideoCategoryView.vue'
import VideoShowroomView from '@/views/videos/VideoShowroomView.vue'
import { useAuthStore } from '@/stores/authStore'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    return { top: 0, left: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    /**
     * 회원 인증
     */
    {
      path: '/auth',
      children: [
        {
          path: 'login',
          name: 'login',
          component: LoginView
        },
        {
          path: 'register',
          name: 'register',
          component: RegisterView
        }
      ]
    },
    /**
     * 차량 영상 관람
     */
    {
      path: '/videos',
      children: [
        {
          path: 'categoryList',
          name: 'videoCategory',
          component: VideoCategoryView
        },
        {
          path: 'categories/:category',
          name: 'videoShowroom',
          component: VideoShowroomView
        },
        {
          path: ':vehicleId',
          name: 'videoArticle',
          component: VideoArticleView
        }
      ]
    },
    /**
     * 브랜드 별 커뮤니티
     */
    {
      path: '/boards',
      children: [
        {
          path: 'category',
          name: 'BrandCategory',
          component: BrandCategoryView
        },
        {
          path: ':brandId',
          name: 'BrandBoard',
          component: BrandBoardView
        },
        {
          path: ':brandId/write',
          name: 'BoardWrite',
          component: BoardWriteView,
          meta: { requiresAuth: true }
        },
        {
          path: 'articles/:articleId/edit',
          name: 'BoardEdit',
          component: BoardEditView,
          meta: { requiresAuth: true }
        },
        {
          path: 'articles/:articleId',
          name: 'BoardArticle',
          component: BoardArticleView
        }
      ]
    },
    /**
     * 전기차 충전 인프라 서비스
     */
    {
      path: '/ev/info',
      name: 'EVInfra',
      component: EVInfraView
    },
    {
      path: '/user',
      children: [
        {
          path: 'me',
          name: 'myPage',
          component: MyPageView,
          meta: { requiresAuth: true }
        },
        {
          path: 'update',
          name: 'updateInfo',
          component: UpdateInfo,
          meta: { requiresAuth: true }
        },
        {
          path: 'activity/:type',
          name: 'myActivity',
          component: MyActivityView,
          meta: { requiresAuth: true }
        }
      ]
    },
    /**
     * 개인 차량 추천 서비스 (AI)
     */
    {
      path: '/ai/survey/:agentType',
      name: 'AiSurveyByType',
      component: AISurveyView
    },
    {
      path: '/ai/loading/:agentType',
      name: 'AiLoading',
      component: AILoadingView
    },
    {
      path: '/ai/result/:agentType',
      name: 'AiResult',
      component: AIResultView
    }
    
  ],
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) {
    return true
  }

  const authStore = useAuthStore()

  if (authStore.accessToken) {
    return true
  }

  try {
    await authStore.refreshSession()
    return true
  } catch {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
})

export default router
