import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { Button, Card, Input, PageHeader } from "../components/ui.jsx";
import { OtpStep } from "../components/OtpStep.jsx";
import { authService } from "../services/authService.js";
import { ApiError } from "../services/api.js";

function isPhone(v) { return /^\+?[0-9]{7,15}$/.test((v ?? "").trim()); }

/* Accepts what users actually type: a bare 10-digit mobile number is
   expanded to +91; anything else passes through to validation. */
function normalizePhone(v) {
  const digits = (v ?? "").replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(digits)) return `+91${digits}`;
  return (v ?? "").trim();
}

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
  const [form, setForm] = useState({ phone: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);
  // "form" → number lookup; "otp" → SMS-code check (purpose varies).
  // step.phone holds the normalized number the code was issued for.
  const [step, setStep] = useState({ name: "form", phone: "", purpose: "LOGIN", expiresInSeconds: 120 });

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function onSubmit(e) {
    e.preventDefault();
    const phone = normalizePhone(form.phone);
    const next = {};
    if (!isPhone(phone)) next.phone = "Enter your 10-digit mobile number.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const challenge = await authService.login({ phone });
      setStep({ name: "otp", phone, purpose: challenge.purpose ?? "LOGIN",
        expiresInSeconds: challenge.expiresInSeconds ?? 120 });
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        // Number exists but is unverified: server issued a REGISTER code.
        setStep({ name: "otp", phone, purpose: "REGISTER", expiresInSeconds: 120 });
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
          <OtpStep phone={step.phone || form.phone} purpose={step.purpose}
            expiresInSeconds={step.expiresInSeconds}
            onVerified={afterVerified(login, navigate)}
            onBack={() => setStep({ name: "form", phone: "", purpose: "LOGIN", expiresInSeconds: 120 })} />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Log in" subtitle="Enter your number — we'll text you a code." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="phone" label="Phone number" type="tel" autoComplete="tel" required
            hint="Enter your 10-digit mobile number."
            value={form.phone} onChange={set("phone")} error={errors.phone} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Sending code…" : "Text me a code"}</Button>
        </form>
        <p>No account? <Link to="/register">Register</Link></p>
      </Card>
    </>
  );
}

export function Register() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: "", phone: "" });
  const [errors, setErrors] = useState({});
  const [pending, setPending] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const [challenge, setChallenge] = useState(null);

  async function onSubmit(e) {
    e.preventDefault();
    const phone = normalizePhone(form.phone);
    const next = {};
    if (!form.name.trim()) next.name = "Enter your name.";
    if (!isPhone(phone)) next.phone = "Enter your 10-digit mobile number.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    setPending(true);
    try {
      const c = await authService.register({ name: form.name, phone });
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
      <PageHeader title="Create account" subtitle="Your number is your account — no password needed." />
      <Card>
        <form onSubmit={onSubmit} noValidate>
          <Input id="name" label="Full name" autoComplete="name" required
            value={form.name} onChange={set("name")} error={errors.name} />
          <Input id="phone" label="Phone number" type="tel" autoComplete="tel" required
            hint="Enter your 10-digit mobile number."
            value={form.phone} onChange={set("phone")} error={errors.phone} />
          {errors.form && <p className="error" role="alert">{errors.form}</p>}
          <Button type="submit" disabled={pending}>{pending ? "Creating…" : "Create account"}</Button>
        </form>
        <p>Have an account? <Link to="/login">Log in</Link></p>
      </Card>
    </>
  );
}
