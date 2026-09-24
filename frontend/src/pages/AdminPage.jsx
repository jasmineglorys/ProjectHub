import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getAdminProjects,
  updateProjectStatus,
  adminDeleteProject,
  getAdminStats,
} from '../api/adminApi';
import { extractError } from '../api/axiosConfig';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { imageUrl } from '../constants';

const TABS = [
  { id: 'PENDING', label: '⏳ Pending' },
  { id: 'APPROVED', label: '✅ Approved' },
  { id: 'REJECTED', label: '❌ Rejected' },
  { id: 'ANALYTICS', label: '📊 Analytics' },
];

export default function AdminPage() {
  const [tab, setTab] = useState('PENDING');
  const [projects, setProjects] = useState([]);
  const [stats, setStats] = useState(null);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busyId, setBusyId] = useState(null);

  const loadStats = useCallback(async () => {
    try {
      const res = await getAdminStats();
      setStats(res.data);
    } catch (err) {
      setError(extractError(err, 'Could not load statistics.'));
    }
  }, []);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    if (tab === 'ANALYTICS') {
      setLoading(false);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError('');

    getAdminProjects(tab)
      .then((res) => {
        if (!cancelled) setProjects(res.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractError(err, 'Could not load projects.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [tab]);

  async function changeStatus(id, status) {
    setBusyId(id);
    setError('');
    try {
      await updateProjectStatus(id, status);
      setProjects((list) => list.filter((p) => p.id !== id));
      setSuccess(`Project ${status.toLowerCase()}.`);
      loadStats();
    } catch (err) {
      setError(extractError(err, 'Could not update the project status.'));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this project permanently?')) return;
    setBusyId(id);
    setError('');
    try {
      await adminDeleteProject(id);
      setProjects((list) => list.filter((p) => p.id !== id));
      setSuccess('Project deleted.');
      loadStats();
    } catch (err) {
      setError(extractError(err, 'Could not delete the project.'));
    } finally {
      setBusyId(null);
    }
  }

  const visible = projects.filter(
    (p) =>
      !search ||
      p.title.toLowerCase().includes(search.toLowerCase()) ||
      p.department.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div>
      <div className="admin-banner">
        <div className="container">
          <span className="badge-admin">ADMIN</span>
          <h1>Admin Dashboard</h1>
          <p>ACCET ProjectHub · Content moderation</p>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />
        <Alert type="success" message={success} onClose={() => setSuccess('')} />

        <div className="admin-stats">
          <StatCard icon="📁" label="Total Projects" value={stats?.totalProjects ?? '—'} />
          <StatCard icon="⏳" label="Pending Review" value={stats?.pending ?? '—'} />
          <StatCard icon="✅" label="Approved" value={stats?.approved ?? '—'} />
          <StatCard icon="👥" label="Registered Users" value={stats?.totalStudents ?? '—'} />
        </div>

        <div className="admin-layout">
          <aside className="admin-side">
            <div className="admin-tabs">
              {TABS.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className={tab === t.id ? 'admin-tab active' : 'admin-tab'}
                  onClick={() => setTab(t.id)}
                >
                  <span>{t.label}</span>
                  {t.id !== 'ANALYTICS' && stats && (
                    <span className="count-pill">
                      {t.id === 'PENDING'
                        ? stats.pending
                        : t.id === 'APPROVED'
                          ? stats.approved
                          : stats.rejected}
                    </span>
                  )}
                </button>
              ))}
            </div>

            {stats && (
              <div className="panel">
                <h3>By department</h3>
                <ul className="bar-list">
                  {Object.entries(stats.byDepartment).map(([dept, count]) => (
                    <li key={dept}>
                      <span>{dept}</span>
                      <span className="bar" style={{ width: `${Math.min(count * 18, 80)}px` }} />
                      <span className="mono">{count}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </aside>

          <div className="admin-main">
            {tab === 'ANALYTICS' ? (
              <AnalyticsPanel stats={stats} />
            ) : loading ? (
              <Loader label="Loading projects…" />
            ) : (
              <>
                <input
                  type="text"
                  className="admin-search"
                  placeholder="Search by title or department…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />

                {visible.length === 0 ? (
                  <div className="empty-state panel">
                    <div className="empty-icon">📭</div>
                    <p>No {tab.toLowerCase()} projects</p>
                  </div>
                ) : (
                  <div className="admin-list">
                    {visible.map((p) => (
                      <div key={p.id} className="admin-row">
                        <img src={imageUrl(p.image, 160, 100)} alt={p.title} />
                        <div className="admin-row-body">
                          <h3>
                            <Link to={`/projects/${p.id}`}>{p.title}</Link>
                          </h3>
                          <p className="muted small">
                            {p.department} · {p.category} · By {p.submittedBy} · {p.submittedAt}
                          </p>
                          <p className="clamp-2 small">{p.description}</p>
                        </div>
                        <div className="admin-row-actions">
                          {p.status !== 'APPROVED' && (
                            <button
                              type="button"
                              className="btn btn-success btn-sm"
                              disabled={busyId === p.id}
                              onClick={() => changeStatus(p.id, 'APPROVED')}
                            >
                              Approve
                            </button>
                          )}
                          {p.status !== 'REJECTED' && (
                            <button
                              type="button"
                              className="btn btn-warning btn-sm"
                              disabled={busyId === p.id}
                              onClick={() => changeStatus(p.id, 'REJECTED')}
                            >
                              {p.status === 'APPROVED' ? 'Revoke' : 'Reject'}
                            </button>
                          )}
                          <button
                            type="button"
                            className="btn btn-danger-outline btn-sm"
                            disabled={busyId === p.id}
                            onClick={() => handleDelete(p.id)}
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value }) {
  return (
    <div className="stat-card">
      <span className="stat-icon">{icon}</span>
      <strong>{value}</strong>
      <span className="muted small">{label}</span>
    </div>
  );
}

function AnalyticsPanel({ stats }) {
  if (!stats) return <Loader label="Loading analytics…" />;

  const totalApproved = stats.approved || 1;

  return (
    <div className="analytics">
      <div className="panel">
        <h2>Projects by category</h2>
        {Object.keys(stats.byCategory).length === 0 ? (
          <p className="muted">No approved projects yet.</p>
        ) : (
          Object.entries(stats.byCategory).map(([cat, count]) => {
            const pct = Math.round((count / totalApproved) * 100);
            return (
              <div key={cat} className="progress-row">
                <div className="progress-head">
                  <span>{cat}</span>
                  <span className="mono muted">
                    {count} ({pct}%)
                  </span>
                </div>
                <div className="progress-track">
                  <div className="progress-fill" style={{ width: `${pct}%` }} />
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="panel">
        <h2>Recent activity</h2>
        <ul className="activity-list">
          {stats.recentActivity.map((p) => (
            <li key={p.id}>
              <span className={`dot dot-${p.status.toLowerCase()}`} />
              <div>
                <Link to={`/projects/${p.id}`}>{p.title}</Link>
                <small className="muted">
                  {p.department} · {p.submittedBy}
                </small>
              </div>
              <small className="muted mono">{p.submittedAt}</small>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
