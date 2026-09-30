import { Link } from 'react-router-dom';
import ProjectCoverImage from './ProjectCoverImage';

export default function ProjectCard({ project, onLike, onBookmark, showStatus = false }) {
  const liked = project.likedByMe;
  const saved = project.bookmarkedByMe;

  return (
    <article className="project-card">
      <Link to={`/projects/${project.id}`} className="card-image-link">
        <ProjectCoverImage project={project} width={600} height={320} />
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
          <Link to={`/projects/${project.id}`}>{project.title}</Link>
        </h3>
        <p className="card-desc">{project.description}</p>

        <div className="tech-row">
          {project.technologies.slice(0, 3).map((tech) => (
            <span key={tech} className="tech-chip">
              {tech}
            </span>
          ))}
          {project.technologies.length > 3 && (
            <span className="tech-chip muted">+{project.technologies.length - 3}</span>
          )}
        </div>

        <div className="card-footer">
          <span className="meta-small">
            {project.teamMembers.length} member{project.teamMembers.length !== 1 ? 's' : ''} · {project.year}
          </span>
          <span className="meta-small">{project.views} views</span>
          <div className="card-actions">
            <button
              type="button"
              className={liked ? 'icon-btn active' : 'icon-btn'}
              onClick={() => onLike?.(project.id)}
              aria-label="Like project"
            >
              {liked ? '❤️' : '🤍'} {project.likes}
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
