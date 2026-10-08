/* Single HTTP layer. All components/services go through request().
   Base URL comes from VITE_API_URL (see .env.example). Throws ApiError
   carrying { status, message, fieldErrors } for UI_UX.md §3 state handling. */

import { getToken } from "../auth/tokenStore.js";

const BASE_URL = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors ?? {};
  }
}

function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function request(path, { method = "GET", body } = {}) {
  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Network failure. Check your connection and retry.", {});
  }
  const isJson = (res.headers.get("content-type") ?? "").includes("application/json");
  const payload = isJson ? await res.json().catch(() => ({})) : {};
  if (!res.ok) {
    throw new ApiError(
      res.status,
      payload.message ?? friendlyFallback(res.status),
      payload.fieldErrors ?? {},
    );
  }
  return payload;
}

function friendlyFallback(status) {
  switch (status) {
    case 400: return "Invalid request. Review the highlighted fields.";
    case 401: return "Session expired. Please log in again.";
    case 403: return "You are not allowed to do that.";
    case 404: return "Not found.";
    case 409: return "Conflict. The slot or record changed — please retry.";
    case 410: return "That code expired. Request a new one.";
    default: return "Something went wrong. Please retry.";
  }
}
