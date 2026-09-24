export default function Loader({ label = 'Loading…', small = false }) {
  return (
    <div className={small ? 'loader loader-small' : 'loader'}>
      <div className="spinner" />
      <span>{label}</span>
    </div>
  );
}
