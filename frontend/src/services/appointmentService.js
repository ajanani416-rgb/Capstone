import { request } from "./api.js";

// Appointment + queue reads/writes. Booking (TASK-005), admin status
// transitions (TASK-006), and live queue (TASK-007) are wired.
export const appointmentService = {
  create: (input) => request("/api/appointments", { method: "POST", body: input }),
  listMine: () => request("/api/appointments/me"),
  get: (id) => request(`/api/appointments/${id}`),
  listAll: (params = {}) => {
    const q = new URLSearchParams(params).toString();
    return request(`/api/appointments${q ? `?${q}` : ""}`);
  },
  updateStatus: (id, status) =>
    request(`/api/appointments/${id}/status`, { method: "PATCH", body: { status } }),
  // Day queue = live /api/queue (backend order + computed waits).
  // Order is immutable; admins influence flow via status moves only (D5).
  todayQueue: (date) => {
    const day = date ?? new Date().toISOString().slice(0, 10);
    return request(`/api/queue?date=${day}`);
  },
};
