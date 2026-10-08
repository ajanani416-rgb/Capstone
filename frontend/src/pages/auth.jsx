import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { Button, Card, Input, PageHeader } from "../components/ui.jsx";
import { OtpStep } from "../components/OtpStep.jsx";
import { authService } from "../services/authService.js";
import { ApiError } from "../services/api.js";

function isEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test((v ?? "").trim()); }

function afterVerified(login, navigate) {
  return (session) => {
    login({
      token: session.token, id: session.id, name: session.name,
      email: session.email, role: session.role ?? "CUSTOMER",
    });
    navigate(session.role === "ADMIN" ? "/admin" : "/customer", { replace: true });
  };
}

export function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);
  // "form" → credentials; "verify" → account exists but email is unverified.
  const [verifyEmail, setVerifyEmail] = useState(null);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!isEmail(form.email)) next.email = "Enter a valid email address.";
    if (!form.password) next.password = "Enter your password.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const session = await authService.login({
        email: form.email.trim().toLowerCase(), password: form.password,
      });
      afterVerified(login, navigate)(session);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        // Password correct but email unverified: server issued a fresh code.
        setVerifyEmail(form.email.trim().toLowerCase());
      } else {
        setErrors({ form: err instanceof ApiError ? err.message : "Login failed. Please retry." });
      }
    } finally {
      setPending(false);
    }
  }

  if (verifyEmail) {
    return (
      <>
        <PageHeader title="Verify your email" subtitle="Your password is correct — one step left." />
        <Card>
          <OtpStep email={verifyEmail} purpose="REGISTER" expiresInSeconds={600}
            onVerified={afterVerified(login, navigate)}
            onBack={() => setVerifyEmail(null)} />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Log in" subtitle="Welcome back — enter your email and password." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="email" label="Email" type="email" autoComplete="email" required
            value={form.email} onChange={set("email")} error={errors.email} />
          <Input id="password" label="Password" type="password" autoComplete="current-password" required
            value={form.password} onChange={set("password")} error={errors.password} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Logging in…" : "Log in"}</Button>
        </form>
        <p>No account? <Link to="/register">Register</Link></p>
      </Card>
    </>
  );
}

export function Register() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const [challenge, setChallenge] = useState(null);

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!form.name.trim()) next.name = "Enter your name.";
    if (!isEmail(form.email)) next.email = "Enter a valid email address.";
    if ((form.password ?? "").length < 8) next.password = "Password must be at least 8 characters.";
    if (form.confirm !== form.password) next.confirm = "Passwords do not match.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const c = await authService.register({
        name: form.name.trim(),
        email: form.email.trim().toLowerCase(),
        password: form.password,
      });
      setChallenge(c);
    } catch (err) {
      setErrors({ form: err instanceof ApiError ? err.message : "Registration failed. Please retry." });
    } finally {
      setPending(false);
    }
  }

  if (challenge) {
    return (
      <>
        <PageHeader title="Verify your email" subtitle="Enter the 6-digit code to activate your account." />
        <Card>
          <OtpStep email={challenge.email ?? form.email.trim().toLowerCase()} purpose="REGISTER"
            expiresInSeconds={challenge.expiresInSeconds ?? 600}
            onVerified={afterVerified(login, navigate)}
            onBack={() => setChallenge(null)} />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Create account" subtitle="Verify your email once — then email + password forever." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="name" label="Full name" autoComplete="name" required
            value={form.name} onChange={set("name")} error={errors.name} />
          <Input id="email" label="Email" type="email" autoComplete="email" required
            value={form.email} onChange={set("email")} error={errors.email} />
          <Input id="password" label="Password" type="password" autoComplete="new-password" required
            hint="At least 8 characters."
            value={form.password} onChange={set("password")} error={errors.password} />
          <Input id="confirm" label="Confirm password" type="password" autoComplete="new-password" required
            value={form.confirm} onChange={set("confirm")} error={errors.confirm} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Creating…" : "Create account"}</Button>
        </form>
        <p>Have an account? <Link to="/login">Log in</Link></p>
      </Card>
    </>
  );
}
