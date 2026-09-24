import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getBookmarkedProjects } from '../api/projectApi';
import { getProfileActivity } from '../api/profileApi';
import { extractError } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import useProjectActions from '../hooks/useProjectActions';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { getNotifications, markNotificationRead } from '../api/notificationApi';

export default function ProfilePage() {
  const { user } = useAuth();

  const [tab, setTab] = useState('pending');
  const [activity, setActivity] = useState({
    pending: [],
    approved: [],
    rejected: [],
    liked: [],
    comments: [],
  });
  const [saved, setSaved] = useState([]);
  const [showSaved, setShowSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notifications, setNotifications] = useState([]);

  const applyChange = useCallback((id, patch) => {
    setActivity((current) => ({
      ...current,
      pending: current.pending.map((p) => (p.id === id ? { ...p, ...patch } : p)),
      approved: current.approved.map((p) => (p.id === id ? { ...p, ...patch } : p)),
      rejected: current.rejected.map((p) => (p.id === id ? { ...p, ...patch } : p)),
      liked: current.liked.map((p) => (p.id === id ? { ...p, ...patch } : p)),
    }));
    setSaved((list) => {
      const updated = list.map((p) => (p.id === id ? { ...p, ...patch } : p));
      // Drop a project from the saved tab once it is un-bookmarked.
      return patch.bookmarkedByMe === false ? updated.filter((p) => p.id !== id) : updated;
    });
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const [activityResponse, bookmarks, notificationResult] = await Promise.all([
          getProfileActivity(),
          getBookmarkedProjects(),
          getNotifications().catch(() => ({ data: [] })),
        ]);
        if (cancelled) return;
        setActivity(activityResponse.data);
        setSaved(bookmarks.data);
        setNotifications(notificationResult.data);
      } catch (err) {
        if (!cancelled) setError(extractError(err, 'Could not load your profile.'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const myProjects = [...activity.pending, ...activity.approved, ...activity.rejected];
  const totalLikes = myProjects.reduce((sum, p) => sum + (p.likes || 0), 0);
  const displayed = activity[tab] || [];
  const tabs = [
    ['pending', 'Pending'],
    ['approved', 'Approved'],
    ['rejected', 'Rejected'],
    ['liked', 'Liked'],
    ['comments', 'My Comments'],
  ];

  return (
    <div>
      <div className="page-banner">
        <div className="container profile-head">
          <span className="avatar-initial xl">{user.name.charAt(0).toUpperCase()}</span>
          <div className="profile-info">
            <h1>{user.name}</h1>
            <div className="profile-meta">
              <span>🎓 {user.department}</span>
              <span>· Year {user.year}</span>
              <span className="mono">· {user.rollNo}</span>
              <span>· {user.email}</span>
            </div>
            <div className="profile-stats">
              <ProfileStat value={myProjects.length} label="Projects" />
              <ProfileStat value={totalLikes} label="Total Likes" />
              <ProfileStat value={saved.length} label="Saved" />
            </div>
          </div>
          <Link to="/submit" className="btn btn-gold">
            + Submit Project
          </Link>
        </div>
      </div>

      <div className="container section">
        <Alert message={error} onClose={() => setError('')} />

        <div className="profile-activity-tabs">
          {tabs.map(([value, label]) => (
            <button
              type="button"
              key={value}
              className={tab === value && !showSaved ? 'activity-tab active' : 'activity-tab'}
              onClick={() => {
                setTab(value);
                setShowSaved(false);
              }}
            >
              <span>{label}</span>
              <strong>{activity[value].length}</strong>
            </button>
          ))}
        </div>

        <div className="saved-toggle-row">
          <button
            type="button"
            className="link-navy"
            onClick={() => setShowSaved((visible) => !visible)}
          >
            {showSaved ? 'Back to activity' : `Saved Projects (${saved.length})`}
          </button>
        </div>

        {loading ? (
          <Loader label="Loading your profile activity…" />
        ) : showSaved ? (
          <ActivityProjectList
            projects={saved}
            emptyTitle="No Saved Projects"
            emptyText="You haven't saved any projects yet."
            onLike={like}
            onBookmark={bookmark}
            showStatus={false}
          />
        ) : tab === 'comments' ? (
          <CommentActivityList comments={activity.comments} />
        ) : displayed.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📂</div>
            <h3>No {tabs.find(([value]) => value === tab)?.[1]} Projects</h3>
            <p>{emptyActivityText(tab)}</p>
            <Link to={tab === 'liked' ? '/browse' : '/submit'} className="btn btn-navy">
              {tab === 'liked' ? 'Browse Projects' : 'Submit a Project'}
            </Link>
          </div>
        ) : (
          <ActivityProjectList
            projects={displayed}
            tab={tab}
            onLike={like}
            onBookmark={bookmark}
          />
        )}

        {notifications.length > 0 && (
          <section className="panel notification-panel">
            <div className="section-heading-row">
              <div>
                <h2>Notifications</h2>
                <p className="muted small">Updates about your projects and comments.</p>
              </div>
              <span className="badge-muted">
                {notifications.filter((notification) => !notification.read).length} unread
              </span>
            </div>
            <div className="notification-list">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={notification.read ? 'notification-item' : 'notification-item unread'}
                >
                  <div>
                    <strong>{notification.message}</strong>
                    <small className="muted">{formatNotificationDate(notification.createdAt)}</small>
                  </div>
                  <div className="notification-actions">
                    {notification.projectId && (
                      <Link to={`/projects/${notification.projectId}`} className="link-navy">
                        View project
                      </Link>
                    )}
                    {!notification.read && (
                      <button
                        type="button"
                        className="link-navy"
                        onClick={async () => {
                          try {
                            await markNotificationRead(notification.id);
                            setNotifications((items) =>
                              items.map((item) =>
                                item.id === notification.id ? { ...item, read: true } : item,
                              ),
                            );
                          } catch (err) {
                            setError(extractError(err, 'Could not mark notification as read.'));
                          }
                        }}
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

function ProfileStat({ value, label }) {
  return (
    <div className="profile-stat">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function formatNotificationDate(value) {
  return new Date(value).toLocaleString();
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : 'Not available';
}

function emptyActivityText(tab) {
  const messages = {
    pending: "You don't have any projects waiting for approval.",
    approved: "You don't have any approved projects yet.",
    rejected: "You don't have any rejected projects.",
    liked: "You haven't liked any projects yet.",
  };
  return messages[tab];
}

function ActivityProjectList({ projects, tab, emptyTitle, emptyText, onLike, onBookmark }) {
  if (projects.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📂</div>
        <h3>{emptyTitle}</h3>
        <p>{emptyText}</p>
      </div>
    );
  }

  const dateLabel = tab === 'approved'
    ? 'Approval date'
    : tab === 'rejected'
      ? 'Rejection date'
      : 'Submission date';

  return (
    <div className="profile-activity-list">
      {projects.map((project) => (
        <article key={project.id} className="profile-activity-card">
          <div className="profile-activity-card-body">
            <div className="badge-row">
              <span className={`dept-badge dept-${project.department.toLowerCase()}`}>
                {project.department}
              </span>
              <span className="badge-muted">{project.category}</span>
              {tab && <span className={`status-badge status-${project.status.toLowerCase()}`}>{project.status}</span>}
            </div>
            <h3>{project.title}</h3>
            {tab === 'liked' && <p className="muted small">By {project.submittedBy}</p>}
            <p className="card-desc">{project.description}</p>
            <div className="tech-row">
              {project.technologies.slice(0, 4).map((technology) => (
                <span key={technology} className="tech-chip">{technology}</span>
              ))}
            </div>
            <div className="profile-activity-meta">
              <span>{dateLabel}: {formatDate(tab === 'approved' || tab === 'rejected' ? project.updatedAt || project.submittedAt : project.submittedAt)}</span>
              {(tab === 'approved' || tab === 'liked') && (
                <span>{project.likes || 0} likes · {project.commentCount || 0} comments</span>
              )}
            </div>
          </div>
          <div className="profile-activity-actions">
            <Link to={`/projects/${project.id}`} className="btn btn-navy">View Project</Link>
            {onLike && (
              <button type="button" className="icon-btn" onClick={() => onLike(project.id)} aria-label="Like project">
                {project.likedByMe ? '❤️' : '🤍'} {project.likes || 0}
              </button>
            )}
            {onBookmark && (
              <button type="button" className="icon-btn" onClick={() => onBookmark(project.id)} aria-label="Save project">
                {project.bookmarkedByMe ? '🔖' : '📑'}
              </button>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}

function CommentActivityList({ comments }) {
  if (comments.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">💬</div>
        <h3>No Comments Yet</h3>
        <p>You haven't commented on any projects yet.</p>
        <Link to="/browse" className="btn btn-navy">Browse Projects</Link>
      </div>
    );
  }

  return (
    <div className="profile-activity-list">
      {comments.map((comment) => (
        <article key={comment.id} className="profile-comment-card">
          <div>
            <h3>{comment.projectTitle}</h3>
            <p>{comment.content}</p>
            <small className="muted">{formatDate(comment.createdAt)}</small>
          </div>
          <Link to={`/projects/${comment.projectId}`} className="btn btn-navy">View Project</Link>
        </article>
      ))}
    </div>
  );
}
