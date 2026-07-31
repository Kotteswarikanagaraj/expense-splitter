import apiClient from './axiosClient';

// Each function here maps 1:1 to a backend endpoint. Components never call
// axios directly — they call these, which keeps the URL/shape of every
// endpoint defined in exactly one place.
export const register = (data) => apiClient.post('/auth/register', data);
export const login = (data) => apiClient.post('/auth/login', data);
