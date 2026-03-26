import React, { useState, useEffect, useCallback } from 'react'
import { runBenchmark, getResults, getDatabases, getPlans, deleteResult, deleteRun, clearResults } from './api'
import Charts from './Charts'
import CompareCharts from './CompareCharts'
import SchemaPage from './SchemaPage'
import ResourceInfo from './ResourceInfo'

const WORKLOAD_OPTIONS = ['insert', 'upsert', 'read'];

export default function App() {
  const [page, setPage] = useState('benchmark');
  const [databases, setDatabases] = useState([]);
  const [plans, setPlans] = useState([]);
  const [results, setResults] = useState([]);
  const [filterDb, setFilterDb] = useState('');
  const [filterPlan, setFilterPlan] = useState('');
  const [running, setRunning] = useState(false);

  // Compare state
  const [selectedPlans, setSelectedPlans] = useState([]);

  // Form state
  const [selectedDb, setSelectedDb] = useState('');
  const [selectedWorkloads, setSelectedWorkloads] = useState(['insert']);
  const [recordCount, setRecordCount] = useState(1000000);
  const [batchSize, setBatchSize] = useState(500);
  const [planName, setPlanName] = useState('');

  const loadDatabases = useCallback(async () => {
    try {
      const dbs = await getDatabases();
      setDatabases(dbs);
      if (dbs.length > 0 && !selectedDb) setSelectedDb(dbs[0]);
    } catch (e) {
      console.error('Failed to load databases:', e);
    }
  }, [selectedDb]);

  const loadPlans = useCallback(async () => {
    try {
      const p = await getPlans();
      setPlans(p);
    } catch (e) {
      console.error('Failed to load plans:', e);
    }
  }, []);

  const loadResults = useCallback(async () => {
    try {
      const data = await getResults(filterDb || undefined, filterPlan || undefined);
      setResults(data);
    } catch (e) {
      console.error('Failed to load results:', e);
    }
  }, [filterDb, filterPlan]);

  useEffect(() => { loadDatabases(); }, [loadDatabases]);
  useEffect(() => { loadPlans(); }, [loadPlans]);
  useEffect(() => { loadResults(); }, [loadResults]);

  const refresh = async () => {
    await Promise.all([loadResults(), loadPlans()]);
  };

  const toggleWorkload = (w) => {
    setSelectedWorkloads(prev =>
      prev.includes(w) ? prev.filter(x => x !== w) : [...prev, w]
    );
  };

  const togglePlanSelection = (plan) => {
    setSelectedPlans(prev =>
      prev.includes(plan) ? prev.filter(p => p !== plan) : [...prev, plan]
    );
  };

  const handleRun = async () => {
    if (!selectedDb || selectedWorkloads.length === 0) return;
    setRunning(true);
    try {
      await runBenchmark({
        database: selectedDb,
        workloads: selectedWorkloads,
        recordCount,
        batchSize,
        planName: planName || undefined,
      });
      await refresh();
    } catch (e) {
      alert('Benchmark failed: ' + e.message);
    } finally {
      setRunning(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this benchmark result?')) return;
    try {
      await deleteResult(id);
      await refresh();
    } catch (e) {
      alert('Delete failed: ' + e.message);
    }
  };

  const handleDeleteRun = async (runId) => {
    if (!confirm('Delete all results from this run?')) return;
    try {
      await deleteRun(runId);
      await refresh();
    } catch (e) {
      alert('Delete failed: ' + e.message);
    }
  };

  const handleClearAll = async () => {
    if (!confirm('Delete ALL benchmark results? This cannot be undone.')) return;
    try {
      await clearResults();
      setSelectedPlans([]);
      await refresh();
    } catch (e) {
      alert('Clear failed: ' + e.message);
    }
  };

  // Group results by runId for display
  const groupedByRun = results.reduce((acc, r) => {
    if (!acc[r.runId]) {
      acc[r.runId] = { runId: r.runId, planName: r.planName, database: r.database, results: [], createdAt: r.createdAt };
    }
    acc[r.runId].results.push(r);
    return acc;
  }, {});

  return (
    <div className="app">
      <div className="header">
        <h1>Multi DB Benchmark</h1>
        <nav className="nav-tabs">
          <button className={`nav-tab ${page === 'benchmark' ? 'active' : ''}`}
                  onClick={() => setPage('benchmark')}>Benchmark</button>
          <button className={`nav-tab ${page === 'compare' ? 'active' : ''}`}
                  onClick={() => setPage('compare')}>Compare Plans</button>
          <button className={`nav-tab ${page === 'schema' ? 'active' : ''}`}
                  onClick={() => setPage('schema')}>Schema & Queries</button>
        </nav>
      </div>

      <ResourceInfo />

      {page === 'schema' ? (
        <SchemaPage />
      ) : page === 'compare' ? (
        <div>
          <div className="panel">
            <h2>Select Plans to Compare</h2>
            {plans.length === 0 ? (
              <div className="empty-state">No plans yet. Run benchmarks with a plan name first.</div>
            ) : (
              <div className="plan-selector">
                {plans.map(p => (
                  <label key={p} className={`plan-chip ${selectedPlans.includes(p) ? 'selected' : ''}`}>
                    <input type="checkbox" checked={selectedPlans.includes(p)}
                           onChange={() => togglePlanSelection(p)} />
                    {p}
                  </label>
                ))}
              </div>
            )}
          </div>
          {selectedPlans.length > 0 && <CompareCharts plans={selectedPlans} />}
        </div>
      ) : (
        <>
          <div className="panel">
            <h2>Run Benchmark</h2>
            <div className="form-row">
              <div className="form-group">
                <label>Plan Name</label>
                <input type="text" value={planName} placeholder="e.g. mysql-batch500-1M"
                       onChange={e => setPlanName(e.target.value)} />
              </div>
              <div className="form-group">
                <label>Database</label>
                <select value={selectedDb} onChange={e => setSelectedDb(e.target.value)}>
                  {databases.map(db => <option key={db} value={db}>{db}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Record Count</label>
                <input type="number" value={recordCount}
                       onChange={e => setRecordCount(Number(e.target.value))} min={1000} step={1000} />
              </div>
              <div className="form-group">
                <label>Batch Size</label>
                <input type="number" value={batchSize}
                       onChange={e => setBatchSize(Number(e.target.value))} min={100} step={100} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Workloads</label>
                <div className="checkbox-group">
                  {WORKLOAD_OPTIONS.map(w => (
                    <label key={w}>
                      <input type="checkbox" checked={selectedWorkloads.includes(w)}
                             onChange={() => toggleWorkload(w)} />
                      {w}
                    </label>
                  ))}
                </div>
              </div>
              <button onClick={handleRun} disabled={running || selectedWorkloads.length === 0}>
                {running ? 'Running...' : 'Run Benchmark'}
              </button>
            </div>
          </div>

          <Charts results={results} />

          <div className="panel">
            <h2>Results History</h2>
            <div className="filter-row">
              <label>Filter:</label>
              <select value={filterPlan} onChange={e => { setFilterPlan(e.target.value); setFilterDb(''); }}>
                <option value="">All Plans</option>
                {plans.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
              <select value={filterDb} onChange={e => { setFilterDb(e.target.value); setFilterPlan(''); }}>
                <option value="">All DBs</option>
                {databases.map(db => <option key={db} value={db}>{db}</option>)}
              </select>
              {results.length > 0 && (
                <button className="btn-danger" onClick={handleClearAll}>Clear All</button>
              )}
            </div>
            {Object.keys(groupedByRun).length === 0 ? (
              <div className="empty-state">No benchmark results yet. Run a benchmark to see results.</div>
            ) : (
              Object.values(groupedByRun).map(group => (
                <div key={group.runId} className="run-group">
                  <div className="run-header">
                    <div>
                      <span className="run-plan-name">{group.planName}</span>
                      <span className="run-meta">{group.database} &middot; {group.createdAt ? new Date(group.createdAt).toLocaleString() : ''}</span>
                    </div>
                    <button className="btn-icon-danger" onClick={() => handleDeleteRun(group.runId)} title="Delete run">
                      &times;
                    </button>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th>Workload</th>
                        <th>Records</th>
                        <th>Duration</th>
                        <th>Throughput</th>
                        <th>Avg Latency</th>
                        <th>Errors</th>
                        <th>Storage</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {group.results.map(r => (
                        <tr key={r.id}>
                          <td>{r.workload}</td>
                          <td>{r.recordCount?.toLocaleString()}</td>
                          <td>{(r.durationMs / 1000).toFixed(1)}s</td>
                          <td>{Math.round(r.throughput).toLocaleString()}/s</td>
                          <td>{r.avgLatencyMs < 1 ? r.avgLatencyMs.toFixed(4) : r.avgLatencyMs.toFixed(2)}ms</td>
                          <td>{r.errorCount}</td>
                          <td>{r.storageMb?.toFixed(1)} MB</td>
                          <td>
                            <button className="btn-icon-danger" onClick={() => handleDelete(r.id)} title="Delete">
                              &times;
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
