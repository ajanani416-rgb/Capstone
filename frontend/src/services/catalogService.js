import { request } from "./api.js";

// Catalog reads are public (active rows for customers, all rows for admins —
// decided backend-side). Writes are ADMIN-only. Wired TASK-006.
export const catalogService = {
  listServices: () => request("/api/services"),
  getService: (id) => request(`/api/services/${id}`),
  createService: (input) => request("/api/services", { method: "POST", body: input }),
  updateService: (id, input) => request(`/api/services/${id}`, { method: "PUT", body: input }),
  deleteService: (id) => request(`/api/services/${id}`, { method: "DELETE" }),
  listBarbers: () => request("/api/barbers"),
  getBarber: (id) => request(`/api/barbers/${id}`),
  createBarber: (input) => request("/api/barbers", { method: "POST", body: input }),
  updateBarber: (id, input) => request(`/api/barbers/${id}`, { method: "PUT", body: input }),
  deleteBarber: (id) => request(`/api/barbers/${id}`, { method: "DELETE" }),
  listUsers: () => request("/api/users"),
};
