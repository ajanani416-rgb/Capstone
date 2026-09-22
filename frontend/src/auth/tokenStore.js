/* Bearer-token storage shared by AuthContext (writes) and api.js (reads).
   Splitting it out avoids a context↔service import cycle. Stored shape:
   { token, id, name, phone, role }. */

const KEY = "salon.auth.session";

export function loadSession() {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  try {
    localStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    // private mode etc. — session still lives in memory for this tab
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    // ignore
  }
}

export function getToken() {
  return loadSession()?.token ?? null;
}
