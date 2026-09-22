import { useCallback, useState } from "react";
import { Link } from "react-router-dom";
import {
  Button, Card, EmptyState, ErrorMessage, Input, Loading, Modal, PageHeader, StatusBadge,
} from "../components/ui.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { appointmentService } from "../services/appointmentService.js";
import { catalogService } from "../services/catalogService.js";
import { ApiError } from "../services/api.js";

/* Legal forward moves (locked, backend-enforced; buttons mirror the map so
   illegal moves are unclickable, and 409 is still handled on race). */
const NEXT = {
  QUEUED: ["IN_SERVICE", "CANCELLED"],
  IN_SERVICE: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

const ACTION_LABEL = {
  IN_SERVICE: "Approve & start",
  COMPLETED: "Complete",
  CANCELLED: "Cancel",
};

export function AdminDashboard() {
  const today = new Date().toISOString().slice(0, 10);
  const loader = useCallback(() => appointmentService.listAll({ date: today }), [today]);
  const { phase, data, error, retry } = useAsync(loader);
  const rows = data ?? [];
  const count = (s) => rows.filter((a) => a.status === s).length;

  return (
    <>
      <PageHeader title="Today at a glance" subtitle="Real numbers from the backend — nothing invented." />
      {phase === "loading" && <Loading />}
      {phase === "empty" && <EmptyState title="No appointments today" body="New bookings will appear here." />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <div className="grid grid-3">
          <Card className="stat">
            <div className="stat-top"><span className="stat-ico amber" aria-hidden="true">◷</span><span className="stat-label">Total today</span></div>
            <div className="stat-value">{rows.length}</div>
            <Link className="stat-link" to="/admin/appointments">View all</Link>
          </Card>
          <Card className="stat">
            <div className="stat-top"><span className="stat-ico blue" aria-hidden="true">◔</span><span className="stat-label">Queued</span></div>
            <div className="stat-value">{count("QUEUED")}</div>
            <Link className="stat-link" to="/admin/queue">Open queue</Link>
          </Card>
          <Card className="stat">
            <div className="stat-top"><span className="stat-ico green" aria-hidden="true">✂</span><span className="stat-label">In service</span></div>
            <div className="stat-value">{count("IN_SERVICE")}</div>
            <Link className="stat-link" to="/admin/queue">Open queue</Link>
          </Card>
        </div>
      )}
    </>
  );
}

