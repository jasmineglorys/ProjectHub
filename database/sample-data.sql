-- ============================================================
-- Optional sample data.
-- The Spring Boot DataSeeder inserts this automatically on the
-- first run (app.seed.enabled=true), so use this file only if
-- you disabled the seeder or want to reload data manually.
--
-- Passwords below are real BCrypt hashes:
--   admin@accet.ac.in       -> admin@2024
--   arjun@student.accet.ac.in / priya@... -> student123
-- ============================================================

USE projecthub_db;

INSERT INTO users (name, email, roll_no, department, study_year, password_hash, avatar, role, created_at)
VALUES
('ProjectHub Admin', 'admin@accet.ac.in', 'ADMIN001', 'Other', 4,
 '$2b$10$VH0oM1FeR9N3eX6fjIqZfeNgVBWgUzcqCkQfhtK4Afa1HBb04jzP6',
 'photo-1560250097-0b93528c311a', 'ADMIN', NOW(6)),
('Arjun Selvam', 'arjun@student.accet.ac.in', '20CSE001', 'CSE', 4,
 '$2b$10$yI0Rl6tbIdU7dqeIKijS8ekDuPm54.DVVj6pKGXv/.1nW67l9xT2a',
 'photo-1507003211169-0a1dd7228f2d', 'STUDENT', NOW(6)),
('Priya Rajan', 'priya@student.accet.ac.in', '20CSE002', 'CSE', 4,
 '$2b$10$yI0Rl6tbIdU7dqeIKijS8ekDuPm54.DVVj6pKGXv/.1nW67l9xT2a',
 'photo-1494790108377-be9c29b29330', 'STUDENT', NOW(6));

INSERT INTO projects
(title, description, department, category, project_year, image, status,
 likes_count, views_count, submitted_at, updated_at, submitted_by)
VALUES
('Smart Attendance System using Face Recognition',
 'An automated attendance system that uses deep learning-based face recognition to mark student attendance in real time. Integrated with college ERP for seamless data sync.',
 'CSE', 'AI & ML', 2024, 'photo-1587825140708-dfaf72ae4b04', 'APPROVED',
 87, 412, '2024-03-15', NOW(6),
 (SELECT id FROM users WHERE email = 'arjun@student.accet.ac.in')),
('IoT-Based Smart Agriculture Monitoring System',
 'A sensor-driven agricultural monitoring platform using ESP32, soil moisture, pH, and temperature sensors, streaming to a cloud dashboard for automated irrigation control.',
 'ECE', 'IoT', 2024, 'photo-1560493676-04071c5f467b', 'APPROVED',
 64, 298, '2024-02-20', NOW(6),
 (SELECT id FROM users WHERE email = 'priya@student.accet.ac.in')),
('Student Grievance Management Portal',
 'A secure web portal that streamlines student complaint registration, tracking, and resolution with department-wise routing, email notifications, and SLA tracking.',
 'CSE', 'Web Development', 2024, 'photo-1551288049-bebda4e38f71', 'APPROVED',
 29, 143, '2024-04-02', NOW(6),
 (SELECT id FROM users WHERE email = 'priya@student.accet.ac.in'));

INSERT INTO project_technologies (project_id, technology) VALUES
(1, 'Python'), (1, 'OpenCV'), (1, 'TensorFlow'), (1, 'Flask'), (1, 'MySQL'),
(2, 'ESP32'), (2, 'MQTT'), (2, 'Node.js'), (2, 'React'), (2, 'AWS IoT'),
(3, 'React'), (3, 'Node.js'), (3, 'Express'), (3, 'MongoDB');

INSERT INTO team_members (name, roll_no, project_id) VALUES
('Arjun Selvam', '20CSE001', 1),
('Priya Rajan',  '20CSE002', 1),
('Karthik M',    '20CSE003', 1),
('Divya Lakshmi','20ECE011', 2),
('Surya Kumar',  '20ECE014', 2),
('Harini R',     '21CSE015', 3),
('Dev Krishnan', '21CSE018', 3);

INSERT INTO project_likes (user_id, project_id, created_at) VALUES
((SELECT id FROM users WHERE email = 'priya@student.accet.ac.in'), 1, NOW(6));

INSERT INTO bookmarks (user_id, project_id, created_at) VALUES
((SELECT id FROM users WHERE email = 'arjun@student.accet.ac.in'), 2, NOW(6));
