import axios from 'axios';

// One shared axios instance for the whole app, instead of importing raw axios
// everywhere. This is what lets us configure the base URL and the auth header
// logic in exactly one place.
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Request interceptor: runs before EVERY request made with this instance.
// We read the JWT out of localStorage and attach it as a Bearer header —
// this is what makes every API call in the app "authenticated" without each
// component having to remember to add the header itself.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: if the backend ever says "your token is invalid/expired"
// (401), the cleanest thing to do is boot the user back to login rather than
// let them sit on a broken session. We don't do this for 403 (Forbidden) —
// that means "you're logged in fine, you're just not allowed to do THIS
// specific thing" (e.g. not a group member), which is a different situation.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