export function AdminAppointments() {
  const [dateFilter, setDateFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const loader = useCallback(() => appointmentService.listAll({
    ...(dateFilter ? { date: dateFilter } : {}),
    ...(statusFilter ? { status: statusFilter } : {}),
  }), [dateFilter, statusFilter]);
  const { phase, data, error, retry } = useAsync(loader);
  const [updating, setUpdating] = useState(null);
  const [rows, setRows] = useState(null);
  const [rowError, setRowError] = useState(null);
  const list = rows ?? data ?? [];

  async function move(id, to) {
    setUpdating(id);
    setRowError(null);
    const prev = list;
    setRows(prev.map((a) => (a.id === id ? { ...a, status: to } : a)));
    try {
      const saved = await appointmentService.updateStatus(id, to);
      setRows(prev.map((a) => (a.id === id ? saved : a)));
    } catch (e) {
      setRows(prev); // rollback; 409 names the legal targets
      setRowError({ id, message: e instanceof ApiError ? e.message : "Update failed. Please retry." });
    } finally {
      setUpdating(null);
    }
  }

  return (
    <>
      <PageHeader title="Appointments" subtitle="Filter by day or status, then advance the queue." />
      <div className="card" style={{ marginBottom: 24, display: "flex", gap: 16, flexWrap: "wrap" }}>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="flt-date">Date</label>
          <input id="flt-date" type="date" value={dateFilter} onChange={(e) => { setRows(null); setDateFilter(e.target.value); }} />
        </div>
        <div className="field" style={{ margin: 0 }}>
          <label htmlFor="flt-status">Status</label>
          <select id="flt-status" value={statusFilter} onChange={(e) => { setRows(null); setStatusFilter(e.target.value); }}>
            <option value="">All</option>
            <option value="QUEUED">Queued</option>
            <option value="IN_SERVICE">In service</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
        {(dateFilter || statusFilter) && (
          <Button variant="secondary" onClick={() => { setRows(null); setDateFilter(""); setStatusFilter(""); }}>
            Clear filters
          </Button>
        )}
      </div>
      {phase === "loading" && <Loading label="Loading appointments…" />}
      {phase === "empty" && <EmptyState title="No appointments match" body="Adjust or clear the filters." />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>ID</th><th>Service</th><th>Date</th><th>Time</th><th>Queue</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id}>
                  <td>{a.id}</td><td>{a.serviceName}</td><td>{a.appointmentDate}</td>
                  <td>{a.appointmentTime}</td><td>{a.queueNumber ?? "—"}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td>
                    {(NEXT[a.status] ?? []).map((to) => (
                      <Button key={to} variant="secondary" disabled={updating === a.id}
                        onClick={() => move(a.id, to)} style={{ marginRight: 8, marginBottom: 4 }}>
                        {updating === a.id ? "Saving…" : ACTION_LABEL[to]}
                      </Button>
                    ))}
                    {(NEXT[a.status] ?? []).length === 0 && <span style={{ color: "var(--text-secondary)" }}>Terminal</span>}
                    {rowError?.id === a.id && <p className="error" role="alert">{rowError.message}</p>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export function AdminQueue() {
  // Live day queue: backend order (immutable) + computed waits.
  const today = new Date().toISOString().slice(0, 10);
  const loader = useCallback(() => appointmentService.todayQueue(today), [today]);
  const { phase, data, error, retry } = useAsync(loader);
  const rows = data ?? [];
  return (
    <>
      <PageHeader title="Queue" subtitle={`Today (${today}) — waits sum the service time ahead on each barber's line.`} />
      {phase === "loading" && <Loading label="Loading queue…" />}
      {phase === "empty" && <EmptyState title="Queue is empty" body="No appointments scheduled for today." />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>Position</th><th>Service</th><th>Barber</th><th>Time</th><th>Queue #</th><th>Wait</th><th>Status</th></tr></thead>
            <tbody>
              {rows.map((a, i) => (
                <tr key={a.id}><td>{i + 1}</td><td>{a.serviceName}</td><td>{a.barberName}</td>
                  <td>{a.appointmentTime}</td><td>{a.queueNumber ?? "—"}</td>
                  <td>{a.estimatedWaitMinutes == null ? "—" : `${a.estimatedWaitMinutes} min`}</td>
                  <td><StatusBadge status={a.status} /></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function CatalogManager({ title, singular, loader, headers, renderRow, fields, initial, onSave, onDelete }) {
  const async = useAsync(loader);
  const [modal, setModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(initial);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(null);

  function openAdd() {
    setEditing(null);
    setForm(initial);
    setError(null);
    setModal(true);
  }

  function openEdit(row) {
    setEditing(row);
    setForm({ ...row });
    setError(null);
    setModal(true);
  }

  async function save(e) {
    e.preventDefault();
    setPending(true);
    setError(null);
    try {
      await onSave(form, editing);
      setModal(false);
      async.retry();
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
        setError(`${err.message} (${Object.values(err.fieldErrors).join(" ")})`);
      } else {
        setError(err instanceof ApiError ? err.message : "Save failed. Please retry.");
      }
    } finally {
      setPending(false);
    }
  }

  async function remove(row) {
    setError(null);
    try {
      await onDelete(row);
      setConfirmDelete(null);
      async.retry();
    } catch (err) {
      // Referenced rows answer 409 with the deactivate-instead remedy.
      setConfirmDelete(null);
      setError(err instanceof ApiError ? err.message : "Delete failed. Please retry.");
    }
  }

  const set = (k) => (e) => setForm((f) => ({
    ...f, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value,
  }));

  return (
    <>
      <PageHeader title={title} actions={<Button onClick={openAdd}>Add new</Button>} />
      {error && !modal && <p className="error" role="alert">{error}</p>}
      {async.phase === "loading" && <Loading />}
      {async.phase === "empty" && <EmptyState title={`No ${singular} yet`} action={<Button onClick={openAdd}>Add the first</Button>} />}
      {async.phase === "error" && <ErrorMessage message={async.error.message} onRetry={async.retry} />}
      {async.phase === "success" && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr>{headers.map((h) => <th key={h}>{h}</th>)}<th>Actions</th></tr></thead>
            <tbody>
              {async.data.map((row) => (
                <tr key={row.id}>
                  {renderRow(row)}
                  <td>
                    <Button variant="secondary" onClick={() => openEdit(row)} style={{ marginRight: 8 }}>Edit</Button>
                    {confirmDelete === row.id ? (
                      <>
                        <Button variant="danger-ghost" onClick={() => remove(row)}>Confirm delete</Button>
                        <Button variant="secondary" onClick={() => setConfirmDelete(null)}>Keep</Button>
                      </>
                    ) : (
                      <Button variant="danger-ghost" onClick={() => setConfirmDelete(row.id)}>Delete</Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal && (
        <Modal title={editing ? `Edit ${singular}` : `Add ${singular}`} onClose={() => setModal(false)}>
          <form onSubmit={save}>
            {fields(form, set)}
            {error && <p className="error" role="alert">{error}</p>}
            <Button type="submit" disabled={pending}>{pending ? "Saving…" : "Save"}</Button>
            {" "}
            <Button variant="secondary" onClick={() => setModal(false)}>Cancel</Button>
          </form>
        </Modal>
      )}
    </>
  );
}

export function AdminServices() {
  const loader = useCallback(() => catalogService.listServices(), []);
  return (
    <CatalogManager title="Services" singular="service" loader={loader}
      headers={["Name", "Duration", "Price", "Active"]}
      initial={{ name: "", description: "", durationMinutes: "", price: "", active: true }}
      onSave={(f, editing) => {
        const body = {
          name: f.name, description: f.description || null,
          durationMinutes: Number(f.durationMinutes), price: f.price, active: Boolean(f.active),
        };
        return editing ? catalogService.updateService(editing.id, body) : catalogService.createService(body);
      }}
      onDelete={(row) => catalogService.deleteService(row.id)}
      fields={(form, set) => (
        <>
          <Input id="svc-name" label="Name" required value={form.name ?? ""} onChange={set("name")} />
          <Input id="svc-desc" label="Description" value={form.description ?? ""} onChange={set("description")} />
          <Input id="svc-dur" label="Duration (minutes)" type="number" min="1" required
            value={form.durationMinutes ?? ""} onChange={set("durationMinutes")} />
          <Input id="svc-price" label="Price" type="number" min="0" step="0.01" required
            value={form.price ?? ""} onChange={set("price")} />
          <div className="field">
            <label htmlFor="svc-active"><input id="svc-active" type="checkbox" checked={Boolean(form.active)} onChange={set("active")} /> Active (visible to customers)</label>
          </div>
        </>
      )}
      renderRow={(s) => (
        <>
          <td>{s.name}</td><td>{s.durationMinutes} min</td>
          <td>₹{s.price}</td><td>{s.active ? "Yes" : "No"}</td>
        </>
      )} />
  );
}

export function AdminBarbers() {
  const loader = useCallback(() => catalogService.listBarbers(), []);
  return (
    <CatalogManager title="Barbers" singular="barber" loader={loader}
      headers={["Name", "Specialization", "Active"]}
      initial={{ name: "", specialization: "", active: true }}
      onSave={(f, editing) => {
        const body = { name: f.name, specialization: f.specialization || null, active: Boolean(f.active) };
        return editing ? catalogService.updateBarber(editing.id, body) : catalogService.createBarber(body);
      }}
      onDelete={(row) => catalogService.deleteBarber(row.id)}
      fields={(form, set) => (
        <>
          <Input id="brb-name" label="Name" required value={form.name ?? ""} onChange={set("name")} />
          <Input id="brb-spec" label="Specialization" value={form.specialization ?? ""} onChange={set("specialization")} />
          <div className="field">
            <label htmlFor="brb-active"><input id="brb-active" type="checkbox" checked={Boolean(form.active)} onChange={set("active")} /> Active (bookable)</label>
          </div>
        </>
      )}
      renderRow={(b) => (
        <>
          <td>{b.name}</td><td>{b.specialization ?? "—"}</td>
          <td>{b.active ? "Yes" : "No"}</td>
        </>
      )} />
  );
}

export function AdminCustomers() {
  const loader = useCallback(() => catalogService.listUsers(), []);
  const { phase, data, error, retry } = useAsync(loader);
  return (
    <>
      <PageHeader title="Customers" subtitle="Read-only directory — accounts change only via registration." />
      {phase === "loading" && <Loading label="Loading customers…" />}
      {phase === "empty" && <EmptyState title="No customers yet" />}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <div className="table-wrap">
          <table className="data">
            <thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Role</th><th>Since</th></tr></thead>
            <tbody>
              {data.map((u) => (
                <tr key={u.id}><td>{u.id}</td><td>{u.name}</td><td>{u.phone}</td>
                  <td>{u.role}</td><td>{u.createdAt?.slice(0, 10) ?? "—"}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
