import axios from "axios";
import { defineStore } from "pinia";
import { ref } from "vue";
import { API_BASE_URL } from "@/constants/api";
import { http } from "@/lib/http";

export const useBoardStore = defineStore('board', ()=> {
    const REST_URL = API_BASE_URL

    const brandId = ref(0)
    const articles = ref([])
    const article = ref({})
    const comments = ref([])

    // 공개 조회(토큰 불필요)는 일반 axios 사용.
    // 만료 토큰이 실리면 permitAll도 401이 되므로 토큰을 싣지 않는다.
    const fetchBrands = async () => {
        const response = await axios.get(`${REST_URL}/boards/brands`)

        return response.data
    }

    const fetchArticles = async (targetBrandId = brandId.value) => {
        brandId.value = targetBrandId

        const response = await axios.get(`${REST_URL}/boards/brands/${targetBrandId}`)

        articles.value = response.data

        return articles.value
    }

    // 게시글 본문은 공개 GET, 댓글은 로그인 시 본인 좋아요 여부(liked)를 받기 위해 http(토큰 부착)로 호출
    const fetchArticle = async (articleId) => {
        const [articleResponse, commentsResponse] = await Promise.all([
            axios.get(`${REST_URL}/boards/articles/${articleId}`),
            http.get(`${REST_URL}/comments/${articleId}`),
        ])

        article.value = articleResponse.data
        comments.value = commentsResponse.data

        return {
            article: article.value,
            comments: comments.value,
        }
    }

    const increaseArticleViewCount = async (articleId) => {
        const response = await axios.post(`${REST_URL}/boards/articles/${articleId}/views`)

        article.value = response.data

        return article.value
    }

    // 이하 인증(또는 인증 인지) 요청: http가 토큰 부착 + 401 시 1회 자동 갱신/재시도 처리.
    // 사용자 식별은 JWT subject로 처리되므로 userId 인자는 더 이상 사용하지 않는다(호출부 호환을 위해 시그니처는 유지).
    const createComment = async (articleId, commentForm) => {
        await http.post(`${REST_URL}/comments/${articleId}`, commentForm)
        const commentsResponse = await http.get(`${REST_URL}/comments/${articleId}`)

        comments.value = commentsResponse.data

        return comments.value
    }

    const updateComment = async (articleId, commentId, commentForm) => {
        await http.put(`${REST_URL}/comments/${articleId}/${commentId}`, commentForm)
        const commentsResponse = await http.get(`${REST_URL}/comments/${articleId}`)

        comments.value = commentsResponse.data

        return comments.value
    }

    const deleteComment = async (articleId, commentId) => {
        await http.delete(`${REST_URL}/comments/${articleId}/${commentId}`)
        comments.value = comments.value.filter((comment) => comment.commentId !== commentId)

        return comments.value
    }

    const createArticle = async (articleForm) => {
        const { imageFiles = [], ...articleFields } = articleForm
        let response

        try {
            if (imageFiles.length > 0) {
                const formData = new FormData()

                Object.entries(articleFields).forEach(([key, value]) => {
                    formData.append(key, value ?? '')
                })
                imageFiles.forEach((imageFile) => {
                    formData.append('images', imageFile)
                })

                response = await http.post(`${REST_URL}/boards/article/form`, formData)
            } else {
                response = await http.post(`${REST_URL}/boards/article`, articleFields)
            }
        } catch (error) {
            console.error('[boardStore.createArticle] request failed', {
                status: error.response?.status,
                data: error.response?.data,
                message: error.message,
            })
            throw error
        }

        if (articleForm.brandId) {
            await fetchArticles(articleForm.brandId)
        }

        return response.data
    }

    const updateArticle = async (articleId, articleForm) => {
        const { imageFiles = [], imageOrder, ...articleFields } = articleForm
        const shouldSyncImages = Array.isArray(imageOrder)
        let response

        if (imageFiles.length > 0 || shouldSyncImages) {
            const formData = new FormData()

            Object.entries(articleFields).forEach(([key, value]) => {
                formData.append(key, value ?? '')
            })
            if (shouldSyncImages) {
                formData.append('imageOrderManaged', 'true')
                imageOrder.forEach((imageOrderItem) => {
                    formData.append('imageOrder', imageOrderItem)
                })
            }
            imageFiles.forEach((imageFile) => {
                formData.append('images', imageFile)
            })

            response = await http.put(`${REST_URL}/boards/articles/${articleId}/form`, formData)
        } else {
            response = await http.put(`${REST_URL}/boards/articles/${articleId}`, articleFields)
        }

        article.value = {
            ...article.value,
            ...response.data,
        }

        return article.value
    }

    const deleteArticle = async (articleId) => {
        await http.delete(`${REST_URL}/boards/articles/${articleId}`)

        article.value = {}
        comments.value = []
    }

    const fetchArticleLikeStatus = async (articleId) => {
        const response = await http.get(`${REST_URL}/boards/articles/${articleId}/likes`)

        return response.data
    }

    const likeArticle = async (articleId) => {
        const response = await http.post(`${REST_URL}/boards/articles/${articleId}/likes`, null)

        return response.data
    }

    const unlikeArticle = async (articleId) => {
        const response = await http.delete(`${REST_URL}/boards/articles/${articleId}/likes`)

        return response.data
    }

    const likeComment = async (commentId) => {
        const response = await http.post(`${REST_URL}/comments/${commentId}/likes`, null)

        return response.data
    }

    const unlikeComment = async (commentId) => {
        const response = await http.delete(`${REST_URL}/comments/${commentId}/likes`)

        return response.data
    }

    const fetchMyArticles = async () => {
        const response = await http.get(`${REST_URL}/user/me/myboards`)

        return response.data
    }

    const fetchMyLikedArticles = async () => {
        const response = await http.get(`${REST_URL}/user/me/mylikes`)

        return response.data
    }

    const fetchMyCommentedArticles = async () => {
        const response = await http.get(`${REST_URL}/user/me/mycomments`)

        return response.data
    }

    return{
        brandId,
        articles,
        article,
        comments,
        fetchBrands,
        fetchArticles,
        fetchArticle,
        increaseArticleViewCount,
        createArticle,
        createComment,
        updateComment,
        deleteComment,
        updateArticle,
        deleteArticle,
        fetchArticleLikeStatus,
        likeArticle,
        unlikeArticle,
        likeComment,
        unlikeComment,
        fetchMyArticles,
        fetchMyLikedArticles,
        fetchMyCommentedArticles,
    }
})
