import apiClient from './axiosClient';

export const getBalances = (groupId) => apiClient.get(`/groups/${groupId}/balances`);
export const getSettlements = (groupId) => apiClient.get(`/groups/${groupId}/settlements`);
export const settleUp = (groupId, settlementId) =>
  apiClient.post(`/groups/${groupId}/settlements/${settlementId}/settle`);
