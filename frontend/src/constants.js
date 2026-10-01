export const DEPARTMENTS = ['CSE', 'ECE', 'EEE', 'Mechanical', 'Civil', 'IT', 'Other'];

export const DEPARTMENT_DOMAINS = {
  CSE: [
    'Web Development', 'AI & ML', 'Data Science', 'Cybersecurity', 'Cloud Computing',
    'IoT', 'Mobile App Development', 'Blockchain', 'Generative AI', 'Software Engineering',
  ],
  IT: [
    'Web Development', 'Cloud Computing', 'Cybersecurity', 'Data Analytics',
    'AI Applications', 'Mobile Applications', 'E-Commerce', 'DevOps', 'Database Systems', 'IoT',
  ],
  ECE: [
    'Embedded Systems', 'IoT', 'VLSI', 'Robotics', 'Communication Systems',
    'Signal Processing', 'Computer Vision', 'Wireless Networks', 'FPGA', 'Biomedical Electronics',
  ],
  EEE: [
    'Renewable Energy', 'Power Systems', 'Electric Vehicles', 'Power Electronics', 'Smart Grid',
    'Industrial Automation', 'Embedded Systems', 'Energy Management', 'Motor Control',
    'Battery Management',
  ],
  Civil: [
    'Structural Engineering', 'Construction Management', 'Transportation Engineering',
    'Environmental Engineering', 'Geotechnical Engineering', 'Smart Cities', 'Water Resources',
    'Green Building', 'Surveying & GIS', 'Disaster Management',
  ],
  Mechanical: [
    'Robotics', 'Automation', 'CAD/CAM', 'Automotive Engineering', 'Thermal Engineering',
    'Manufacturing', 'Mechatronics', 'Renewable Energy', 'Material Science',
    'Industrial Engineering',
  ],
  Other: [],
};

export const CUSTOM_DOMAIN = '__custom_domain__';
export const CATEGORIES = [
  ...new Set([...Object.values(DEPARTMENT_DOMAINS).flat(), 'Other']),
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

export const SAMPLE_IMAGES = [
  'photo-1517694712202-14dd9538aa97',
  'photo-1518770660439-4636190af475',
  'photo-1485827404703-89b55fcc595e',
  'photo-1451187580459-43490279c0fa',
  'photo-1581091226825-a6a2a5aee158',
  'photo-1486325212027-8081e485255e',
];

const COVER_IMAGE_SETS = {
  software: [
    { id: SAMPLE_IMAGES[0], label: 'Software workspace' },
    { id: SAMPLE_IMAGES[3], label: 'Connected systems' },
    { id: SAMPLE_IMAGES[2], label: 'Intelligent technology' },
  ],
  electronics: [
    { id: SAMPLE_IMAGES[1], label: 'Electronics and circuits' },
    { id: SAMPLE_IMAGES[4], label: 'Engineering laboratory' },
    { id: SAMPLE_IMAGES[2], label: 'Robotics and automation' },
  ],
  energy: [
    { id: SAMPLE_IMAGES[4], label: 'Energy engineering' },
    { id: SAMPLE_IMAGES[3], label: 'Connected infrastructure' },
    { id: SAMPLE_IMAGES[1], label: 'Power electronics' },
  ],
  civil: [
    { id: SAMPLE_IMAGES[5], label: 'Architecture and structures' },
    { id: SAMPLE_IMAGES[4], label: 'Construction engineering' },
    { id: SAMPLE_IMAGES[3], label: 'Smart infrastructure' },
  ],
  mechanical: [
    { id: SAMPLE_IMAGES[2], label: 'Robotics and machines' },
    { id: SAMPLE_IMAGES[4], label: 'Mechanical workshop' },
    { id: SAMPLE_IMAGES[1], label: 'Manufacturing systems' },
  ],
};

export function getCoverOptions(department, domain) {
  const value = `${department} ${domain}`.toLowerCase();
  if (/\b(civil|structural|construction|transportation|geotechnical|smart cities|water resources|green building|surveying|disaster)\b/.test(value)) {
    return COVER_IMAGE_SETS.civil;
  }
  if (/\b(mechanical|robotics|automation|automotive|thermal|manufacturing|mechatronics|material science|industrial engineering|cad\/cam)\b/.test(value)) {
    return COVER_IMAGE_SETS.mechanical;
  }
  if (/\b(eee|renewable energy|power|electric vehicles|smart grid|energy|motor control|battery)\b/.test(value)) {
    return COVER_IMAGE_SETS.energy;
  }
  if (/\b(ece|embedded|iot|vlsi|communication|signal processing|wireless|fpga|biomedical|electronics)\b/.test(value)) {
    return COVER_IMAGE_SETS.electronics;
  }
  return COVER_IMAGE_SETS.software;
}

/** Builds an Unsplash URL from the stored photo id. */
export function imageUrl(photoId, width = 600, height = 340) {
  const id = photoId || SAMPLE_IMAGES[0];
  return `https://images.unsplash.com/${id}?w=${width}&h=${height}&fit=crop&auto=format`;
}

export function projectImageUrl(project, width = 600, height = 340) {
  const cover = project?.files?.find((file) => file.fileType === 'COVER');
  if (cover?.downloadUrl) {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    return new URL(cover.downloadUrl, apiBaseUrl).toString();
  }
  return imageUrl(project?.image, width, height);
}

export const PROJECT_YEARS = [2026, 2025, 2024, 2023, 2022, 2021];

export const ACADEMIC_YEARS = ['2023-2027', '2024-2028', '2025-2029', '2026-2030'];

export function isValidAcademicYear(value) {
  const match = /^(\d{4})-(\d{4})$/.exec(value.trim());
  return Boolean(match && Number(match[1]) < Number(match[2]));
}
