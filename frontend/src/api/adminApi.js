import api from './axiosConfig';

export const getAdminProjects = (status) =>
  api.get('/admin/projects', { params: { status } });

export const getSimilarProjects = (id) => api.get(`/admin/projects/${id}/similar`);

export const updateProjectStatus = (id, status) =>
  api.patch(`/admin/projects/${id}/status`, { status });

export const rejectProject = (id) => api.patch(`/admin/projects/${id}/reject`);

export const requestProjectChanges = (id, reason) =>
  api.patch(`/admin/projects/${id}/request-changes`, { reason });

export const adminDeleteProject = (id) => api.delete(`/admin/projects/${id}`);

export const getAdminStats = () => api.get('/admin/stats');
