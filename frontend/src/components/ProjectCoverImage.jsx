import { useEffect, useState } from 'react';
import { imageUrl } from '../constants';

const FALLBACK_IMAGE = '/images/default-project-cover.jpg';

function coverSource(value, width, height) {
  if (typeof value !== 'string' || !value.trim()) return null;
  const image = value.trim();
  try {
    const url = new URL(image);
    return ['http:', 'https:'].includes(url.protocol) ? image : null;
  } catch {
    if (image.startsWith('/') && !image.startsWith('//')) return image;
    return /^photo-[a-zA-Z0-9-]+$/.test(image) ? imageUrl(image, width, height) : null;
  }
}

export default function ProjectCoverImage({ project, className = '', width = 600, height = 340 }) {
  const image = coverSource(project?.coverImage || project?.image, width, height);
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setFailed(false);
    setLoaded(false);
  }, [image]);

  return (
    <img
      className={`project-cover-image${loaded ? '' : ' is-loading'}${className ? ` ${className}` : ''}`}
      src={failed || !image ? FALLBACK_IMAGE : image}
      alt={project?.title ? `${project.title} cover` : 'Project cover'}
      loading="lazy"
      decoding="async"
      aria-busy={!loaded}
      onLoad={() => setLoaded(true)}
      onError={() => {
        if (!failed && image) {
          setFailed(true);
          setLoaded(false);
        }
      }}
    />
  );
}