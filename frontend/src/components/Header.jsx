import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Header() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    setMenuOpen(false);
    navigate('/');
  }

  const close = () => setMenuOpen(false);

  return (
    <header className="site-header">
      <div className="header-stripe" />
      <div className="container header-inner">
        <Link to="/" className="brand" onClick={close}>
          <img src="/logo.png" alt="ACCET logo" className="brand-logo" />
          <span className="brand-text">
            <strong>ProjectHub</strong>
            <small>ACCET · Karaikudi</small>
          </span>
        </Link>

        <button
          type="button"
          className="menu-toggle"
          onClick={() => setMenuOpen((o) => !o)}
          aria-label="Toggle navigation"
        >
          {menuOpen ? '✕' : '☰'}
        </button>

        <nav className={menuOpen ? 'main-nav open' : 'main-nav'}>
          <NavLink to="/" end onClick={close}>
            Home
          </NavLink>
          <NavLink to="/browse" onClick={close}>
            Projects
          </NavLink>
          <NavLink to="/departments" onClick={close}>
            Departments
          </NavLink>
          {isAdmin && (
            <NavLink to="/admin" onClick={close}>
              Admin
            </NavLink>
          )}

          <div className="nav-actions">
            {isAuthenticated ? (
              <>
                {!isAdmin && (
                  <Link to="/submit" className="btn btn-gold btn-sm" onClick={close}>
                    + Submit
                  </Link>
                )}
                {!isAdmin && (
                  <Link to="/profile" className="nav-user" onClick={close}>
                    <span className="avatar-initial">{user.name.charAt(0).toUpperCase()}</span>
                    <span className="nav-user-name">{user.name.split(' ')[0]}</span>
                  </Link>
                )}
                <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost btn-sm" onClick={close}>
                  Login
                </Link>
                <Link to="/register" className="btn btn-gold btn-sm" onClick={close}>
                  Register
                </Link>
              </>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
}
