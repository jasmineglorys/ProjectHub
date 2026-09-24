import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getDepartmentStats } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { DEPT_META } from '../constants';

export default function DepartmentsPage() {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    getDepartmentStats()
      .then((res) => {
        if (!cancelled) setStats(res.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractError(err, 'Could not load departments.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const totalProjects = stats.reduce((sum, d) => sum + d.projectCount, 0);

  return (
    <div>
      <div className="page-banner">
        <div className="container">
          <h1>Departments</h1>
          <p>
            {totalProjects} approved project{totalProjects !== 1 ? 's' : ''} across{' '}
            {stats.length} departments
          </p>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />
        {loading ? (
          <Loader label="Loading departments…" />
        ) : (
          <div className="dept-grid">
            {stats.map((d) => (
              <Link
                key={d.department}
                to={`/browse?department=${encodeURIComponent(d.department)}`}
                className="dept-card"
              >
                <span className="dept-icon">{DEPT_META[d.department]?.icon || '🔬'}</span>
                <h3>{d.department}</h3>
                <p>{DEPT_META[d.department]?.desc}</p>
                <span className="dept-count">
                  {d.projectCount} project{d.projectCount !== 1 ? 's' : ''}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
