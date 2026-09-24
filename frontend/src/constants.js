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

export const SAMPLE_IMAGES = [
  'photo-1517694712202-14dd9538aa97',
  'photo-1518770660439-4636190af475',
  'photo-1485827404703-89b55fcc595e',
  'photo-1451187580459-43490279c0fa',
  'photo-1581091226825-a6a2a5aee158',
  'photo-1486325212027-8081e485255e',
];

/** Builds an Unsplash URL from the stored photo id. */
export function imageUrl(photoId, width = 600, height = 340) {
  const id = photoId || SAMPLE_IMAGES[0];
  return `https://images.unsplash.com/${id}?w=${width}&h=${height}&fit=crop&auto=format`;
}

export const PROJECT_YEARS = [2026, 2025, 2024, 2023, 2022, 2021];
