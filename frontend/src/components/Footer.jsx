export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-inner">
        <div>
          <strong className="font-display">ProjectHub</strong>
          <p>Alagappa Chettiar Government College of Engineering and Technology  · Karaikudi</p>
        </div>
        <p className="muted small">
          Built with React, Spring Boot and MySQL · © {new Date().getFullYear()}
        </p>
      </div>
    </footer>
  );
}
