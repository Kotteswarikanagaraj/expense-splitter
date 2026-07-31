import { createContext, useContext, useState } from 'react';
import * as authApi from '../api/authApi';

// React Context is how we avoid "prop drilling" — passing `user` and `login`
// down through 5 layers of components that don't care about auth themselves,
// just to reach the one component at the bottom that does. Any component can
// call useAuth() and get the current user + auth actions directly.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Lazy initial state: read whatever was in localStorage on page load, so a
  // browser refresh doesn't log the user out. This is the ENTIRE "session
  // persistence" mechanism — there's no server-side session to restore from,
  // consistent with the backend being fully stateless (JWT-based).
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const persistSession = (authResponse) => {
    const { token, userId, name, email } = authResponse.data;
    const userObj = { userId, name, email };
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userObj));
    setUser(userObj);
  };

  const login = async (credentials) => {
    const response = await authApi.login(credentials);
    persistSession(response);
  };

  const register = async (details) => {
    const response = await authApi.register(details);
    persistSession(response);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook wrapping useContext — lets components do `const { user } = useAuth()`
// instead of `useContext(AuthContext)` everywhere, and is the natural place to
// throw a clear error if someone uses it outside the provider by mistake.
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
