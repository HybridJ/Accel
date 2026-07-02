import { API_BASE_URL } from '@/constants/api'

// article 이미지는 비공개 GCS 버킷(accel-file-container/article-images)에 저장되므로
// 직접 storage.googleapis.com URL로는 403이 난다. 항상 백엔드 프록시로 서빙해야 한다.
//   GET {API_BASE_URL}/boards/article-image?objectName=<objectName>
//
// 저장된 image_url은 여러 형태로 들어올 수 있다:
//   - http://accel.ai.kr/boards/article-image?objectName=article-images/xxx.jpeg  (백엔드가 /api 없이 절대 URL 생성 → 브라우저가 깨짐)
//   - /boards/article-image?objectName=...  또는  /api/boards/article-image?objectName=...
//   - article-images/xxx.jpeg              (object name)
//   - gs://accel-file-container/article-images/xxx.jpeg
//   - https://placehold.co/...             (시드/외부 이미지)
//   - blob:..., data:...                   (작성/수정 화면 로컬 미리보기)
// 이를 모두 로드 가능한 URL로 변환한다.

const ARTICLE_IMAGE_FOLDER = 'article-images'

const toProxyUrl = (objectName) => {
    return `${API_BASE_URL}/boards/article-image?objectName=${encodeURIComponent(objectName)}`
}

const extractObjectName = (url) => {
    const match = url.match(/[?&]objectName=([^&]+)/)

    if (!match) {
        return ''
    }

    try {
        return decodeURIComponent(match[1])
    } catch {
        return match[1]
    }
}

export const resolveArticleImageUrl = (rawUrl) => {
    const url = String(rawUrl ?? '').trim()

    if (!url) {
        return ''
    }

    // 1) objectName 쿼리를 가진 프록시 URL의 모든 형태(절대/상대, /api 유무, http/https) → objectName만 뽑아 프록시 URL 재구성
    const objectName = extractObjectName(url)
    if (objectName) {
        return toProxyUrl(objectName)
    }

    // 2) gs:// 또는 버킷 직접 URL, 혹은 object name이 그대로 저장된 경우 → 프록시 URL로 변환
    const gsObject = url
        .replace(/^gs:\/\/accel-file-container\//i, '')
        .replace(/^https?:\/\/storage\.googleapis\.com\/accel-file-container\//i, '')
        .replace(/^\/+/, '')

    if (gsObject !== url || gsObject.startsWith(`${ARTICLE_IMAGE_FOLDER}/`)) {
        return toProxyUrl(gsObject)
    }

    // 3) 외부 절대 URL(placehold.co 등) / blob: / data: → 그대로 사용
    return url
}

// article 객체(imageUrls 배열 또는 '||'로 이어진 imageUrl 문자열)를 표시 가능한 URL 배열로 변환한다.
export const resolveArticleImageUrls = (article) => {
    const rawUrls = Array.isArray(article?.imageUrls) && article.imageUrls.length > 0
        ? article.imageUrls
        : String(article?.imageUrl ?? '').split('||')

    return rawUrls
        .map((url) => resolveArticleImageUrl(url))
        .filter(Boolean)
}
