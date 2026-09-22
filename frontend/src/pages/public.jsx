import { Link } from "react-router-dom";
import { Button, Card, PageHeader } from "../components/ui.jsx";

export function Landing() {
  return (
    <>
      <PageHeader
        title="Book your chair. Skip the wait."
        subtitle="Browse services, pick your barber, get a queue number, and track your appointment — all in one place."
        actions={
          <>
            <Link to="/services"><Button>Browse services</Button></Link>
            <Link to="/register"><Button variant="secondary">Create account</Button></Link>
          </>
        }
      />
      <div className="grid grid-3">
        <Card><h2>1. Pick a service</h2><p>Real prices and durations from the salon catalog — no phone tag.</p></Card>
        <Card><h2>2. Get a queue number</h2><p>Assigned at booking by the backend, so first-come means first-served.</p></Card>
        <Card><h2>3. Track status</h2><p>See where you stand without calling the front desk.</p></Card>
      </div>
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
