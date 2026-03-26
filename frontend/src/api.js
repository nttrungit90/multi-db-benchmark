const BASE = '/bench';

export async function runBenchmark(request) {
  const res = await fetch(`${BASE}/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getResults(db, plan) {
  const params = new URLSearchParams();
  if (db) params.set('db', db);
  if (plan) params.set('plan', plan);
  const qs = params.toString();
  const res = await fetch(`${BASE}/results${qs ? '?' + qs : ''}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getDatabases() {
  const res = await fetch(`${BASE}/databases`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getPlans() {
  const res = await fetch(`${BASE}/plans`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function deleteResult(id) {
  const res = await fetch(`${BASE}/results/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function deleteRun(runId) {
  const res = await fetch(`${BASE}/runs/${runId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function clearResults() {
  const res = await fetch(`${BASE}/results`, { method: 'DELETE' });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function getSchemas() {
  const res = await fetch(`${BASE}/schemas`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
