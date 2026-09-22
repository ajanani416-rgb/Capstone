import { Route, Routes } from "react-router-dom";
import { AdminLayout, CustomerLayout, PublicLayout } from "../layouts/layouts.jsx";
import { Forbidden, Landing, NotFound } from "../pages/public.jsx";
import { Login, Register } from "../pages/auth.jsx";
import { Services } from "../pages/Services.jsx";
import {
  AppointmentDetail, Book, CustomerDashboard, MyAppointments, MyQueue,
} from "../pages/customer.jsx";
import {
  AdminAppointments, AdminBarbers, AdminCustomers, AdminDashboard, AdminQueue, AdminServices,
} from "../pages/admin.jsx";
import { RequireAuth, RequireRole } from "./guards.jsx";

/* Route map mirrors docs/SCREEN_SPECIFICATIONS.md S-01…S-14.
   Backend contracts locked per docs/DATA_API.md. */

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route index element={<Landing />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="services" element={<Services />} />
        <Route path="403" element={<Forbidden />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<RequireRole role="CUSTOMER" />}>
          <Route element={<CustomerLayout />}>
            <Route path="customer" element={<CustomerDashboard />} />
            <Route path="customer/book" element={<Book />} />
            <Route path="customer/appointments" element={<MyAppointments />} />
            <Route path="customer/appointments/:id" element={<AppointmentDetail />} />
            <Route path="customer/queue" element={<MyQueue />} />
          </Route>
        </Route>
        <Route element={<RequireRole role="ADMIN" />}>
          <Route element={<AdminLayout />}>
            <Route path="admin" element={<AdminDashboard />} />
            <Route path="admin/appointments" element={<AdminAppointments />} />
            <Route path="admin/queue" element={<AdminQueue />} />
            <Route path="admin/services" element={<AdminServices />} />
            <Route path="admin/barbers" element={<AdminBarbers />} />
            <Route path="admin/customers" element={<AdminCustomers />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
