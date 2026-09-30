import { useCallback, useEffect, useState } from 'react';
import { getWinningProjects } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import useProjectActions from '../hooks/useProjectActions';
import ProjectCard from '../components/ProjectCard';
import Loader from '../components/Loader';
import Alert from '../components/Alert';

export default function WinningProjectsPage() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const applyChange = useCallback((id, patch) => {
    setProjects((current) => current.map((project) =>
      project.id === id ? { ...project, ...patch } : project));
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  useEffect(() => {
    let cancelled = false;

    getWinningProjects()
      .then((response) => {
        if (!cancelled) setProjects(response.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractError(err, 'Could not load winning projects.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <div className="page-banner">
        <div className="container">
          <h1>Winning Projects</h1>
          <p>Projects recognized with an award certificate</p>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />
        {loading ? (
          <Loader label="Loading winning projects…" />
        ) : projects.length === 0 ? (
          <div className="empty-state">
            <h2>No winning projects yet</h2>
            <p>Approved award-winning projects will appear here.</p>
          </div>
        ) : (
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
        )}
      </div>
    </div>
  );
}