# Caméra Foyer

Page web pour consulter les vidéos journalières de la caméra de surveillance
du foyer et les supprimer après visionnage, sans attendre la purge
automatique des 7 jours. Backend Java / Spring Boot, frontend Vue 3, **pas de
base de données** : le dossier des vidéos fait foi.

Sixième projet **indépendant** de `lol-results`, `foot-results`,
`tennis-results`, `budget-foyer` et `calendrier-foyer` : repo séparé, ports
différents (backend 8086 / 8386 en HTTPS, Vite 5179).

## 1. Principe

- Chaque nuit, un script côté serveur assemble les enregistrements de la
  caméra en **un fichier par jour**, `AAAA-MM-JJ.mp4`, dans un dossier
  (par défaut `/var/lib/motion/jours`). Une purge automatique supprime déjà
  les fichiers de plus de 7 jours.
- Le **backend** liste ce dossier et supprime un fichier à la demande. Il ne
  sert **pas** les vidéos elles-mêmes.
- **nginx** (le conteneur du frontend) sert directement les vidéos sous
  `/videos/AAAA-MM-JJ.mp4`, avec la prise en charge des requêtes Range :
  on peut avancer ou reculer dans la vidéo sans la télécharger en entier.

## 2. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

| Variable d'environnement | Rôle | Défaut |
|---|---|---|
| `VIDEOS_DIR` | Dossier contenant les fichiers `AAAA-MM-JJ.mp4` | `/var/lib/motion/jours` |
| `CORS_ALLOWED_ORIGINS` | Motifs d'origines autorisées, séparés par des virgules (ex: `http://*:8186,https://*:8286`) | `http://*:5179,https://*.ts.net:[*]` |

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/videos` | `[{ "date": "AAAA-MM-JJ", "size": <octets> }]`, de la plus récente à la plus ancienne |
| DELETE | `/api/videos/{date}` | Supprime `{date}.mp4` : 204 si supprimé, 404 si absent, 400 si `date` n'est pas une date `AAAA-MM-JJ` valide |

Les fichiers dont le nom n'est pas une date valide (`notes.mp4`,
`2026-02-30.mp4`, `.part`…) sont ignorés. Le paramètre `date` est validé
strictement (chiffres et tirets uniquement, date réelle) **avant** de
construire le chemin : aucun parcours de répertoire (`../`) n'est possible.

## 3. Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

Démarre sur `http://localhost:5179`. L'API est appelée sur le même hôte que
la page : port 8086 en HTTP, 8386 en HTTPS (Tailscale). Le lien « Retour au
portail » pointe vers le portail-foyer (8185 en HTTP, 8285 en HTTPS).

En dev avec Vite, la lecture des vidéos ne fonctionne pas (aucun nginx
devant `/videos/`) : la liste et la suppression, elles, fonctionnent.

## 4. Docker (frontend + nginx)

`frontend/Dockerfile` est multi-stage : build Vite avec Node, puis image
nginx qui sert l'appli (`frontend/nginx.conf`) et les vidéos. Il reste à
monter le dossier des vidéos sur `/usr/share/nginx/videos/` :

```bash
docker build -t camera-foyer-frontend frontend
docker run -d -p 8186:80 -v /var/lib/motion/jours:/usr/share/nginx/videos:ro camera-foyer-frontend
```

Le volume peut être en lecture seule côté nginx : c'est le backend qui
supprime les fichiers. Il doit donc avoir accès en écriture au même dossier.

## Structure du repo

```
camera-foyer/
├── backend/    Spring Boot (Java 21, Maven, sans base de données)
└── frontend/   Vue 3 + Vite, Dockerfile nginx (sert aussi /videos/)
```
