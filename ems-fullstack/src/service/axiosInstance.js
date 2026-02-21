import axios from 'axios';

import { getToken } from './AuthService';

// In production (Docker), nginx proxies /api → backend:8090
// In development, Vite proxy handles it (see vite.config.js)
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const api = axios.create({
    baseURL,
});

api.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
