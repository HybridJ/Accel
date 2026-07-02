<template>
    <footer class="site-footer">
        <div class="footer-main">
            <!-- 브랜드 -->
            <div class="footer-brand">
                <img src="../../assets/ACCEL_Logo_white.png" alt="ACCEL" class="footer-logo">
                <p class="brand-slogan">당신의 취향을 읽고,<br>어울리는 차를 큐레이션합니다.</p>
                <p class="brand-desc">
                    차종 추천부터 전기차 인프라, 타랑께까지<br>
                    자동차의 모든 여정을 한 곳에서.
                </p>
            </div>

            <!-- 서비스 -->
            <nav class="footer-col" aria-label="서비스 바로가기">
                <h3 class="footer-heading">서비스</h3>
                <RouterLink :to="{ name: 'AiSurveyByType', params: { agentType: 'carbti' } }">CARBTI</RouterLink>
                <RouterLink :to="{ name: 'AiSurveyByType', params: { agentType: 'carsulting' } }">CARSULTING
                </RouterLink>
                <RouterLink :to="{ name: 'EVInfra' }">EV Info</RouterLink>
                <RouterLink :to="{ name: 'videoCategory' }">자동차 극장</RouterLink>
                <RouterLink :to="{ name: 'BrandCategory' }">타랑께</RouterLink>
            </nav>

            <!-- 프로젝트 / 팀 -->
            <div class="footer-col">
                <h3 class="footer-heading">프로젝트</h3>
                <span class="footer-meta">SSAFY 15기 대전 5반 1팀</span>
                <span class="footer-meta">개발 기간 2026.05 ~ 2026.06</span>
                <div class="footer-team">
                    <a v-for="member in team" :key="member.name" :href="member.github" target="_blank"
                        rel="noopener noreferrer">{{ member.name }}</a>
                </div>
                <a class="footer-repo" href="https://lab.ssafy.com/s2ndn1s/accel.git" target="_blank"
                    rel="noopener noreferrer">
                    GitLab 저장소
                </a>
            </div>

            <!-- 기술 & AI -->
            <div class="footer-col">
                <h3 class="footer-heading">Powered by</h3>
                <span class="footer-meta">Vue 3 · Spring Boot · MySQL</span>
                <span class="footer-meta">Google Gemini · OpenAI · Anthropic Claude</span>
            </div>
        </div>

        <div class="footer-bottom">
            <span>© 2026 ACCEL · SSAFY 비영리 교육용 프로젝트</span>
            <span class="footer-disclaimer">
                자동차 극장과 AI 추천 결과 일부는 생성형 AI로 제작되었으며 실제 차량 사양과 다를 수 있습니다.
            </span>
        </div>
    </footer>
</template>

<script setup>
// GitHub 핸들로 교체해 주세요 (현재는 GitHub 메인으로 연결된 placeholder)
const team = [
    { name: '김준호', github: 'https://github.com/' },
    { name: '나주원', github: 'https://github.com/' },
]
</script>

<style scoped>
.site-footer {
    width: 100%;
    padding: 40px 60px 28px;
    color: var(--muted-blue);
    background: rgba(0, 0, 0, 0.92);
    border-top: 1px solid var(--line-blue);
    box-shadow: 0 -12px 34px rgba(4, 217, 255, 0.12);
    box-sizing: border-box;
}

.footer-main {
    display: grid;
    grid-template-columns: 1.6fr 1fr 1.1fr 1.1fr;
    gap: 32px 40px;
    align-items: start;
    max-width: 1280px;
    margin: 0 auto;
}

.footer-brand {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.footer-logo {
    width: 260px;
    height: auto;
    display: block;
    filter: drop-shadow(0 0 10px rgba(4, 217, 255, 0.45));
}

.brand-slogan {
    margin: 0;
    max-width: 320px;
    font-size: 18px;
    font-weight: 600;
    line-height: 1.55;
    color: var(--neon-text);
}

.brand-desc {
    margin: 0;
    max-width: 320px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--muted-blue);
}

.footer-col {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.footer-heading {
    margin: 0 0 6px;
    color: var(--neon-cyan);
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
}

.footer-col a {
    width: fit-content;
    color: var(--muted-blue);
    text-decoration: none;
    font-size: 14px;
    transition: color 0.16s ease;
}

.footer-col a:hover {
    color: var(--neon-cyan);
}

.footer-meta {
    font-size: 14px;
    line-height: 1.5;
}

.footer-team {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    margin-top: 4px;
}

.footer-repo {
    margin-top: 6px;
    font-weight: 600;
    color: var(--neon-cyan) !important;
}

.footer-bottom {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 1280px;
    margin: 28px auto 0;
    padding-top: 20px;
    border-top: 1px solid rgba(140, 207, 255, 0.16);
    font-size: 13px;
    line-height: 1.5;
}

.footer-disclaimer {
    color: rgba(140, 207, 255, 0.7);
}

@media (max-width: 768px) {
    .site-footer {
        padding: 32px 24px 24px;
    }

    .footer-main {
        grid-template-columns: 1fr 1fr;
        gap: 28px 24px;
    }

    .footer-brand {
        grid-column: 1 / -1;
    }

    .footer-logo {
        width: 200px;
    }
}

@media (max-width: 480px) {
    .footer-main {
        grid-template-columns: 1fr;
    }
}
</style>
