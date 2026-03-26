import React, { useState, useEffect } from 'react'

export default function ResourceInfo() {
  const [resources, setResources] = useState(null);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    fetch('/bench/resources')
      .then(r => r.json())
      .then(setResources)
      .catch(console.error);
  }, []);

  if (!resources) return null;

  const dbs = Object.keys(resources).filter(k => k !== 'identical' && k !== 'note');

  return (
    <div className="resource-panel">
      <div className="resource-header" onClick={() => setExpanded(!expanded)}>
        <span className="resource-title">Resource Config</span>
        <span className="resource-badge">
          {resources.identical ? 'Identical limits' : 'Different limits'}
        </span>
        <span className="resource-toggle">{expanded ? '▾' : '▸'}</span>
      </div>
      {expanded && (
        <div className="resource-body">
          <p className="resource-note">{resources.note}</p>
          <div className="resource-grid">
            {dbs.map(db => {
              const info = resources[db];
              return (
                <div key={db} className="resource-card">
                  <h4>{db}</h4>
                  <div className="resource-row">
                    <span>CPU Limit</span>
                    <span>{info.cpuLimit}</span>
                  </div>
                  <div className="resource-row">
                    <span>Memory Limit</span>
                    <span>{info.memoryLimit}</span>
                  </div>
                  {info.cpuReserve && (
                    <div className="resource-row">
                      <span>CPU Reserve</span>
                      <span>{info.cpuReserve}</span>
                    </div>
                  )}
                  {info.memoryReserve && (
                    <div className="resource-row">
                      <span>Memory Reserve</span>
                      <span>{info.memoryReserve}</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
