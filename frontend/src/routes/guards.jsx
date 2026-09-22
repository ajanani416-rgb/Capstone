import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

/* UX-only guards. The backend is the security boundary
   (docs/ARCHITECTURE.md §4); these just keep navigation sensible. */

export function RequireAuth() {
  const { user } = useAuth();
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

export function RequireRole({ role }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return user.role === role ? <Outlet /> : <Navigate to="/403" replace />;
}
