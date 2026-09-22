import { useEffect, useRef, useState } from "react";
import { Button } from "./ui.jsx";
import { authService } from "../services/authService.js";
import { ApiError } from "../services/api.js";

/* Six-box SMS-code entry. Props: phone, purpose ("REGISTER"|"LOGIN"),
   expiresInSeconds, onVerified(session). Handles paste, backspace nav,
   resend with cooldown, and maps 401 (wrong) / 410 (dead → resend) / 400. */

function formatCountdown(total) {
  const m = Math.floor(total / 60);
  const s = String(total % 60).padStart(2, "0");
  return `${m}:${s}`;
}

export function OtpStep({ phone, purpose, expiresInSeconds, onVerified, onBack }) {
  const [boxes, setBoxes] = useState(["", "", "", "", "", ""]);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const [remaining, setRemaining] = useState(expiresInSeconds ?? 120);
  const [cooldown, setCooldown] = useState(30);
  const inputs = useRef([]);

  useEffect(() => {
    inputs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (remaining <= 0) return;
    const t = setTimeout(() => setRemaining((r) => r - 1), 1000);
    return () => clearTimeout(t);
  }, [remaining]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  function setBox(i, v) {
    setBoxes((b) => {
      const next = [...b];
      next[i] = v.replace(/\D/g, "").slice(-1);
      return next;
    });
  }

  function onChange(i, v) {
    setBox(i, v);
    if (v.replace(/\D/g, "") && i < 5) inputs.current[i + 1]?.focus();
  }

  function onKeyDown(i, e) {
    if (e.key === "Backspace" && !boxes[i] && i > 0) inputs.current[i - 1]?.focus();
  }

  function onPaste(e) {
    const digits = (e.clipboardData.getData("text") ?? "").replace(/\D/g, "").slice(0, 6);
    if (!digits) return;
    e.preventDefault();
    setBoxes(digits.padEnd(6, "").split("").slice(0, 6).map((d) => d));
    inputs.current[Math.min(digits.length, 5)]?.focus();
  }

  async function submit(e) {
    e?.preventDefault();
    const code = boxes.join("");
    if (code.length < 6) {
      setError("Enter all 6 digits.");
      return;
    }
    setPending(true);
    setError(null);
    try {
      const session = await authService.verifyOtp({ phone, code, purpose });
      onVerified({
        token: session.token, id: session.id,
        name: session.name, phone: session.phone, role: session.role,
      });
    } catch (err) {
      if (err instanceof ApiError && err.status === 410) {
        setError("That code expired. Request a new one below.");
      } else {
        setError(err instanceof ApiError ? err.message : "Verification failed. Please retry.");
      }
    } finally {
      setPending(false);
    }
  }

  async function resend() {
    setPending(true);
    setError(null);
    try {
      const challenge = await authService.resendOtp({ phone, purpose });
      setBoxes(["", "", "", "", "", ""]);
      setRemaining(challenge.expiresInSeconds ?? 120);
      setCooldown(30);
      inputs.current[0]?.focus();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Resend failed. Please retry.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <p>We texted a 6-digit code to <strong>{phone}</strong>. Enter it below to continue.</p>
      <div className="otp-wrap">
        <div className="otp-boxes" onPaste={onPaste} role="group" aria-label="6-digit verification code">
          {boxes.map((v, i) => (
            <input
              key={i}
              ref={(el) => { inputs.current[i] = el; }}
              value={v}
              inputMode="numeric"
              autoComplete={i === 0 ? "one-time-code" : "off"}
              aria-label={`Digit ${i + 1}`}
              onChange={(e) => onChange(i, e.target.value)}
              onKeyDown={(e) => onKeyDown(i, e)}
            />
          ))}
        </div>
      </div>
      <div className="otp-meta">
        <span>{remaining > 0 ? `Expires in ${formatCountdown(remaining)}` : "Code expired — resend for a new one."}</span>
        <button type="button" disabled={pending || cooldown > 0} onClick={resend}>
          {cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}
        </button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <Button type="submit" disabled={pending}>{pending ? "Verifying…" : "Verify & continue"}</Button>
      {" "}
      {onBack && <Button variant="secondary" onClick={onBack}>Back</Button>}
    </form>
  );
}
