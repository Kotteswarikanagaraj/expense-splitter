import apiClient from './axiosClient';

export const addExpense = (data) => apiClient.post('/expenses', data);

// filters is a plain object like { page: 0, size: 10, paidBy: 2 } —
// axios turns it into ?page=0&size=10&paidBy=2 and drops undefined/null keys.
export const getExpenses = (groupId, filters = {}) =>
  apiClient.get(`/expenses/group/${groupId}`, { params: filters });
