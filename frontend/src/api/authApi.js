import api from './axiosConfig';

export const registerUser = (payload) => api.post('/auth/register', payload);

export const loginUser = (payload) => api.post('/auth/login', payload);

export const fetchCurrentUser = () => api.get('/auth/me');

export const requestPasswordReset = (payload) => api.post('/auth/forgot-password', payload);

export const resendPasswordResetOtp = (payload) => api.post('/auth/resend-otp', payload);

export const verifyPasswordResetOtp = (payload) =>
	api.post('/auth/verify-otp', payload, { withCredentials: true });

export const resetPassword = (payload) =>
	api.post('/auth/reset-password', payload, { withCredentials: true });
