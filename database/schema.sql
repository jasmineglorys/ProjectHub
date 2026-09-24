-- ============================================================
-- ACCET ProjectHub — MySQL schema
-- Database: projecthub_db
-- ------------------------------------------------------------
-- Spring Boot creates these tables automatically because
-- spring.jpa.hibernate.ddl-auto=update. Run this file only if
-- you prefer to create the schema by hand (then set
-- ddl-auto=validate in application.properties).
-- ============================================================

CREATE DATABASE IF NOT EXISTS projecthub_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE projecthub_db;

-- ------------------------------------------------------------
-- 1. users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    roll_no       VARCHAR(30)  DEFAULT NULL,
    department    VARCHAR(30)  DEFAULT NULL,
    study_year    INT          DEFAULT NULL,
    password_hash VARCHAR(100) NOT NULL,          -- BCrypt hash, never plain text
    avatar        VARCHAR(120) DEFAULT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email   UNIQUE (email),
    CONSTRAINT uk_users_roll_no UNIQUE (roll_no)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 2. projects
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(4000) NOT NULL,
    deploy_link  VARCHAR(500)  DEFAULT NULL,
    department   VARCHAR(40)   NOT NULL,
    category     VARCHAR(40)   NOT NULL,
    project_year INT           NOT NULL,
    image        VARCHAR(200)  DEFAULT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    likes_count  INT           NOT NULL DEFAULT 0,
    views_count  INT           NOT NULL DEFAULT 0,
    submitted_at DATE          NOT NULL,
    updated_at   DATETIME(6)   DEFAULT NULL,
    submitted_by BIGINT        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_projects_status     (status),
    KEY idx_projects_department (department),
    KEY idx_projects_category   (category),
    CONSTRAINT fk_projects_user FOREIGN KEY (submitted_by)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 3. project_technologies  (one row per technology)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_technologies (
    project_id BIGINT      NOT NULL,
    technology VARCHAR(60) NOT NULL,
    KEY idx_tech_project (project_id),
    CONSTRAINT fk_tech_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 4. team_members
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS team_members (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    roll_no    VARCHAR(30)  DEFAULT NULL,
    project_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_team_project (project_id),
    CONSTRAINT fk_team_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 5. project_likes  (a user may like a project once)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_likes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    project_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_like_user_project UNIQUE (user_id, project_id),
    CONSTRAINT fk_like_user    FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_like_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 6. bookmarks  (a user may save a project once)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookmarks (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    project_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookmark_user_project UNIQUE (user_id, project_id),
    CONSTRAINT fk_bookmark_user    FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 7. project_comments
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    content    VARCHAR(1000) NOT NULL,
    project_id BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comments_project (project_id),
    CONSTRAINT fk_comment_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 8. notifications
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    type         VARCHAR(30)   NOT NULL,
    message      VARCHAR(500)  NOT NULL,
    recipient_id BIGINT        NOT NULL,
    project_id   BIGINT        DEFAULT NULL,
    read_at      DATETIME(6)   DEFAULT NULL,
    created_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient (recipient_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
) ENGINE = InnoDB;
