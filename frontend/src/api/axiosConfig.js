import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Attach the JWT to every outgoing request when the user is logged in.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ph_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Normalise backend errors into a single readable message.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      // Don't bounce the user out for a failed login attempt.
      if (!url.includes('/auth/login') && !url.includes('/auth/register')) {
        localStorage.removeItem('ph_token');
        localStorage.removeItem('ph_user');
      }
    }
    return Promise.reject(error);
  },
);

/** Pull a user-facing message out of any axios error. */
export function extractError(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data;
  if (!data) {
    if (error?.code === 'ECONNABORTED') return 'The server took too long to respond.';
    if (error?.message === 'Network Error') {
      return 'Cannot reach the server. Is the Spring Boot backend running on port 8080?';
    }
    return fallback;
  }
  if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors).join(' ');
  }
  return data.message || fallback;
}

export default api;
