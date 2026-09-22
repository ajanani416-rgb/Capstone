export function Button({ variant = "primary", ...props }) {
  const cls = variant === "secondary" ? "btn btn-secondary"
    : variant === "danger-ghost" ? "btn btn-danger-ghost"
    : "btn btn-primary";
  return <button type={props.type ?? "button"} {...props} className={`${cls} ${props.className ?? ""}`} />;
}

export function Input({ label, hint, error, required, id, ...props }) {
  return (
    <div className="field">
      <label htmlFor={id}>
        {required ? <span className="required">{label}</span> : label}
      </label>
      <input id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} {...props} />
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="error" id={`${id}-error`} role="alert">{error}</span>}
    </div>
  );
}

export function Select({ label, hint, error, required, id, children, ...props }) {
  return (
    <div className="field">
      <label htmlFor={id}>
        {required ? <span className="required">{label}</span> : label}
      </label>
      <select id={id} aria-invalid={Boolean(error)} {...props}>{children}</select>
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span className="error" role="alert">{error}</span>}
    </div>
  );
}

const BADGE_CLASS = {
  QUEUED: "badge badge-queued",
  IN_SERVICE: "badge badge-in-service",
  COMPLETED: "badge badge-completed",
  CANCELLED: "badge badge-cancelled",
};

const BADGE_ICON = { QUEUED: "◷", IN_SERVICE: "✂", COMPLETED: "✓", CANCELLED: "✕" };

export function StatusBadge({ status }) {
  const key = String(status ?? "").toUpperCase().replace(/[\s-]+/g, "_");
  // Text + icon + color: never color alone (docs/UI_UX.md §5).
  return (
    <span className={BADGE_CLASS[key] ?? "badge badge-queued"}>
      <span aria-hidden="true">{BADGE_ICON[key] ?? "•"}</span> {String(status ?? "Unknown")}
    </span>
  );
}

export function Loading({ label = "Loading…" }) {
  return (
    <div className="state" role="status" aria-live="polite">
      <div className="skeleton" style={{ maxWidth: 320, margin: "0 auto 12px" }} />
      <div className="skeleton" style={{ maxWidth: 240, margin: "0 auto 12px" }} />
      <p>{label}</p>
    </div>
  );
}

export function EmptyState({ title, body, action }) {
  return (
    <div className="state">
      <h2>{title}</h2>
      {body && <p>{body}</p>}
      {action}
    </div>
  );
}

export function ErrorMessage({ message, onRetry }) {
  return (
    <div className="state" role="alert">
      <h2>Couldn&apos;t load this view</h2>
      <p>{message ?? "Something went wrong."}</p>
      {onRetry && <Button onClick={onRetry}>Retry</Button>}
    </div>
  );
}

export function Card({ children, className = "" }) {
  return <section className={`card ${className}`}>{children}</section>;
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <header style={{ display: "flex", gap: 16, alignItems: "baseline", marginBottom: 24, flexWrap: "wrap" }}>
      <div style={{ flex: 1 }}>
        <h1 style={{ margin: 0 }}>{title}</h1>
        {subtitle && <p style={{ color: "var(--text-secondary)", margin: "4px 0 0" }}>{subtitle}</p>}
      </div>
      {actions}
    </header>
  );
}

export function Modal({ title, children, onClose }) {
  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      onKeyDown={(e) => { if (e.key === "Escape") onClose?.(); }}
      role="presentation"
    >
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 style={{ marginTop: 0 }}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
