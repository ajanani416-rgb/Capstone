import { request } from "./api.js";

// Email + password auth with email-OTP verification. Register returns a
// challenge { email, purpose, expiresInSeconds } (no session); login with a
// verified account returns the JWT directly, and with an unverified one
// answers 403 (issuing a fresh REGISTER code) — callers branch on status.
// The JWT arrives from login or verifyOtp.
export const authService = {
  register: (input) => request("/api/auth/register", { method: "POST", body: input }),
  login: (input) => request("/api/auth/login", { method: "POST", body: input }),
  verifyOtp: (input) => request("/api/auth/verify-otp", { method: "POST", body: input }),
  resendOtp: (input) => request("/api/auth/otp/resend", { method: "POST", body: input }),
  me: () => request("/api/auth/me"),
};
