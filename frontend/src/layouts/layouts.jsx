import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

export function PublicLayout() {
  const { user, logout } = useAuth();
  return (
    <>
      <nav className="navbar" aria-label="Public">
        <Link to="/">Smart Salon</Link>
        <Link to="/services">Services</Link>
        <span className="spacer" />
        {user ? (
          <>
            <Link to={user.role === "ADMIN" ? "/admin" : "/customer"}>Dashboard</Link>
            <button className="btn btn-secondary" onClick={logout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </nav>
      <main className="main" style={{ margin: "0 auto" }}><Outlet /></main>
      <footer className="footer">
        <div className="footer-inner">
          <span className="footer-brand">Smart Salon</span>
          <span>Appointments & queue, without the phone tag.</span>
          <span className="spacer" />
          <small>College capstone MVP · demo build</small>
        </div>
      </footer>
    </>
  );
}

function SidebarShell({ title, mark, links, base, children }) {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark" aria-hidden="true">{mark}</span>{title}</div>
        <div className="nav-label">Menu</div>
        <nav aria-label={title}>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.to === base}
              className={({ isActive }) => (isActive ? "active" : "")}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="session">
          <div>{user?.phone ?? "Signed out"}</div>
          {user && <button onClick={logout}>Log out</button>}
        </div>
      </aside>
      <main className="main">{children ?? <Outlet />}</main>
    </div>
  );
}

export function CustomerLayout() {
  return (
    <SidebarShell
      title="My Salon"
      mark="S"
      base="/customer"
      links={[
        { to: "/customer", label: "Dashboard" },
        { to: "/customer/book", label: "Book appointment" },
        { to: "/customer/appointments", label: "My appointments" },
        { to: "/customer/queue", label: "Queue status" },
      ]}
    />
  );
}

export function AdminLayout() {
  return (
    <SidebarShell
      title="Salon Admin"
      mark="A"
      base="/admin"
      links={[
        { to: "/admin", label: "Dashboard" },
        { to: "/admin/appointments", label: "Appointments" },
        { to: "/admin/queue", label: "Queue" },
        { to: "/admin/services", label: "Services" },
        { to: "/admin/barbers", label: "Barbers" },
        { to: "/admin/customers", label: "Customers" },
      ]}
    />
  );
}
