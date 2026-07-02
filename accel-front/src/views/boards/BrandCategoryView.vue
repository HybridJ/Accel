<template>
    <main class="brand-category-page">
        <header class="category-hero">
            <p class="category-kicker">타랑께</p>
            <h1>타랑께</h1>
        </header>

        <section
            class="brand-section"
            v-for="section in brandSections"
            :key="section.key"
            :class="section.key"
        >
            <div class="brand-section-header">
                <div class="section-title-wrap">
                    <span class="section-marker" aria-hidden="true"></span>
                    <h2>{{ section.title }}</h2>
                </div>
                <span class="brand-count">{{ section.brands.length }}개 브랜드</span>
            </div>

            <div class="brand-grid">
                <RouterLink
                    class="brand-item"
                    v-for="brand in section.brands"
                    :key="brand.brandId"
                    :to="brandRoute(brand.brandId)"
                >
                    <div class="brand-logo-box">
                        <img class="brand-logo" :src="getBrandLogo(brand.brandSlug)" :alt="`${brand.boardName} 로고`">
                    </div>
                    <div class="brand-link">
                        {{ brand.boardName }}
                    </div>
                </RouterLink>
            </div>
        </section>
    </main>
</template>

<script setup>
import fallbackLogo from '@/assets/ACCEL_Logo.png';
import { useBoardStore } from '@/stores/boardStore';
import { computed, onMounted, ref } from 'vue';

const store = useBoardStore()
const brands = ref([])
const logoSlugAliases = {
    'rolls-royce': 'rollsroyce',
}
const brandLogos = import.meta.glob('../../assets/logos/*.png', {
    eager: true,
    import: 'default',
})

const isDomesticBrand = (brand) => {
    return [brand.isDomestic, brand.is_domestic, brand.domestic].some((value) =>
        value === true || value === 1 || value === 'true'
    )
}

const domesticBrands = computed(() => brands.value.filter(isDomesticBrand))
const importedBrands = computed(() => brands.value.filter((brand) => !isDomesticBrand(brand)))
const brandSections = computed(() => [
    { key: 'domestic', title: '국산차', brands: domesticBrands.value },
    { key: 'imported', title: '수입차', brands: importedBrands.value },
])

const getBrandLogo = (brandSlug) => {
    const logoSlug = logoSlugAliases[brandSlug] ?? brandSlug

    return brandLogos[`../../assets/logos/${logoSlug}.png`] ?? fallbackLogo
}

const brandRoute = (brandId) => {
    return {
        name: 'BrandBoard',
        params: { brandId: `${brandId}` },
    }
}

onMounted(async () => {
    brands.value = await store.fetchBrands()
})
</script>

<style scoped>
.brand-category-page {
    width: min(100%, 1180px);
    margin: 0 auto;
    padding: 116px 24px 64px;
    box-sizing: border-box;
}

.category-hero {
    margin-bottom: 42px;
    padding-bottom: 22px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.18);
}

.category-kicker {
    margin: 0 0 8px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.category-hero h1 {
    margin: 0;
    color: #ffffff;
    font-size: 40px;
    line-height: 1.18;
    font-weight: 800;
}

.brand-section + .brand-section {
    margin-top: 60px;
}

.brand-section {
    --section-accent: var(--neon-cyan);
}

.brand-section.domestic {
    --section-accent: #5cffb0;
}

.brand-section.imported {
    --section-accent: #ffd166;
}

.brand-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    margin-bottom: 22px;
    padding-bottom: 14px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.16);
}

.section-title-wrap {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
}

.section-marker {
    width: 10px;
    height: 10px;
    flex: 0 0 10px;
    border-radius: 999px;
    background: var(--section-accent);
    box-shadow: 0 0 18px color-mix(in srgb, var(--section-accent) 62%, transparent);
}

.brand-section-header h2 {
    margin: 0;
    color: #ffffff;
    font-size: 28px;
    line-height: 1.2;
    font-weight: 800;
}

.brand-count {
    flex: 0 0 auto;
    padding: 7px 12px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
}

.brand-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
    gap: 20px;
}

.brand-item {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 150px;
    padding: 12px 12px 14px;
    overflow: hidden;
    color: var(--neon-text);
    text-decoration: none;
    background:
        linear-gradient(180deg, rgba(8, 22, 42, 0.94), rgba(2, 7, 18, 0.94));
    border: 1px solid rgba(140, 207, 255, 0.18);
    border-radius: 8px;
    cursor: pointer;
    transition:
        transform 0.18s ease,
        border-color 0.18s ease,
        box-shadow 0.18s ease,
        background 0.18s ease;
}

.brand-item::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    height: 2px;
    background: linear-gradient(90deg, var(--section-accent), transparent);
    opacity: 0.76;
}

.brand-item:hover,
.brand-item:focus-visible {
    border-color: color-mix(in srgb, var(--section-accent) 72%, rgba(255, 255, 255, 0.2));
    background:
        linear-gradient(180deg, rgba(10, 30, 56, 0.98), rgba(2, 9, 22, 0.98));
    box-shadow: 0 14px 38px rgba(0, 0, 0, 0.34), 0 0 22px rgba(4, 217, 255, 0.16);
    outline: none;
    transform: translateY(-3px);
}

.brand-logo-box {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 90px;
    padding: 17px;
    background: linear-gradient(180deg, #ffffff, #f8fafc);
    border: 1px solid rgba(255, 255, 255, 0.86);
    border-radius: 6px;
    box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.05);
    transition: border-color 0.18s ease, transform 0.18s ease;
}

.brand-item:hover .brand-logo-box,
.brand-item:focus-visible .brand-logo-box {
    border-color: color-mix(in srgb, var(--section-accent) 44%, #ffffff);
    transform: translateY(-1px);
}

.brand-logo {
    width: 100%;
    height: 100%;
    object-fit: contain;
    display: block;
}

.brand-link {
    min-height: 22px;
    color: #ffffff;
    text-align: center;
    text-decoration: none;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.35;
    overflow-wrap: anywhere;
}

@media (max-width: 640px) {
    .brand-category-page {
        padding: 96px 14px 44px;
    }

    .category-hero {
        margin-bottom: 34px;
    }

    .category-hero h1 {
        font-size: 30px;
    }

    .brand-section + .brand-section {
        margin-top: 46px;
    }

    .brand-section-header {
        align-items: flex-start;
        gap: 12px;
    }

    .brand-section-header h2 {
        font-size: 24px;
    }

    .brand-count {
        padding: 6px 10px;
        font-size: 12px;
    }

    .brand-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 14px;
    }

    .brand-item {
        min-height: 136px;
        padding: 10px;
    }

    .brand-logo-box {
        height: 76px;
        padding: 14px;
    }

    .brand-link {
        font-size: 14px;
    }
}
</style>
