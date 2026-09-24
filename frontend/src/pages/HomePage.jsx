import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { browseProjects, getDepartmentStats } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import useProjectActions from '../hooks/useProjectActions';
import ProjectCard from '../components/ProjectCard';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { DEPT_META } from '../constants';

export default function HomePage() {
  const { isAuthenticated } = useAuth();
  const [featured, setFeatured] = useState([]);
  const [deptStats, setDeptStats] = useState([]);
  const [totalProjects, setTotalProjects] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const applyChange = useCallback((id, patch) => {
    setFeatured((list) => list.map((p) => (p.id === id ? { ...p, ...patch } : p)));
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const [projectsRes, statsRes] = await Promise.all([
          browseProjects({ sort: 'popular', page: 0, size: 6 }),
          getDepartmentStats(),
        ]);
        if (cancelled) return;
        setFeatured(projectsRes.data.content);
        setTotalProjects(projectsRes.data.totalElements);
        setDeptStats(statsRes.data);
      } catch (err) {
        if (!cancelled) setError(extractError(err, 'Could not load the homepage.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const activeDepartments = deptStats.filter((d) => d.projectCount > 0).length;
  const totalTech = new Set(featured.flatMap((p) => p.technologies)).size;

  return (
    <div>
      <section className="hero">
        <div className="container hero-inner">
          <span className="hero-pill">
            🎓 Alagappa Chettiar Government College of Engineering &amp; Technology
          </span>
          <h1>
            Discover Student <span className="gold">Innovation</span>
            <br />
            at ACCET
          </h1>
          <p>
            A central platform for ACCET students to share, discover, and collaborate on
            engineering projects across all departments. Browse real projects built by your peers.
          </p>
          <div className="hero-buttons">
            <Link to="/browse" className="btn btn-gold">
              Browse Projects →
            </Link>
            {isAuthenticated ? (
              <Link to="/submit" className="btn btn-outline-light">
                + Submit Your Project
              </Link>
            ) : (
              <Link to="/login" className="btn btn-outline-light">
                Student Login
              </Link>
            )}
          </div>
        </div>
      </section>

      <section className="container stats-strip">
        <Stat value={totalProjects} label="Approved Projects" />
        <Stat value={activeDepartments} label="Active Departments" />
        <Stat value={deptStats.length} label="Departments Listed" />
        <Stat value={totalTech} label="Technologies in Use" />
      </section>

      <section className="container section">
        <Alert message={error} onClose={() => setError('')} />
        <div className="section-head">
          <div>
            <h2>Featured Projects</h2>
            <p className="muted">The most liked work from across the campus.</p>
          </div>
          <Link to="/browse" className="link-gold">
            View all →
          </Link>
        </div>

        {loading ? (
          <Loader label="Loading featured projects…" />
        ) : featured.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📂</div>
            <h3>No approved projects yet</h3>
            <p>Once the admin approves submissions they will appear here.</p>
          </div>
        ) : (
          <div className="card-grid">
            {featured.map((project) => (
              <ProjectCard
                key={project.id}
                project={project}
                onLike={like}
                onBookmark={bookmark}
              />
            ))}
          </div>
        )}
      </section>

      <section className="container section">
        <div className="section-head">
          <div>
            <h2>Browse by Department</h2>
            <p className="muted">Every branch, one place.</p>
          </div>
        </div>
        <div className="dept-grid">
          {deptStats.map((d) => (
            <Link key={d.department} to={`/browse?department=${d.department}`} className="dept-card">
              <span className="dept-icon">{DEPT_META[d.department]?.icon || '🔬'}</span>
              <h3>{d.department}</h3>
              <p>{DEPT_META[d.department]?.desc}</p>
              <span className="dept-count">{d.projectCount} projects</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}

function Stat({ value, label }) {
  return (
    <div className="stat-box">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}
