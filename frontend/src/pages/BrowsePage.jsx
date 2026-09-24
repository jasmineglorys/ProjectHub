import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { browseProjects } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import useProjectActions from '../hooks/useProjectActions';
import ProjectCard from '../components/ProjectCard';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { DEPARTMENTS, CATEGORIES } from '../constants';

const PAGE_SIZE = 12;

export default function BrowsePage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [department, setDepartment] = useState(searchParams.get('department') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [sort, setSort] = useState(searchParams.get('sort') || 'popular');
  const [page, setPage] = useState(0);

  const [projects, setProjects] = useState([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const applyChange = useCallback((id, patch) => {
    setProjects((list) => list.map((p) => (p.id === id ? { ...p, ...patch } : p)));
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  // Debounce the text search so we don't fire a request per keystroke.
  const [debouncedSearch, setDebouncedSearch] = useState(search);
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 400);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, department, category, sort]);

  useEffect(() => {
    const params = {};
    if (debouncedSearch) params.search = debouncedSearch;
    if (department) params.department = department;
    if (category) params.category = category;
    if (sort !== 'popular') params.sort = sort;
    setSearchParams(params, { replace: true });
  }, [debouncedSearch, department, category, sort, setSearchParams]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const res = await browseProjects({
          search: debouncedSearch || undefined,
          department: department || undefined,
          category: category || undefined,
          sort,
          page,
          size: PAGE_SIZE,
        });
        if (cancelled) return;
        setProjects(res.data.content);
        setTotal(res.data.totalElements);
        setTotalPages(res.data.totalPages);
      } catch (err) {
        if (!cancelled) setError(extractError(err, 'Could not load projects.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [debouncedSearch, department, category, sort, page]);

  function clearAll() {
    setSearch('');
    setDepartment('');
    setCategory('');
  }

  const hasFilters = Boolean(search || department || category);

  return (
    <div>
      <div className="page-banner">
        <div className="container">
          <h1>Browse Projects</h1>
          <p>Explore {total} approved project{total !== 1 ? 's' : ''} from ACCET students</p>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />

        <div className="filter-bar">
          <input
            type="text"
            placeholder="🔍 Search title, tech, team…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select value={department} onChange={(e) => setDepartment(e.target.value)}>
            <option value="">All Departments</option>
            {DEPARTMENTS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All Categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            <option value="popular">Most Liked</option>
            <option value="views">Most Viewed</option>
            <option value="newest">Newest First</option>
          </select>
        </div>

        {hasFilters && (
          <div className="filter-chips">
            <span className="muted small">Active filters:</span>
            {search && <Chip label={`"${search}"`} onRemove={() => setSearch('')} />}
            {department && <Chip label={department} onRemove={() => setDepartment('')} />}
            {category && <Chip label={category} onRemove={() => setCategory('')} />}
            <button type="button" className="link-danger" onClick={clearAll}>
              Clear all
            </button>
          </div>
        )}

        {loading ? (
          <Loader label="Loading projects…" />
        ) : projects.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🔍</div>
            <h3>No projects found</h3>
            <p>Try adjusting your search or filters.</p>
          </div>
        ) : (
          <>
            <p className="muted small results-count">
              {total} project{total !== 1 ? 's' : ''} found
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
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  ← Previous
                </button>
                <span className="muted small">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
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

function Chip({ label, onRemove }) {
  return (
    <span className="chip">
      {label}
      <button type="button" onClick={onRemove} aria-label={`Remove ${label}`}>
        ✕
      </button>
    </span>
  );
}
