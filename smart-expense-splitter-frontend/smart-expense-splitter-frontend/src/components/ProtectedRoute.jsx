import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Wraps any page that requires login. If there's no user in AuthContext,
// redirect to /login instead of rendering the children. This is the standard
// React Router pattern for route guarding — no separate routing library needed.
export default function ProtectedRoute({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}
