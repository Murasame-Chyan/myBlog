<template>
  <el-card class="weather-card">
    <div v-if="loading" class="weather-loading"><img :src="loadingIcon" width="40" alt="loading" /></div>
    <div v-else-if="error" class="weather-error">天气不可用</div>
    <div v-else class="weather-content">
      <img :src="iconSrc" :alt="weather.text" class="weather-icon" />
      <span class="weather-temp">{{ weather.temperature }}°C</span>
      <span class="weather-text">{{ weather.text }}</span>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const weather = ref({ temperature: '--', text: '加载中' })
const loading = ref(true), error = ref(false)
const loadingIcon = '/pics/weather/loading.svg'
const iconSrc = ref(loadingIcon)

const iconMap = { '晴': 'sunny', '多云': 'cloudy', '阴': 'cloudy', '雨': 'rainy', '雪': 'snowy', '雾': 'foggy' }

onMounted(async () => {
  try {
    const resp = await fetch('/api/weather/now?location=beijing')
    const data = await resp.json()
    if (data.code === 200 && data.data) {
      weather.value = data.data
      const w = data.data.text || ''
      let icon = 'unknown'
      for (const [k, v] of Object.entries(iconMap)) { if (w.includes(k)) { icon = v; break } }
      iconSrc.value = `/pics/weather/${icon}.svg`
    } else error.value = true
  } catch { error.value = true }
  loading.value = false
})
</script>

<style scoped>
.weather-card { text-align: center; } .weather-content { display: flex; align-items: center; gap: 8px; justify-content: center; }
.weather-icon { width: 40px; height: 40px; } .weather-temp { font-size: 1.5rem; font-weight: bold; } .weather-text { color: #666; }
</style>
