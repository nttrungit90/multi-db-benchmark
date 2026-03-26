import React, { useState, useEffect, useMemo } from 'react'
import { getResults } from './api'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts'

const PALETTE = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
  '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1',
];

export default function CompareCharts({ plans }) {
  const [allResults, setAllResults] = useState([]);

  useEffect(() => {
    Promise.all(plans.map(p => getResults(undefined, p)))
      .then(arrays => setAllResults(arrays.flat()))
      .catch(console.error);
  }, [plans]);

  const { throughputData, latencyData, storageData, workloads } = useMemo(() => {
    if (allResults.length === 0) return { throughputData: [], latencyData: [], storageData: [], workloads: [] };

    const workloads = [...new Set(allResults.map(r => r.workload))];

    // For each workload, build a row with each plan as a bar
    const throughputData = workloads.map(w => {
      const row = { workload: w };
      for (const plan of plans) {
        const match = allResults.find(r => r.planName === plan && r.workload === w);
        if (match) row[plan] = Math.round(match.throughput);
      }
      return row;
    });

    const latencyData = workloads.map(w => {
      const row = { workload: w };
      for (const plan of plans) {
        const match = allResults.find(r => r.planName === plan && r.workload === w);
        if (match) row[plan] = Number(match.avgLatencyMs.toFixed(4));
      }
      return row;
    });

    const durationData = workloads.map(w => {
      const row = { workload: w };
      for (const plan of plans) {
        const match = allResults.find(r => r.planName === plan && r.workload === w);
        if (match) row[plan] = Number((match.durationMs / 1000).toFixed(1));
      }
      return row;
    });

    // Storage: one entry per plan (latest)
    const storageData = plans.map(plan => {
      const planResults = allResults.filter(r => r.planName === plan);
      const latest = planResults.length > 0 ? planResults[planResults.length - 1] : null;
      return { plan, storageMb: latest ? Number(latest.storageMb.toFixed(1)) : 0 };
    });

    return { throughputData, latencyData, durationData, storageData, workloads };
  }, [allResults, plans]);

  if (allResults.length === 0) {
    return <div className="empty-state">Loading comparison data...</div>;
  }

  return (
    <div className="charts-grid">
      <div className="chart-card">
        <h3>Throughput Comparison (records/sec)</h3>
        <ResponsiveContainer width="100%" height={350}>
          <BarChart data={throughputData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="workload" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Legend />
            {plans.map((plan, i) => (
              <Bar key={plan} dataKey={plan} fill={PALETTE[i % PALETTE.length]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="chart-card">
        <h3>Latency Comparison (ms/record)</h3>
        <ResponsiveContainer width="100%" height={350}>
          <BarChart data={latencyData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="workload" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Legend />
            {plans.map((plan, i) => (
              <Bar key={plan} dataKey={plan} fill={PALETTE[i % PALETTE.length]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="chart-card">
        <h3>Storage Comparison (MB)</h3>
        <ResponsiveContainer width="100%" height={350}>
          <BarChart data={storageData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="plan" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Bar dataKey="storageMb" name="Storage (MB)">
              {storageData.map((_, i) => (
                <rect key={i} fill={PALETTE[i % PALETTE.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Detail table */}
      <div className="chart-card" style={{ gridColumn: '1 / -1' }}>
        <h3>Detailed Comparison</h3>
        <table>
          <thead>
            <tr>
              <th>Plan</th>
              <th>Database</th>
              <th>Workload</th>
              <th>Records</th>
              <th>Duration</th>
              <th>Throughput</th>
              <th>Avg Latency</th>
              <th>Storage</th>
            </tr>
          </thead>
          <tbody>
            {allResults
              .filter(r => plans.includes(r.planName))
              .map(r => (
                <tr key={r.id}>
                  <td><span className="plan-badge">{r.planName}</span></td>
                  <td>{r.database}</td>
                  <td>{r.workload}</td>
                  <td>{r.recordCount?.toLocaleString()}</td>
                  <td>{(r.durationMs / 1000).toFixed(1)}s</td>
                  <td>{Math.round(r.throughput).toLocaleString()}/s</td>
                  <td>{r.avgLatencyMs < 1 ? r.avgLatencyMs.toFixed(4) : r.avgLatencyMs.toFixed(2)}ms</td>
                  <td>{r.storageMb?.toFixed(1)} MB</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
