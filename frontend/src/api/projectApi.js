import api from './axiosConfig';

export const browseProjects = (params) => api.get('/projects', { params });

export const getWinningProjects = () => api.get('/projects/winners');

export const getProject = (id) => api.get(`/projects/${id}`);

export const getProjectFile = (projectId, fileId) =>
	api.get(`/projects/${projectId}/files/${fileId}`, { responseType: 'blob' });

export const createProject = (formData) => api.post('/projects', formData);

export const updateProject = (id, payload) => api.put(`/projects/${id}`, payload);

export const deleteProject = (id) => api.delete(`/projects/${id}`);

export const getMyProjects = () => api.get('/projects/my');

export const getBookmarkedProjects = () => api.get('/projects/bookmarked');

export const toggleLike = (id) => api.post(`/projects/${id}/like`);

export const toggleBookmark = (id) => api.post(`/projects/${id}/bookmark`);

export const getProjectComments = (id) => api.get(`/projects/${id}/comments`);

export const addProjectComment = (id, payload) => api.post(`/projects/${id}/comments`, payload);

export const getDepartmentStats = () => api.get('/departments/stats');

export const getDepartments = () => api.get('/meta/departments');

export const getCategories = () => api.get('/meta/categories');
