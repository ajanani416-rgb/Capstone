import { useCallback } from "react";
import { Link } from "react-router-dom";
import { Button, Card } from "../components/ui.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { catalogService } from "../services/catalogService.js";

/* Intro page (order per brief): appointment story hero, top-right login/signup
   lives in the navbar, client views services, admin approves from the schedule.
   Structure follows inspo marquee-hero + Hero-Features-CTA: one big thought,
   live catalog preview (real data only), role tracks, approval flow, CTA. */

function ServicesPreview() {
  const loader = useCallback(() => catalogService.listServices(), []);
  const { phase, data } = useAsync(loader);
  if (phase !== "success" || data.length === 0) return null;
  return (
    <section className="landing-section" aria-label="Popular services">
      <h2>On the menu this week</h2>
      <p className="section-sub">Live from the salon catalog — real prices, real durations.</p>
      <div className="grid grid-3">
        {data.slice(0, 3).map((s) => (
          <Card key={s.id}>
            <h3 style={{ marginTop: 0 }}>{s.name}</h3>
            <p><strong>{s.durationMinutes} min</strong> · ₹{s.price}</p>
            <Link to="/customer/book"><Button variant="secondary">Book this</Button></Link>
          </Card>
        ))}
      </div>
      <p><Link to="/services">View all services →</Link></p>
    </section>
  );
}

export function Landing() {
  return (
    <>
      <section className="hero">
        <span className="eyebrow">Salon appointments, minus the phone tag</span>
        <h1 className="hero-title">Your chair is waiting. Book it in a minute.</h1>
        <p className="hero-sub">
          Pick a service, choose your barber, and get a queue number instantly.
          The salon sees your booking on the schedule and approves it — you just track your turn.
        </p>
        <div className="hero-ctas">
          <Link to="/register"><Button>Get an appointment</Button></Link>
          <Link to="/services"><Button variant="secondary">Browse services</Button></Link>
        </div>
      </section>

      <ServicesPreview />

      <section className="landing-section" aria-label="Choose your path">
        <h2>Two doors, one salon</h2>
        <p className="section-sub">Clients book in seconds. The salon runs the day from one schedule.</p>
        <div className="split">
          <Card className="role-card">
            <h3>✂&ensp;For clients</h3>
            <ol>
              <li>View services with real prices and durations</li>
              <li>Book a barber, date and time — get a queue number</li>
              <li>Track your status and live wait, no phone calls</li>
            </ol>
            <Link to="/register"><Button>Start as a client</Button></Link>
          </Card>
          <Card className="role-card">
            <h3>◷&ensp;For the salon admin</h3>
            <ol>
              <li>See every booking on the day&apos;s schedule</li>
              <li>Approve bookings and move them through the queue</li>
              <li>Manage services, barbers and the customer list</li>
            </ol>
            <Link to="/login"><Button variant="secondary">Admin sign in</Button></Link>
          </Card>
        </div>
      </section>

      <section className="landing-section" aria-label="How approval works">
        <h2>How a booking gets approved</h2>
        <p className="section-sub">Nothing happens by phone or notebook — every step lives in the system.</p>
        <div className="flow">
          <div className="flow-step"><span className="flow-num">1</span><p><strong>Client books.</strong> Service, barber, date, time — queue number issued.</p></div>
          <div className="flow-step"><span className="flow-num">2</span><p><strong>Admin sees it.</strong> The booking lands on the schedule instantly.</p></div>
          <div className="flow-step"><span className="flow-num">3</span><p><strong>Admin approves.</strong> One click starts the service when the chair is free.</p></div>
          <div className="flow-step"><span className="flow-num">4</span><p><strong>Client tracks.</strong> Status and live wait, right on their phone.</p></div>
        </div>
      </section>

      <section className="landing-section" aria-label="Get started">
        <div className="cta-band">
          <h2>Skip the wait next time you need a cut.</h2>
          <p>Create an account, verify your number, and book your first chair.</p>
          <Link to="/register"><Button>Get an appointment</Button></Link>
        </div>
      </section>
    </>
  );
}

export function NotFound() {
  return (
    <div className="state">
      <h1>Page not found</h1>
      <p>The page you asked for doesn&apos;t exist.</p>
      <Link to="/">Back to home</Link>
    </div>
  );
}

export function Forbidden() {
  return (
    <div className="state">
      <h1>Not allowed</h1>
      <p>Your account doesn&apos;t have access to this area.</p>
      <Link to="/">Back to home</Link>
    </div>
  );
}
