import { useCallback } from "react";
import { Link } from "react-router-dom";
import { Button, Card, EmptyState, ErrorMessage, Loading, PageHeader } from "../components/ui.jsx";
import { useAsync } from "../hooks/useAsync.js";
import { catalogService } from "../services/catalogService.js";

export function Services() {
  const loader = useCallback(() => catalogService.listServices(), []);
  const { phase, data, error, retry } = useAsync(loader);

  return (
    <>
      <PageHeader title="Services" subtitle="Real catalog, real prices." />
      {phase === "loading" && <Loading label="Loading services…" />}
      {phase === "empty" && (
        <EmptyState title="No services yet"
          body="The salon hasn't published its catalog. Check back soon."
          action={<Button variant="secondary" onClick={retry}>Refresh</Button>} />
      )}
      {phase === "error" && <ErrorMessage message={error.message} onRetry={retry} />}
      {phase === "success" && (
        <div className="grid grid-3">
          {data.map((s) => (
            <Card key={s.id}>
              <h2 style={{ marginTop: 0 }}>{s.name}</h2>
              {s.description && <p>{s.description}</p>}
              <p><strong>{s.durationMinutes} min</strong> · ₹{s.price}</p>
              <Link to="/customer/book"><Button>Book this</Button></Link>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
