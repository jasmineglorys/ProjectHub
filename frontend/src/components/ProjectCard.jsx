import { Link, useLocation } from 'react-router-dom';
import { projectImageUrl } from '../constants';

export default function ProjectCard({
  project,
  onLike,
  onBookmark,
  showStatus = false,
  showAcademicInfo = false,
}) {
  const location = useLocation();
  const returnTo = `${location.pathname}${location.search}`;
  const saved = project.bookmarkedByMe;

  return (
    <article className="project-card">
      <Link to={`/projects/${project.id}`} state={{ from: returnTo }} className="card-image-link">
        <img src={projectImageUrl(project, 600, 320)} alt={project.title} loading="lazy" />
        <span className={`dept-badge dept-${project.department.toLowerCase()}`}>
          {project.department}
        </span>
        {showStatus && (
          <span className={`status-badge status-${project.status.toLowerCase()}`}>
            {project.status === 'PENDING'
              ? 'Pending review'
              : project.status === 'APPROVED'
                ? 'Approved'
                : 'Rejected'}
          </span>
        )}
      </Link>

      <div className="card-body">
        <span className="badge-muted">{project.category}</span>
        <h3>
          <Link to={`/projects/${project.id}`} state={{ from: returnTo }}>{project.title}</Link>
        </h3>
        <p className="card-desc">{project.description}</p>

        <div className="tech-row">
          {project.technologies.slice(0, 3).map((tech) => (
            <Link
              key={tech}
              className="tech-chip"
              to={`/browse?technology=${encodeURIComponent(tech)}`}
              aria-label={`Browse projects using ${tech}`}
            >
              {tech}
            </Link>
          ))}
          {project.technologies.length > 3 && (
            <span className="tech-chip muted">+{project.technologies.length - 3}</span>
          )}
        </div>

        <div className="card-footer">
          <div className="card-project-meta">
            <span className="meta-small">
              {project.teamMembers.length} member{project.teamMembers.length !== 1 ? 's' : ''} ·{' '}
              {project.year}
            </span>
            {showAcademicInfo && project.submittedAt && (
              <span className="meta-small">Uploaded {formatDate(project.submittedAt)}</span>
            )}
            {showAcademicInfo && project.academicYear && (
              <span className="meta-small">Academic year {project.academicYear}</span>
            )}
          </div>
          <div className="card-actions">
            <button
              type="button"
              className={project.likedByMe ? 'icon-btn active' : 'icon-btn'}
              onClick={() => onLike?.(project.id)}
              aria-label="Like project"
            >
              {project.likedByMe ? '❤️' : '🤍'} {project.likes}
            </button>
            <button
              type="button"
              className={saved ? 'icon-btn active' : 'icon-btn'}
              onClick={() => onBookmark?.(project.id)}
              aria-label="Save project"
            >
              {saved ? '🔖' : '📑'}
            </button>
          </div>
        </div>
      </div>

    </article>
  );
}

function formatDate(value) {
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
}
