import React, { useState, useEffect } from 'react'
import { getSchemas } from './api'

const DB_COLORS = {
  mysql: '#3b82f6',
  mongodb: '#10b981',
  postgresql: '#f59e0b',
  redis: '#ef4444',
};

export default function SchemaPage() {
  const [schemas, setSchemas] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    getSchemas().then(data => {
      setSchemas(data);
      if (data.length > 0) setSelected(data[0].database);
    }).catch(console.error);
  }, []);

  const current = schemas.find(s => s.database === selected);

  return (
    <div>
      <div className="schema-tabs">
        {schemas.map(s => (
          <button
            key={s.database}
            className={`schema-tab ${selected === s.database ? 'active' : ''}`}
            style={selected === s.database ? { borderColor: DB_COLORS[s.database] || '#94a3b8' } : {}}
            onClick={() => setSelected(s.database)}
          >
            {s.database}
          </button>
        ))}
      </div>

      {current && (
        <div className="schema-content">
          <div className="schema-section">
            <h3>Schema Definition</h3>
            <pre className="code-block">{current.schemaDefinition}</pre>
          </div>

          <div className="schema-section">
            <h3>Primary Key</h3>
            <pre className="code-block">{current.primaryKey}</pre>
          </div>

          <div className="schema-section">
            <h3>Indexes</h3>
            {current.indexes.map((idx, i) => (
              <pre key={i} className="code-block">{idx}</pre>
            ))}
          </div>

          <div className="schema-grid">
            <div className="schema-section">
              <h3>Insert Command</h3>
              <pre className="code-block">{current.insertCommand}</pre>
            </div>

            <div className="schema-section">
              <h3>Upsert Command</h3>
              <pre className="code-block">{current.upsertCommand}</pre>
            </div>

            <div className="schema-section">
              <h3>Read Query</h3>
              <pre className="code-block">{current.readCommand}</pre>
            </div>
          </div>
        </div>
      )}

      {schemas.length === 0 && (
        <div className="empty-state">Loading schema information...</div>
      )}
    </div>
  );
}
