<template>
  <header class="topbar">
    <div class="title-group">
      <a :href="PORTAL_URL" class="portal-link" title="Retour au portail du foyer">← Retour au portail</a>
      <h1>📹 Caméra Foyer</h1>
    </div>
    <button class="secondary" :disabled="loading" @click="load">↻ Actualiser</button>
  </header>

  <main>
    <section v-if="playing" class="player">
      <div class="player-header">
        <h2 class="date-title">{{ formatDate(playing) }}</h2>
        <button class="icon secondary" title="Fermer le lecteur" @click="playing = null">✕</button>
      </div>
      <!-- :key recree la balise a chaque changement de date (sinon certains navigateurs gardent l'ancienne video) -->
      <video :key="playing" :src="api.videoUrl(playing)" controls autoplay preload="metadata"></video>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading && videos.length === 0" class="empty-hint">Chargement…</p>
    <p v-else-if="!error && videos.length === 0" class="empty-hint">
      Aucune vidéo pour le moment. Les vidéos de plus de 7 jours sont supprimées automatiquement.
    </p>

    <ul class="video-list">
      <li v-for="video in videos" :key="video.date" class="video-row" :class="{ active: video.date === playing }">
        <div class="video-info">
          <div class="video-date">{{ formatDate(video.date) }}</div>
          <div class="video-size">{{ formatSize(video.size) }}</div>
        </div>
        <div class="inline">
          <button @click="play(video.date)">▶ Lire</button>
          <button class="danger" :disabled="deleting === video.date" @click="remove(video.date)">🗑 Supprimer</button>
        </div>
      </li>
    </ul>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from './services/api'
import { PORTAL_URL } from './portal'

const videos = ref([])
const playing = ref(null)
const deleting = ref(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    videos.value = await api.getVideos()
  } catch {
    error.value = 'Impossible de charger la liste des vidéos.'
  } finally {
    loading.value = false
  }
}

function play(date) {
  playing.value = date
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function remove(date) {
  if (!window.confirm(`Supprimer définitivement la vidéo du ${formatDate(date)} ?`)) {
    return
  }
  deleting.value = date
  error.value = ''
  try {
    // On libere le fichier avant de le supprimer s'il est en cours de lecture
    if (playing.value === date) {
      playing.value = null
    }
    await api.deleteVideo(date)
    videos.value = videos.value.filter(v => v.date !== date)
  } catch (e) {
    if (e.response?.status === 404) {
      // Deja supprimee (ex: purge automatique des 7 jours) : on la retire quand meme
      videos.value = videos.value.filter(v => v.date !== date)
    } else {
      error.value = `La suppression de la vidéo du ${formatDate(date)} a échoué.`
    }
  } finally {
    deleting.value = null
  }
}

// "AAAA-MM-JJ" -> "jeudi 9 octobre 2026". Date construite en heure LOCALE (jamais
// new Date('AAAA-MM-JJ'), interprete en UTC et qui peut afficher la veille).
function formatDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('fr-FR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  })
}

const UNITS = ['octets', 'Ko', 'Mo', 'Go', 'To']

function formatSize(bytes) {
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < UNITS.length - 1) {
    value /= 1024
    unit++
  }
  const digits = unit === 0 || value >= 100 ? 0 : 1
  return `${value.toLocaleString('fr-FR', { maximumFractionDigits: digits })} ${UNITS[unit]}`
}

onMounted(load)
</script>
