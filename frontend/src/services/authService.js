import { request } from "./api.js";

// Email-OTP auth (TASK-009). Neither register nor login returns a session:
// both return a challenge { email, purpose, expiresInSeconds }. The JWT
// arrives only from verifyOtp. Login with an unverified email answers 403
// (and issues a REGISTER code) — callers branch on status alone.
export const authService = {
  register: (input) => request("/api/auth/register", { method: "POST", body: input }),
  login: (input) => request("/api/auth/login", { method: "POST", body: input }),
  verifyOtp: (input) => request("/api/auth/verify-otp", { method: "POST", body: input }),
  resendOtp: (input) => request("/api/auth/otp/resend", { method: "POST", body: input }),
  me: () => request("/api/auth/me"),
};
