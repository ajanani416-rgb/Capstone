import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { clearSession, loadSession, saveSession } from "./tokenStore.js";

/* Session state (JWT, decision D1; OTP-verified, TASK-009). Route guards using
   this context are convenience — the backend filter chain is the real
   authorization boundary (docs/ARCHITECTURE.md §4). The token attaches via
   services/api.js. */

const AuthContext = createContext({ user: null, login: () => {}, logout: () => {} });

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadSession);

  const login = useCallback((session) => {
    setUser(session);
    saveSession(session);
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    clearSession();
  }, []);

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
