import axios from 'axios'

// Ports du backend : HTTP en LAN (http://<ip>), HTTPS via Tailscale
// (https://serveur-foyer.tail0af124.ts.net).
const PORT_BACKEND_HTTP = 8086
const PORT_BACKEND_HTTPS = 8386

// L'API suit le protocole de la page : une page HTTPS qui appelle une API HTTP est
// bloquee par le navigateur ("contenu mixte").
const BASE_URL = window.location.protocol === 'https:'
  ? `https://${window.location.hostname}:${PORT_BACKEND_HTTPS}/api`
  : `http://${window.location.hostname}:${PORT_BACKEND_HTTP}/api`
const client = axios.create({ baseURL: BASE_URL })

export default {
  // [{ date: 'AAAA-MM-JJ', size: <octets> }], de la plus recente a la plus ancienne
  getVideos() {
    return client.get('/videos').then(r => r.data)
  },
  deleteVideo(date) {
    return client.delete(`/videos/${date}`)
  },
  // Fichier servi directement par nginx (meme origine que la page), pas par le backend :
  // nginx gere les requetes Range, indispensables pour avancer/reculer dans la video.
  videoUrl(date) {
    return `/videos/${date}.mp4`
  }
}
