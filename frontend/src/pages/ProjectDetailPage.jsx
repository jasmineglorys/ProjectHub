import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {
  addProjectComment,
  deleteProject,
  getProject,
  getProjectFile,
  getProjectComments,
} from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import useProjectActions from '../hooks/useProjectActions';
import Loader from '../components/Loader';
import Alert from '../components/Alert';
import { imageUrl } from '../constants';

export default function ProjectDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentText, setCommentText] = useState('');
  const [commenting, setCommenting] = useState(false);
  const [downloadingFileId, setDownloadingFileId] = useState(null);
  const [openingFileId, setOpeningFileId] = useState(null);
  const [preview, setPreview] = useState(null);

  const applyChange = useCallback((_projectId, patch) => {
    setProject((prev) => (prev ? { ...prev, ...patch } : prev));
  }, []);

  const { like, bookmark } = useProjectActions(applyChange, setError);

  useEffect(() => () => {
    if (preview?.url) URL.revokeObjectURL(preview.url);
  }, [preview]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');

    getProject(id)
      .then(async (res) => {
        if (cancelled) return;
        setProject(res.data);
        try {
          const commentsResponse = await getProjectComments(id);
          if (!cancelled) setComments(commentsResponse.data);
        } catch {
          if (!cancelled) setComments([]);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(extractError(err, 'This project could not be loaded.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleCommentSubmit(event) {
    event.preventDefault();
    if (!commentText.trim()) return;

    setCommenting(true);
    setError('');
    try {
      const response = await addProjectComment(id, { content: commentText.trim() });
      setComments((current) => [...current, response.data]);
      setCommentText('');
    } catch (err) {
      setError(extractError(err, 'Could not add your comment.'));
    } finally {
      setCommenting(false);
    }
  }

  async function handleFileDownload(file) {
    setDownloadingFileId(file.id);
    setError('');
    try {
      const response = await getProjectFile(project.id, file.id);
      const objectUrl = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = file.fileName || 'project-file';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
    } catch (err) {
      setError(extractError(err, `Could not download ${file.fileName || 'this file'}.`));
    } finally {
      setDownloadingFileId(null);
    }
  }

  async function handleFileOpen(file) {
    setOpeningFileId(file.id);
    setError('');
    try {
      const response = await getProjectFile(project.id, file.id);
      const contentType = getPreviewContentType(file);
      const previewFile = { ...file, contentType };
      if (isNativePreviewType(contentType)) {
        if (isTextPreviewType(contentType)) {
          setPreview({ file: previewFile, text: await response.data.text() });
        } else {
          setPreview({ file: previewFile, url: URL.createObjectURL(response.data) });
        }
      } else {
        const buffer = await response.data.arrayBuffer();
        try {
          const { OfficeParser } = await import('officeparser');
          const extension = getFileExtension(file.fileName);
          const fileType = OFFICE_FILE_TYPES.has(extension) ? extension : undefined;
          const document = await OfficeParser.parseOffice(buffer, { fileType });
          const html = await document.to('html');
          setPreview({ file: previewFile, html: html.value });
        } catch {
          setPreview({ file: previewFile, rawPreview: formatBinaryPreview(buffer) });
        }
      }
    } catch (err) {
      setError(extractError(err, `Could not open ${file.fileName || 'this file'}.`));
    } finally {
      setOpeningFileId(null);
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this project permanently?')) return;
    setDeleting(true);
    try {
      await deleteProject(id);
      navigate('/profile');
    } catch (err) {
      setError(extractError(err, 'Could not delete the project.'));
      setDeleting(false);
    }
  }

  function handleShare() {
    const url = window.location.href;
    if (navigator.share) {
      navigator.share({ title: project.title, url }).catch(() => {});
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(url);
      setSuccess('Link copied to clipboard.');
    }
  }

  if (loading) return <Loader label="Loading project…" />;

  if (error && !project) {
    return (
      <div className="page-narrow empty-state">
        <div className="empty-icon">⚠️</div>
        <h2>Project unavailable</h2>
        <p>{error}</p>
        <Link to="/browse" className="btn btn-navy">
          Back to Browse
        </Link>
      </div>
    );
  }

  return (
    <div>
      <div className="detail-hero">
        <img src={imageUrl(project.image, 1400, 600)} alt={project.title} />
        <div className="detail-hero-overlay" />
        <button type="button" className="back-btn" onClick={() => navigate(-1)}>
          ← Back
        </button>
        <div className="detail-hero-text container">
          <div className="badge-row">
            <span className={`dept-badge dept-${project.department.toLowerCase()}`}>
              {project.department}
            </span>
            <span className="badge-light">{project.category}</span>
            <span className="badge-light">{project.year}</span>
            {project.academicYear && <span className="badge-light">{project.academicYear}</span>}
            {project.winner && <span className="badge-light">Award winner</span>}
            {project.status !== 'APPROVED' && (
              <span className={`status-badge status-${project.status.toLowerCase()}`}>
                {project.status}
              </span>
            )}
          </div>
          <h1>{project.title}</h1>
        </div>
      </div>

      <div className="container section detail-layout">
        <div className="detail-main">
          <Alert message={error} onClose={() => setError('')} />
          <Alert type="success" message={success} onClose={() => setSuccess('')} />

          <section className="panel">
            <h2>About this project</h2>
            <p className="detail-description">{project.description}</p>
            {project.deployLink && (
              <a className="btn btn-navy" href={project.deployLink} target="_blank" rel="noreferrer">
                Open deployed project
              </a>
            )}
          </section>

          <section className="panel">
            <h2>Technologies used</h2>
            <div className="tech-row">
              {project.technologies.map((tech) => (
                <span key={tech} className="tech-chip">
                  {tech}
                </span>
              ))}
            </div>
          </section>

          <section className="panel">
            <h2>Team members</h2>
            <ul className="member-list">
              {project.teamMembers.map((m, index) => (
                <li key={`${m.name}-${index}`}>
                  <span className="avatar-initial">{m.name.charAt(0).toUpperCase()}</span>
                  <div>
                    <strong>{m.name}</strong>
                    {m.rollNo && <small className="mono">{m.rollNo}</small>}
                  </div>
                </li>
              ))}
            </ul>
          </section>

          {project.files?.length > 0 && (
            <section className="panel">
              <h2>Project files</h2>
              <ul className="project-file-list">
                {project.files.map((file) => (
                  <li key={file.id} className="project-file-item">
                    <div className="project-file-meta">
                      <strong>{file.fileName}</strong>
                      <small className="muted">
                        {file.fileType === 'CERTIFICATE'
                          ? 'Award certificate'
                          : file.fileType === 'SUPPORTING'
                            ? 'Supporting file'
                            : 'Media file'}
                        {' · '}{formatFileSize(file.fileSize)}
                      </small>
                    </div>
                    <div className="project-file-actions">
                      <button
                        type="button"
                        className="btn btn-navy"
                        onClick={() => handleFileOpen(file)}
                        disabled={openingFileId !== null || downloadingFileId !== null}
                      >
                        {openingFileId === file.id ? 'Opening…' : 'Open'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline"
                        onClick={() => handleFileDownload(file)}
                        disabled={openingFileId !== null || downloadingFileId !== null}
                      >
                        {downloadingFileId === file.id ? 'Downloading…' : 'Download'}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
              {preview && (
                <div className="project-file-preview">
                  <div className="project-file-preview-head">
                    <h3>{preview.file.fileName}</h3>
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={() => setPreview(null)}
                    >
                      Close preview
                    </button>
                  </div>
                  <FilePreview preview={preview} />
                </div>
              )}
            </section>
          )}

          <section className="panel">
            <div className="section-heading-row">
              <div>
                <h2>Comments</h2>
                <p className="muted small">Share feedback or ask a question about this project.</p>
              </div>
              <span className="badge-muted">{comments.length}</span>
            </div>

            {comments.length === 0 ? (
              <p className="muted">No comments yet.</p>
            ) : (
              <div className="comment-list">
                {comments.map((comment) => (
                  <article key={comment.id} className="comment-item">
                    <span className="avatar-initial">{comment.authorName.charAt(0).toUpperCase()}</span>
                    <div>
                      <strong>{comment.authorName}</strong>
                      <p>{comment.content}</p>
                      <small className="muted">{formatDate(comment.createdAt)}</small>
                    </div>
                  </article>
                ))}
              </div>
            )}

            {user ? (
              <form className="comment-form" onSubmit={handleCommentSubmit}>
                <textarea
                  rows={3}
                  value={commentText}
                  onChange={(event) => setCommentText(event.target.value)}
                  placeholder="Write a comment..."
                  maxLength={1000}
                />
                <button type="submit" className="btn btn-navy" disabled={commenting || !commentText.trim()}>
                  {commenting ? 'Posting…' : 'Post comment'}
                </button>
              </form>
            ) : (
              <p className="muted">
                <Link to="/login" className="link-navy">Sign in</Link> to leave a comment.
              </p>
            )}
          </section>
        </div>

        <aside className="detail-side">
          <div className="panel">
            <div className="side-stats">
              <div>
                <strong>{project.likes}</strong>
                <span>Likes</span>
              </div>
              <div>
                <strong>{project.views}</strong>
                <span>Views</span>
              </div>
            </div>

            <button
              type="button"
              className={project.likedByMe ? 'btn btn-navy full' : 'btn btn-outline full'}
              onClick={() => like(project.id)}
            >
              {project.likedByMe ? '❤️ Liked' : '🤍 Like this project'}
            </button>
            <button
              type="button"
              className={project.bookmarkedByMe ? 'btn btn-gold full' : 'btn btn-outline full'}
              onClick={() => bookmark(project.id)}
            >
              {project.bookmarkedByMe ? '🔖 Saved' : '📑 Save for later'}
            </button>
            <button type="button" className="btn btn-ghost-dark full" onClick={handleShare}>
              🔗 Share
            </button>
          </div>

          <div className="panel">
            <h3>Submission</h3>
            <dl className="meta-list">
              <div>
                <dt>Submitted by</dt>
                <dd>{project.submittedBy}</dd>
              </div>
              <div>
                <dt>Submitted on</dt>
                <dd>{project.submittedAt}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{project.status}</dd>
              </div>
            </dl>

            {isAdmin && (
              <button
                type="button"
                className="btn btn-danger full"
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? 'Deleting…' : 'Delete project'}
              </button>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

function formatDate(value) {
  return new Date(value).toLocaleString();
}

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return 'Size unavailable';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const OFFICE_FILE_TYPES = new Set([
  'docx', 'pptx', 'xlsx', 'odt', 'odp', 'ods', 'odg', 'rtf', 'csv', 'md', 'html', 'epub',
]);

const EXTENSION_MIME_TYPES = {
  pdf: 'application/pdf',
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  webp: 'image/webp',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
  mp4: 'video/mp4',
  webm: 'video/webm',
  mov: 'video/quicktime',
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  ogg: 'audio/ogg',
  txt: 'text/plain',
  log: 'text/plain',
  json: 'application/json',
  xml: 'application/xml',
};

function getFileExtension(fileName = '') {
  return fileName.split('.').pop()?.toLowerCase() || '';
}

function getPreviewContentType(file) {
  const contentType = (file.contentType || '').toLowerCase();
  if (contentType && contentType !== 'application/octet-stream') return contentType;
  return EXTENSION_MIME_TYPES[getFileExtension(file.fileName)] || contentType;
}

function isNativePreviewType(contentType) {
  return contentType.startsWith('image/')
    || contentType.startsWith('video/')
    || contentType.startsWith('audio/')
    || contentType === 'application/pdf'
    || contentType === 'text/plain'
    || contentType === 'application/json'
    || contentType === 'application/xml';
}

function isTextPreviewType(contentType) {
  return contentType === 'text/plain'
    || contentType === 'application/json'
    || contentType === 'application/xml';
}

function formatBinaryPreview(buffer) {
  const bytes = new Uint8Array(buffer).subarray(0, 1024);
  const rows = [];
  for (let offset = 0; offset < bytes.length; offset += 16) {
    const row = bytes.subarray(offset, offset + 16);
    const hex = Array.from(row, (byte) => byte.toString(16).padStart(2, '0')).join(' ');
    const text = Array.from(row, (byte) => (byte >= 32 && byte <= 126 ? String.fromCharCode(byte) : '.'))
      .join('');
    rows.push(`${offset.toString(16).padStart(4, '0')}  ${hex.padEnd(47)}  ${text}`);
  }
  return `${rows.join('\n')}\n\nShowing the first ${bytes.length} bytes of the file.`;
}

function FilePreview({ preview }) {
  const { file, url, html, rawPreview } = preview;
  const contentType = file.contentType || '';

  if (contentType.startsWith('image/')) {
    return <img className="project-file-preview-image" src={url} alt={file.fileName} />;
  }
  if (contentType.startsWith('video/')) {
    return <video className="project-file-preview-media" src={url} controls />;
  }
  if (contentType.startsWith('audio/')) {
    return <audio className="project-file-preview-media" src={url} controls />;
  }
  if (contentType === 'application/pdf') {
    return <iframe className="project-file-preview-frame" src={url} title={`Preview: ${file.fileName}`} />;
  }
  if (preview.text !== undefined) {
    return <pre className="project-file-preview-text">{preview.text}</pre>;
  }
  if (html) {
    return (
      <iframe
        className="project-file-preview-frame"
        srcDoc={html}
        sandbox=""
        title={`Preview: ${file.fileName}`}
      />
    );
  }
  return <pre className="project-file-preview-text">{rawPreview}</pre>;
}
