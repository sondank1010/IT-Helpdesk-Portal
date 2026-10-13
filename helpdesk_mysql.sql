
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS it_support
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE it_support;

CREATE TABLE app_users (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          ENUM('USER', 'AGENT', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id   BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE login_sessions (
    id           VARCHAR(255) NOT NULL,
    user_id      BIGINT NOT NULL,
    refresh_hash VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP(6) NOT NULL,
    revoked      BIT(1) NOT NULL DEFAULT b'0',

    PRIMARY KEY (id),
    CONSTRAINT fk_login_sessions_user FOREIGN KEY (user_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tickets (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    description    TEXT NOT NULL,
    priority       ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    status         ENUM('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'NEW',
    created_by_id  BIGINT NOT NULL,
    assigned_to_id BIGINT NULL,
    category_id    BIGINT NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- updated_at duoc cap nhat boi Ticket.touch(), dung nhu entity.
    PRIMARY KEY (id),
    CONSTRAINT fk_tickets_creator FOREIGN KEY (created_by_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tickets_agent FOREIGN KEY (assigned_to_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ticket_comments (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    body       TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    -- Khoa dich cho FK ghep: attachment va comment phai cung ticket.
    CONSTRAINT uk_ticket_comments_id_ticket UNIQUE (id, ticket_id),
    CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ticket_history (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT NOT NULL,
    actor_id   BIGINT NOT NULL,
    action     VARCHAR(255) NOT NULL,
    details    TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_history_actor FOREIGN KEY (actor_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attachments (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id      BIGINT NOT NULL,
    comment_id     BIGINT NULL,
    uploaded_by_id BIGINT NOT NULL,
    original_name  VARCHAR(255) NOT NULL,
    stored_name    VARCHAR(255) NOT NULL,
    content_type   VARCHAR(255) NOT NULL,
    size           BIGINT NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT ck_attachments_size CHECK (size >= 0),
    CONSTRAINT fk_attachments_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- comment_id NULL: file gan truc tiep vao ticket.
    -- comment_id co gia tri: comment bat buoc thuoc dung ticket_id.
    CONSTRAINT fk_attachments_comment_ticket FOREIGN KEY (comment_id, ticket_id)
        REFERENCES ticket_comments (id, ticket_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    ticket_id  BIGINT NULL,
    message    VARCHAR(255) NOT NULL,
    read_flag  BIT(1) NOT NULL DEFAULT b'0',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES app_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Entity luu ticketId kieu Long; DB van rang buoc FK de tranh ID mo coi.
    -- TicketService.delete da xoa notifications truoc khi xoa ticket.
    CONSTRAINT fk_notifications_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
