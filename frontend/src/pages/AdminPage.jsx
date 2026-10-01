import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  getAdminProjects,
  getSimilarProjects,
  updateProjectStatus,
  rejectProject,
  requestProjectChanges,
  adminDeleteProject,
  getAdminStats,
} from '../api/adminApi';
import { extractError } from '../api/axiosConfig';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { imageUrl } from '../constants';

const TABS = [
  { id: 'PENDING', label: '⏳ Pending' },
  { id: 'CHANGES_REQUESTED', label: '🛠️ Changes Requested' },
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
  const [requestingChangesId, setRequestingChangesId] = useState(null);
  const [changeReason, setChangeReason] = useState('');
  const [checkingSimilarityId, setCheckingSimilarityId] = useState(null);
  const [similarityWarning, setSimilarityWarning] = useState(null);
  const [rejectionProject, setRejectionProject] = useState(null);

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
    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') loadStats();
    };

    window.addEventListener('focus', loadStats);
    document.addEventListener('visibilitychange', refreshWhenVisible);
    return () => {
      window.removeEventListener('focus', loadStats);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
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

  async function submitRejection(event) {
    event.preventDefault();
    if (!rejectionProject) return;

    setBusyId(rejectionProject.id);
    setError('');
    try {
      await rejectProject(rejectionProject.id);
      setProjects((list) => list.filter((project) => project.id !== rejectionProject.id));
      setRejectionProject(null);
      setSuccess('Project rejected successfully.');
      loadStats();
    } catch (err) {
      setError(extractError(err, 'Could not reject the project.'));
    } finally {
      setBusyId(null);
    }
  }

  async function checkSimilarProjectsBeforeApproval(id) {
    setCheckingSimilarityId(id);
    setError('');
    try {
      const response = await getSimilarProjects(id);
      if (response.data.length > 0) {
        setSimilarityWarning({ projectId: id, matches: response.data });
      } else {
        await changeStatus(id, 'APPROVED');
      }
    } catch (err) {
      setError(extractError(err, 'Could not check for similar projects. Try again before approving.'));
    } finally {
      setCheckingSimilarityId(null);
    }
  }

  function approveDespiteSimilarity() {
    if (!similarityWarning) return;
    const { projectId } = similarityWarning;
    setSimilarityWarning(null);
    changeStatus(projectId, 'APPROVED');
  }

  async function submitChangeRequest(event, id) {
    event.preventDefault();
    const reason = changeReason.trim();
    if (!reason) return;

    setBusyId(id);
    setError('');
    try {
      await requestProjectChanges(id, reason);
      setProjects((list) => list.filter((project) => project.id !== id));
      setRequestingChangesId(null);
      setChangeReason('');
      setSuccess('Change request sent to the student.');
      loadStats();
    } catch (err) {
      setError(extractError(err, 'Could not request project changes.'));
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
                        : t.id === 'CHANGES_REQUESTED'
                          ? stats.changesRequested
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
                              disabled={busyId === p.id || checkingSimilarityId !== null}
                              onClick={() => checkSimilarProjectsBeforeApproval(p.id)}
                            >
                              {checkingSimilarityId === p.id ? 'Checking…' : 'Approve'}
                            </button>
                          )}
                          {p.status !== 'REJECTED' && (
                            <button
                              type="button"
                              className="btn btn-warning btn-sm"
                              disabled={busyId === p.id}
                              onClick={() => {
                                setRejectionProject(p);
                              }}
                            >
                              {p.status === 'APPROVED' ? 'Revoke' : 'Reject'}
                            </button>
                          )}
                          {p.status === 'PENDING' && (
                            <button
                              type="button"
                              className="btn btn-outline btn-sm"
                              disabled={busyId === p.id}
                              onClick={() => {
                                setRequestingChangesId(p.id);
                                setChangeReason('');
                              }}
                            >
                              Request Changes
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
                        {requestingChangesId === p.id && (
                          <form
                            className="request-changes-form"
                            onSubmit={(event) => submitChangeRequest(event, p.id)}
                          >
                            <label htmlFor={`change-reason-${p.id}`}>Reason for requested changes</label>
                            <textarea
                              id={`change-reason-${p.id}`}
                              value={changeReason}
                              onChange={(event) => setChangeReason(event.target.value)}
                              maxLength={450}
                              rows={3}
                              required
                              placeholder="Explain what the student should complete or correct."
                            />
                            <div className="request-changes-actions">
                              <button
                                type="submit"
                                className="btn btn-navy btn-sm"
                                disabled={busyId === p.id || !changeReason.trim()}
                              >
                                Send Request
                              </button>
                              <button
                                type="button"
                                className="btn btn-ghost btn-sm"
                                onClick={() => {
                                  setRequestingChangesId(null);
                                  setChangeReason('');
                                }}
                              >
                                Cancel
                              </button>
                            </div>
                          </form>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
      {rejectionProject && (
        <div className="rejection-modal-backdrop">
          <form
            className="rejection-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="rejection-modal-title"
            onSubmit={submitRejection}
          >
            <h2 id="rejection-modal-title">Confirm project rejection</h2>
            <dl className="rejection-project-details">
              <div>
                <dt>Project title</dt>
                <dd>{rejectionProject.title}</dd>
              </div>
              <div>
                <dt>Student name</dt>
                <dd>{rejectionProject.submittedBy}</dd>
              </div>
              <div>
                <dt>Department</dt>
                <dd>{rejectionProject.department}</dd>
              </div>
            </dl>
            <div className="rejection-modal-actions">
              <button
                type="button"
                className="btn btn-ghost"
                disabled={busyId === rejectionProject.id}
                onClick={() => {
                  setRejectionProject(null);
                }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-warning"
                disabled={busyId === rejectionProject.id}
              >
                Confirm Rejection
              </button>
            </div>
          </form>
        </div>
      )}
      {similarityWarning && (
        <div className="similarity-backdrop">
          <section
            className="similarity-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="similarity-dialog-title"
          >
            <div className="similarity-dialog-heading">
              <div>
                <p className="similarity-eyebrow">Manual review recommended</p>
                <h2 id="similarity-dialog-title">Similar project found</h2>
              </div>
              <button
                type="button"
                className="icon-btn"
                aria-label="Close similarity warning"
                onClick={() => setSimilarityWarning(null)}
              >
                ✕
              </button>
            </div>
            <p className="muted">
              Review these existing projects before deciding. This warning does not block approval.
            </p>
            <ul className="similarity-match-list">
              {similarityWarning.matches.map((project) => (
                <li key={project.id}>
                  <div>
                    <strong>{project.title}</strong>
                    <p className="muted small">
                      {project.department} · {project.year} · {project.status.replaceAll('_', ' ')}
                    </p>
                    <p className="similarity-fields">
                      Similar in: {project.matchingFields.join(', ')}
                    </p>
                  </div>
                  <Link className="link-navy" to={`/projects/${project.id}`}>
                    Review
                  </Link>
                </li>
              ))}
            </ul>
            <div className="similarity-dialog-actions">
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setSimilarityWarning(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-success"
                onClick={approveDespiteSimilarity}
              >
                Approve anyway
              </button>
            </div>
          </section>
        </div>
      )}
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

      <div className="panel">
        <h2>Department Project Insights</h2>
        {Object.entries(stats.departmentInsights || {}).length === 0 ? (
          <p className="muted">No uploaded projects yet.</p>
        ) : (
          <div className="department-insights-grid">
            {Object.entries(stats.departmentInsights || {}).map(([department, categories]) => {
              const totalProjects = Object.values(categories).reduce((total, count) => total + count, 0);
              const categorySummary = summarizeDepartmentCategories(categories);

              return (
                <section className="department-insight" key={department}>
                  <div className="department-insight-head">
                    <h3>{department} &ndash; Project Insights</h3>
                    <div className="department-total">
                      <span className="muted small">Total Projects</span>
                      <strong>{totalProjects}</strong>
                    </div>
                  </div>
                  <ul className="department-insight-list">
                    {Object.entries(categorySummary)
                      .filter(([, count]) => count > 0)
                      .map(([category, count]) => (
                        <li key={category}>
                          <span>{category}</span>
                          <strong>{count}</strong>
                        </li>
                      ))}
                  </ul>
                </section>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function summarizeDepartmentCategories(categories) {
  const summary = {
    'Web Development': 0,
    AI: 0,
    IoT: 0,
    Mobile: 0,
    Cybersecurity: 0,
    Others: 0,
  };

  Object.entries(categories).forEach(([category, count]) => {
    const normalized = category.toLowerCase();
    const group = normalized.includes('web development')
      ? 'Web Development'
      : normalized.includes('ai')
        ? 'AI'
        : normalized.includes('iot')
          ? 'IoT'
          : normalized.includes('mobile')
            ? 'Mobile'
            : normalized.includes('cybersecurity')
              ? 'Cybersecurity'
              : 'Others';
    summary[group] += count;
  });

  return summary;
}
