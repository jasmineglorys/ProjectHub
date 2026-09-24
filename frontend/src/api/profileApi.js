import api from './axiosConfig';

export const getProfileActivity = () => api.get('/profile/activity');
