import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { browseProjects } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import useProjectActions from '../hooks/useProjectActions';
import ProjectCard from '../components/ProjectCard';
import Loader from '../components/Loader';
import Alert from '../components/Alert';

const PAGE_SIZE = 12;

export default function WinningProjectsPage() {
  const [projects, setProjects] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const applyChange = useCallback((id, patch) => {
    setProjects((list) => list.map((project) => (
      project.id === id ? { ...project, ...patch } : project
    )));
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const response = await browseProjects({
          winning: true,
          sort: 'newest',
          page,
          size: PAGE_SIZE,
        });
        if (cancelled) return;
        setProjects(response.data.content);
        setTotal(response.data.totalElements);
        setTotalPages(response.data.totalPages);
      } catch (err) {
        if (!cancelled) setError(extractError(err, 'Could not load winning projects.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div>
      <div className="page-banner">
        <div className="container">
          <h1>Winning Projects</h1>
          <p>Student projects recognized for their achievements</p>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />
        {loading ? (
          <Loader label="Loading winning projects…" />
        ) : projects.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🏆</div>
            <h3>No winning projects yet</h3>
            <p>Approved projects with achievement details will appear here.</p>
            <Link to="/browse" className="btn btn-navy">Browse Projects</Link>
          </div>
        ) : (
          <>
            <p className="muted small results-count">
              {total} winning project{total !== 1 ? 's' : ''}
            </p>
            <div className="card-grid">
              {projects.map((project) => (
                <ProjectCard
                  key={project.id}
                  project={project}
                  onLike={like}
                  onBookmark={bookmark}
                />
              ))}
            </div>
            {totalPages > 1 && (
              <div className="pagination">
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={page === 0}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                >
                  ← Previous
                </button>
                <span className="muted small">Page {page + 1} of {totalPages}</span>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}