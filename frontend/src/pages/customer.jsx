import { useCallback, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  Button, Card, EmptyState, ErrorMessage, Input, Loading, PageHeader, Select, StatusBadge,
} from "../components/ui.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { appointmentService } from "../services/appointmentService.js";
import { catalogService } from "../services/catalogService.js";
import { ApiError } from "../services/api.js";

export function CustomerDashboard() {
  const loader = useCallback(() => appointmentService.listMine(), []);
  const { phase, data, error, retry } = useAsync(loader);

  return (
    <>
      <PageHeader title="My dashboard" subtitle="Your next visit at a glance."
        actions={<Link to="/customer/book"><Button>Book appointment</Button></Link>} />
      {phase === "loading" && <Loading label="Loading your appointments…" />}
      {phase === "empty" && (
        <EmptyState title="No appointments yet"
          body="Book your first appointment to see it here."
          action={<Link to="/customer/book"><Button>Book now</Button></Link>} />
      )}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <Card>
          <p><strong>{data[0]?.serviceName ?? data[0]?.service?.name ?? "Upcoming appointment"}</strong></p>
          <StatusBadge status={data[0]?.status ?? "QUEUED"} />
          <p>Queue number: <strong>{data[0]?.queueNumber ?? "—"}</strong></p>
          <Link to="/customer/appointments">View all</Link>
        </Card>
      )}
    </>
  );
}

export function Book() {
  const navigate = useNavigate();
  const servicesAsync = useAsync(useCallback(() => catalogService.listServices(), []));
  const barbersAsync = useAsync(useCallback(() => catalogService.listBarbers(), []));
  const [form, setForm] = useState({ serviceId: "", barberId: "", date: "", time: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const loading = servicesAsync.phase === "loading" || barbersAsync.phase === "loading";
  const failed = servicesAsync.phase === "error" ? servicesAsync : barbersAsync.phase === "error" ? barbersAsync : null;
  // Native pickers: no slot grid is invented — the backend owns availability
  // and answers 409 when the exact slot is taken (selections are preserved).
  const today = new Date().toISOString().slice(0, 10);

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!form.serviceId) next.serviceId = "Choose a service.";
    if (!form.barberId) next.barberId = "Choose a barber.";
    if (!form.date) next.date = "Pick a date.";
    else if (form.date < today) next.date = "Date must be today or later.";
    if (!form.time) next.time = "Pick a time.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const created = await appointmentService.create({
        serviceId: Number(form.serviceId), barberId: Number(form.barberId),
        appointmentDate: form.date, appointmentTime: form.time,
      });
      navigate(`/customer/appointments/${created.id ?? ""}`, { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Slot taken: keep every selection, say exactly what to do next.
        setErrors({ form: err.message });
      } else if (err instanceof ApiError && err.status === 400 && err.fieldErrors) {
        setErrors({ ...err.fieldErrors, form: err.message });
      } else {
        setErrors({ form: err instanceof ApiError ? err.message : "Booking failed. Please retry." });
      }
    } finally {
      setPending(false);
    }
  }

  if (loading) return (<><PageHeader title="Book appointment" /><Loading label="Loading catalog…" /></>);
  if (failed) return (<><PageHeader title="Book appointment" /><ErrorMessage message={failed.error.message} onRetry={() => { servicesAsync.retry(); barbersAsync.retry(); }} /></>);

  return (
    <>
      <PageHeader title="Book appointment" subtitle="Pick a service, barber, date and time." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Select id="service" label="Service" required value={form.serviceId} onChange={set("serviceId")} error={errors.serviceId}>
            <option value="">Select…</option>
            {(servicesAsync.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.name} · {s.durationMinutes} min</option>)}
          </Select>
          <Select id="barber" label="Barber" required value={form.barberId} onChange={set("barberId")} error={errors.barberId}>
            <option value="">Select…</option>
            {(barbersAsync.data ?? []).map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </Select>
          <Input id="date" label="Date" type="date" required min={today}
            value={form.date} onChange={set("date")} error={errors.date} />
          <Input id="time" label="Time" type="time" required
            value={form.time} onChange={set("time")} error={errors.time} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Booking…" : "Review & confirm"}</Button>
        </form>
      </Card>
    </>
  );
}

function AppointmentTable({ rows }) {
  return (
    <div className="table-wrap">
      <table className="data">
        <thead><tr><th>Service</th><th>Barber</th><th>Date</th><th>Time</th><th>Queue</th><th>Status</th></tr></thead>
        <tbody>
          {rows.map((a) => (
            <tr key={a.id}>
              <td><Link to={`/customer/appointments/${a.id}`}>{a.serviceName ?? a.service?.name ?? `#${a.id}`}</Link></td>
              <td>{a.barberName ?? a.barber?.name ?? "—"}</td>
              <td>{a.appointmentDate ?? "—"}</td>
              <td>{a.appointmentTime ?? "—"}</td>
              <td>{a.queueNumber ?? "—"}</td>
              <td><StatusBadge status={a.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function MyAppointments() {
  const loader = useCallback(() => appointmentService.listMine(), []);
  const { phase, data, error, retry } = useAsync(loader);
  return (
    <>
      <PageHeader title="My appointments" />
      {phase === "loading" && <Loading label="Loading appointments…" />}
      {phase === "empty" && <EmptyState title="No appointments yet" body="Book your first appointment to see it here."
        action={<Link to="/customer/book"><Button>Book now</Button></Link>} />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && <AppointmentTable rows={data} />}
    </>
  );
}

export function AppointmentDetail() {
  const { id } = useParams();
  const loader = useCallback(() => appointmentService.get(id), [id]);
  const { phase, data, error, retry } = useAsync(loader, { isEmpty: (d) => !d });
  return (
    <>
      <PageHeader title="Appointment detail" />
      {phase === "loading" && <Loading />}
      {phase === "empty" && <EmptyState title="Appointment not found" action={<Link to="/customer/appointments">Back to list</Link>} />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <Card>
          <p>Queue number: <strong>{data.queueNumber ?? "—"}</strong></p>
          <p><StatusBadge status={data.status} /></p>
          {data.estimatedWaitMinutes != null && <p>Estimated wait: {data.estimatedWaitMinutes} min</p>}
          <Link to="/customer/queue">Check queue status</Link>
        </Card>
      )}
    </>
  );
}

export function MyQueue() {
  // Customer view of "my queue": own appointments for today, backend-ordered.
  // The full day queue stays admin-only until TASK-007's live queue semantics.
  const today = new Date().toISOString().slice(0, 10);
  const loader = useCallback(() => appointmentService.listMine(), []);
  const { phase, data, error, retry } = useAsync(loader, {
    isEmpty: (d) => !d || d.filter((a) => a.appointmentDate === today).length === 0,
  });
  const rows = (data ?? []).filter((a) => a.appointmentDate === today);
  return (
    <>
      <PageHeader title="Queue status" subtitle="Your visits for today — refresh to update." />
      {phase === "loading" && <Loading label="Loading queue…" />}
      {phase === "empty" && <EmptyState title="Nothing in queue today"
        body="Book an appointment for today to see it here."
        action={<Link to="/customer/book"><Button>Book now</Button></Link>} />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && <AppointmentTable rows={rows} />}
    </>
  );
}
