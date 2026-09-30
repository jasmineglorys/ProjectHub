export const DEPARTMENTS = ['CSE', 'ECE', 'EEE', 'Mechanical', 'Civil', 'IT', 'Other'];

export const CATEGORIES = [
  'Web Development',
  'AI & ML',
  'IoT',
  'Mobile Applications',
  'Data Science',
  'Embedded Systems',
  'Robotics',
  'Cloud Computing',
  'Other',
];

export const DEPT_META = {
  CSE: {
    icon: '💻',
    desc: 'Computer Science & Engineering projects spanning software, systems, and algorithms.',
  },
  ECE: {
    icon: '📡',
    desc: 'Electronics & Communication projects in signal processing, VLSI, and wireless systems.',
  },
  EEE: {
    icon: '⚡',
    desc: 'Electrical & Electronics Engineering covering power systems, drives, and energy.',
  },
  Mechanical: {
    icon: '⚙️',
    desc: 'Mechanical Engineering projects in design, manufacturing, thermal, and fluid systems.',
  },
  Civil: {
    icon: '🏗️',
    desc: 'Civil Engineering projects in structures, transportation, geotechnics, and urban planning.',
  },
  IT: {
    icon: '🌐',
    desc: 'Information Technology projects in networks, databases, cybersecurity, and cloud.',
  },
  Other: {
    icon: '🔬',
    desc: 'Interdisciplinary and innovative projects that bridge multiple engineering domains.',
  },
};

export const DEFAULT_IMAGE = 'photo-1518770660439-4636190af475';

/** Builds an Unsplash URL from the stored photo id. */
export function imageUrl(photoId, width = 600, height = 340) {
  const id = photoId || DEFAULT_IMAGE;
  return `https://images.unsplash.com/${id}?w=${width}&h=${height}&fit=crop&auto=format`;
}

const PROJECT_COVER_RULES = [
  { image: 'photo-1677442136019-21780ecad995', keywords: ['artificial intelligence', 'machine learning', 'deep learning', 'tensorflow', 'pytorch', 'neural network', 'computer vision', 'natural language processing', 'generative ai', 'large language model', 'llm', 'ai', 'ml', 'nlp'] },
  { image: 'photo-1512941937669-90a1b58e7e9', keywords: ['mobile applications', 'mobile app', 'react native', 'android', 'flutter', 'kotlin', 'ios'] },
  { image: 'photo-1498050108023-c5249f4df085', keywords: ['web development', 'frontend', 'backend', 'full stack', 'javascript', 'react', 'angular', 'vue', 'html', 'css', 'spring boot', 'node.js'] },
  { image: 'photo-1518770660439-4636190af475', keywords: ['embedded systems', 'embedded system', 'embedded', 'microcontroller', 'microprocessor', 'accelerometer', 'vibration sensor', 'earthquake', 'seismic', 'circuit board', 'electronics', 'hardware prototype', 'hardware', '8051', 'stm32', 'pic'] },
  { image: 'photo-1581091226825-a6a2a5aee158', keywords: ['internet of things', 'smart agriculture', 'smart home', 'smart device', 'connected device', 'connected sensors', 'raspberry pi', 'esp8266', 'esp32', 'mqtt', 'arduino', 'sensor', 'iot'] },
  { image: 'photo-1563013544-824ae1b704d3', keywords: ['cybersecurity', 'cyber security', 'network security', 'ethical hacking', 'cyber attack', 'information security', 'encryption', 'firewall', 'security'] },
  { image: 'photo-1451187580459-43490279c0fa', keywords: ['cloud computing', 'google cloud', 'kubernetes', 'docker', 'aws', 'azure', 'gcp', 'cloud'] },
  { image: 'photo-1551288049-bebda4e38f71', keywords: ['data science', 'data analytics', 'visualization', 'data visualization', 'power bi', 'statistics', 'pandas', 'numpy', 'dataset', 'analytics'] },
  { image: 'photo-1544383835-bda2bc66a55d', keywords: ['database', 'postgresql', 'mongodb', 'mysql', 'database management', 'oracle', 'sql'] },
  { image: 'photo-1485827404703-89b55fcc595e', keywords: ['robotics', 'robot', 'automation'] },
  { image: 'photo-1639762681485-074b7f938ba0', keywords: ['blockchain', 'distributed ledger', 'smart contract', 'cryptocurrency', 'ethereum', 'web3'] },
];

export function resolveProjectCoverImage(project = {}) {
  const technologies = Array.isArray(project.technologies)
    ? project.technologies.join(' ')
    : project.technologies;
  for (const value of [project.category, technologies, project.title, project.description]) {
    if (!value) continue;
    const text = String(value).toLowerCase();
    const rule = PROJECT_COVER_RULES.find(({ keywords }) => keywords.some((keyword) =>
      keyword.length <= 3
        ? new RegExp(`(^|[^a-z0-9])${keyword}([^a-z0-9]|$)`).test(text)
        : text.includes(keyword)));
    if (rule) return rule.image;
  }
  return DEFAULT_IMAGE;
}

export const PROJECT_YEARS = [2026, 2025, 2024, 2023, 2022, 2021];

export const ACADEMIC_YEARS = ['2022-2026', '2023-2027', '2024-2028', '2025-2029'];

export function isValidAcademicYear(value) {
  const match = /^(\d{4})-(\d{4})$/.exec(value.trim());
  return Boolean(match && Number(match[1]) < Number(match[2]));
}
