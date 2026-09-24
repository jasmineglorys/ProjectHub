import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="page-narrow empty-state">
      <div className="empty-icon">🧭</div>
      <h2>Page not found</h2>
      <p>The page you are looking for does not exist.</p>
      <Link to="/" className="btn btn-navy">
        Back to Home
      </Link>
    </div>
  );
}
