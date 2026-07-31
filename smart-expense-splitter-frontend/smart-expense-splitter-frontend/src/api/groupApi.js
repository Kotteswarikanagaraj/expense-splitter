import apiClient from './axiosClient';

export const createGroup = (data) => apiClient.post('/groups', data);
export const addMember = (groupId, userId) =>
  apiClient.post(`/groups/${groupId}/members`, { userId });
export const getMyGroups = () => apiClient.get('/groups/my');
export const getGroup = (groupId) => apiClient.get(`/groups/${groupId}`);
