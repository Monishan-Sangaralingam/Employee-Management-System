import axios from 'axios';

import { getToken } from './AuthService';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function getDashboard() {
  const response = await api.get('/api/dashboard');
  return response.data;
}
