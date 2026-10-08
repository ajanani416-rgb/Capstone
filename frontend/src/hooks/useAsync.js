import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "../services/api.js";

const defaultIsEmpty = (d) => Array.isArray(d) && d.length === 0;

/* Load-once async state machine: idle → loading → success | empty | error.
   Callers decide emptiness via isEmpty(data). Retry re-runs the loader.
   The isEmpty predicate is read through a ref so inline/default callbacks
   never retrigger the fetch effect (a fresh function identity per render
   used to cause an infinite request loop — found in-browser, TASK-009). */
export function useAsync(loader, options = {}) {
  const { isEmpty = defaultIsEmpty } = options;
  const isEmptyRef = useRef(isEmpty);
  useEffect(() => {
    isEmptyRef.current = isEmpty;
  }, [isEmpty]);
  const [state, setState] = useState({ phase: "loading", data: null, error: null });
  const run = useCallback(async () => {
    setState({ phase: "loading", data: null, error: null });
    try {
      const data = await loader();
      setState({ phase: isEmptyRef.current(data) ? "empty" : "success", data, error: null });
    } catch (e) {
      const message = e instanceof ApiError ? e.message : "Something went wrong. Please retry.";
      setState({ phase: "error", data: null, error: { message, status: e?.status } });
    }
  }, [loader]);

  useEffect(() => { run(); }, [run]);
  return { ...state, retry: run };
}
