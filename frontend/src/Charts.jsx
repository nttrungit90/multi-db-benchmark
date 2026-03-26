import React, { useMemo } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts'

const COLORS = {
  mysql: '#3b82f6',
  mongodb: '#10b981',
  postgresql: '#f59e0b',
  redis: '#ef4444',
  cassandra: '#8b5cf6',
};

function getColor(db) {
  return COLORS[db] || '#94a3b8';
}

export default function Charts({ results }) {
  const chartData = useMemo(() => {
    if (!results || results.length === 0) return { throughput: [], latency: [], storage: [] };

    // Get latest result per (database, workload) pair
    const latest = {};
    for (const r of results) {
      const key = `${r.database}-${r.workload}`;
      if (!latest[key]) latest[key] = r;
    }
    const entries = Object.values(latest);

    // Group by workload for throughput/latency
    const workloads = [...new Set(entries.map(e => e.workload))];
    const dbs = [...new Set(entries.map(e => e.database))];

    const throughput = workloads.map(w => {
      const row = { workload: w };
      for (const e of entries) {
        if (e.workload === w) row[e.database] = Math.round(e.throughput);
      }
      return row;
    });

    const latency = workloads.map(w => {
      const row = { workload: w };
      for (const e of entries) {
        if (e.workload === w) row[e.database] = Number(e.avgLatencyMs.toFixed(4));
      }
      return row;
    });

    // Storage: latest per db
    const storageMap = {};
    for (const e of entries) {
      storageMap[e.database] = Number(e.storageMb?.toFixed(1) || 0);
    }
    const storage = Object.entries(storageMap).map(([db, mb]) => ({ database: db, storageMb: mb }));

    return { throughput, latency, storage, dbs };
  }, [results]);

  if (!results || results.length === 0) {
    return null;
  }

  const { throughput, latency, storage, dbs } = chartData;

  return (
    <div className="charts-grid">
      <div className="chart-card">
        <h3>Throughput (records/sec)</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={throughput}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="workload" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Legend />
            {dbs.map(db => (
              <Bar key={db} dataKey={db} fill={getColor(db)} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="chart-card">
        <h3>Average Latency (ms/record)</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={latency}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="workload" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Legend />
            {dbs.map(db => (
              <Bar key={db} dataKey={db} fill={getColor(db)} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="chart-card">
        <h3>Storage Size (MB)</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={storage}>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
            <XAxis dataKey="database" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #475569' }} />
            <Bar dataKey="storageMb" fill="#3b82f6" name="Storage (MB)">
              {storage.map((entry, i) => (
                <rect key={i} fill={getColor(entry.database)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
