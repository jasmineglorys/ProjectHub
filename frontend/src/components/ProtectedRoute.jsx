import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Loader from './Loader';

/**
 * Guards a route. `adminOnly` additionally requires the ADMIN role.
 */
export default function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, isAdmin, initialising } = useAuth();
  const location = useLocation();

  if (initialising) {
    return <Loader label="Checking your session…" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (adminOnly && !isAdmin) {
    return (
      <div className="page-narrow empty-state">
        <div className="empty-icon">🔒</div>
        <h2>Admin access required</h2>
        <p>This dashboard is only available to moderators.</p>
      </div>
    );
  }

  return children;
}
