import axios from "axios";

// Instance configurée une seule fois, réutilisée partout
const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true, // Envoie les cookies à chaque requête
});

// Variable pour éviter de lancer plusieurs refresh en parallèle
let isRefreshing = false;
// File d'attente des requêtes en attente du refresh
let failedQueue: {
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}[] = [];

// Quand le refresh réussit, on relance toutes les requêtes en attente
const processQueue = (error: unknown | null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve();
    }
  });
  failedQueue = [];
};

// Intercepteur sur les RÉPONSES (pas les requêtes)
api.interceptors.response.use(
  // Cas succès : on ne touche à rien
  (response) => response,

  // Cas erreur : on intercepte les 401
  async (error) => {
    const originalRequest = error.config;

    // Si c'est un 401 ET que ce n'est pas déjà un retry
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Si c'est la route /auth/refresh qui échoue → on ne retry pas
      if (originalRequest.url?.includes("/api/auth/refresh")) {
        return Promise.reject(error);
      }

      // Si un refresh est déjà en cours, on met la requête en file d'attente
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Appel au refresh — le cookie refreshToken est envoyé auto
        await api.post("/api/auth/refresh");
        // Si ça marche, on relance les requêtes en attente
        processQueue(null);
        // Et on retry la requête originale
        return api(originalRequest);
      } catch (refreshError) {
        // Le refresh a échoué → session expirée
        processQueue(refreshError);
        // // Rediriger vers login
        // window.location.href = "/login";
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;