import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { Button, Card, Input, PageHeader } from "../components/ui.jsx";
import { OtpStep } from "../components/OtpStep.jsx";
import { authService } from "../services/authService.js";
import { ApiError } from "../services/api.js";

function isPhone(v) { return /^\+?[0-9]{7,15}$/.test((v ?? "").trim()); }

function afterVerified(login, navigate) {
  return (session) => {
    login({
      token: session.token, id: session.id, name: session.name,
      phone: session.phone, role: session.role ?? "CUSTOMER",
    });
    navigate(session.role === "ADMIN" ? "/admin" : "/customer", { replace: true });
  };
}

export function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ phone: "", password: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);
  // "form" → password check; "otp" → SMS-code check (purpose varies).
  const [step, setStep] = useState({ name: "form", purpose: "LOGIN", expiresInSeconds: 600 });

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!isPhone(form.phone)) next.phone = "Enter a valid phone number.";
    if (!form.password) next.password = "Enter your password.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const challenge = await authService.login(form);
      setStep({ name: "otp", purpose: challenge.purpose ?? "LOGIN",
        expiresInSeconds: challenge.expiresInSeconds ?? 600 });
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        // Password correct, phone unverified: server issued a REGISTER code.
        setStep({ name: "otp", purpose: "REGISTER", expiresInSeconds: 600 });
      } else {
        setErrors({ form: err instanceof ApiError ? err.message : "Login failed. Please retry." });
      }
    } finally {
      setPending(false);
    }
  }

  if (step.name === "otp") {
    return (
      <>
        <PageHeader title="Check your texts" subtitle="Enter the 6-digit code to finish logging in." />
        <Card>
          <OtpStep phone={form.phone} purpose={step.purpose}
            expiresInSeconds={step.expiresInSeconds}
            onVerified={afterVerified(login, navigate)}
            onBack={() => setStep({ name: "form", purpose: "LOGIN", expiresInSeconds: 600 })} />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Log in" subtitle="Welcome back." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="phone" label="Phone number" type="tel" autoComplete="tel" required
            hint="Digits only, e.g. +919876543210."
            value={form.phone} onChange={set("phone")} error={errors.phone} />
          <Input id="password" label="Password" type="password" autoComplete="current-password" required
            value={form.password} onChange={set("password")} error={errors.password} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Logging in…" : "Continue with SMS code"}</Button>
        </form>
        <p>No account? <Link to="/register">Register</Link></p>
      </Card>
    </>
  );
}

export function Register() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: "", phone: "", password: "", confirm: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const [challenge, setChallenge] = useState(null);

  async function onSubmit(e) {
    e.preventDefault();
    const next = {};
    if (!form.name.trim()) next.name = "Enter your name.";
    if (!isPhone(form.phone)) next.phone = "Enter a valid phone number.";
    if (form.password.length < 8) next.password = "Use at least 8 characters.";
    if (form.confirm !== form.password) next.confirm = "Passwords don't match.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const c = await authService.register({ name: form.name, phone: form.phone, password: form.password });
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
        <PageHeader title="Verify your number" subtitle="Enter the 6-digit code to activate your account." />
        <Card>
          <OtpStep phone={challenge.phone ?? form.phone} purpose="REGISTER"
            expiresInSeconds={challenge.expiresInSeconds ?? 600}
            onVerified={afterVerified(login, navigate)}
            onBack={() => setChallenge(null)} />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Create account" subtitle="Book faster and track your queue." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="name" label="Full name" autoComplete="name" required
            value={form.name} onChange={set("name")} error={errors.name} />
          <Input id="phone" label="Phone number" type="tel" autoComplete="tel" required
            hint="Digits only, e.g. +919876543210."
            value={form.phone} onChange={set("phone")} error={errors.phone} />
          <Input id="password" label="Password" type="password" autoComplete="new-password" required
            hint="At least 8 characters." value={form.password} onChange={set("password")} error={errors.password} />
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
