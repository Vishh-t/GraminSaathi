import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  signup: (data) => api.post('/auth/signup', data),
  login: (data) => api.post('/auth/login', data),
};

export const referenceAPI = {
  getVillages: () => api.get('/villages'),
  getBusinessCategories: () => api.get('/business-categories'),
  getSchemes: () => api.get('/schemes'),
};

export const analysisAPI = {
  analyze: (data) => api.post('/analyze', data),
  discover: (data) => api.post('/discover', data),
  simulate: (data) => api.post('/simulate', data),
  goalSeek: (data) => api.post('/goal-seek', data),
  downloadPdf: (params) => api.get('/analyze/pdf', { params, responseType: 'blob' }),
};

export const reportsAPI = {
  save: (data) => api.post('/reports', data),
  getAll: () => api.get('/reports'),
};

export default api;