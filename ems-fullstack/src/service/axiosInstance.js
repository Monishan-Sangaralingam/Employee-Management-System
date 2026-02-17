import axios from 'axios';

import { getToken } from './AuthService';

export const api = axios.create({
    baseURL: 'http://localhost:8090',
});

api.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
