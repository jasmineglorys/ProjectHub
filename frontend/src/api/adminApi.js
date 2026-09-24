import api from './axiosConfig';

export const getAdminProjects = (status) =>
  api.get('/admin/projects', { params: { status } });

export const updateProjectStatus = (id, status) =>
  api.patch(`/admin/projects/${id}/status`, { status });

export const adminDeleteProject = (id) => api.delete(`/admin/projects/${id}`);

export const getAdminStats = () => api.get('/admin/stats');
