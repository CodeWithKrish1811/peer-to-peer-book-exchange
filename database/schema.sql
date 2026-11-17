-- =====================================================================
--  Student Peer-to-Peer Book Exchange System
--  Database : book_exchange_db   (MySQL 8.0+)
--  Run with : mysql -u root -p < database/schema.sql
--  NOTE: this script drops and re-creates the four tables, so re-running it
--        resets the database to the sample data.
--  All sample students use the password:  Password@123
-- =====================================================================

CREATE DATABASE IF NOT EXISTS book_exchange_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE book_exchange_db;

DROP TABLE IF EXISTS exchange_history;
DROP TABLE IF EXISTS exchange_requests;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS students;

-- ---------------------------------------------------------------------
-- 1. STUDENTS
-- ---------------------------------------------------------------------
CREATE TABLE students (
    student_id    INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    college_id    VARCHAR(30)  NOT NULL,          -- the student's university ID number
    phone         VARCHAR(20)  NULL,
    password_hash VARCHAR(255) NOT NULL,          -- PBKDF2 hash, never plain text
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_students_email   UNIQUE (email),
    CONSTRAINT uq_students_college UNIQUE (college_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. BOOKS
-- ---------------------------------------------------------------------
CREATE TABLE books (
    book_id             INT AUTO_INCREMENT PRIMARY KEY,
    owner_id            INT          NOT NULL,
    title               VARCHAR(200) NOT NULL,
    author              VARCHAR(150) NOT NULL,
    subject             VARCHAR(100) NOT NULL,
    isbn                VARCHAR(20)  NULL,
    edition             VARCHAR(30)  NOT NULL,
    book_condition      ENUM('NEW','GOOD','AVERAGE') NOT NULL DEFAULT 'GOOD',
    description         TEXT         NULL,
    exchange_preference VARCHAR(255) NULL,
    availability        ENUM('AVAILABLE','EXCHANGE_PENDING','EXCHANGED') NOT NULL DEFAULT 'AVAILABLE',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_owner FOREIGN KEY (owner_id) REFERENCES students(student_id),
    INDEX idx_books_owner   (owner_id),
    INDEX idx_books_title   (title),
    INDEX idx_books_author  (author),
    INDEX idx_books_subject (subject),
    INDEX idx_books_avail   (availability)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. EXCHANGE_REQUESTS
-- ---------------------------------------------------------------------
CREATE TABLE exchange_requests (
    request_id        INT AUTO_INCREMENT PRIMARY KEY,
    requester_id      INT NOT NULL,
    owner_id          INT NOT NULL,
    requested_book_id INT NOT NULL,
    offered_book_id   INT NULL,                    -- optional book offered in return
    message           VARCHAR(500) NULL,
    request_status    ENUM('PENDING','ACCEPTED','REJECTED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    request_date      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_requester FOREIGN KEY (requester_id)      REFERENCES students(student_id),
    CONSTRAINT fk_req_owner     FOREIGN KEY (owner_id)          REFERENCES students(student_id),
    CONSTRAINT fk_req_requested FOREIGN KEY (requested_book_id) REFERENCES books(book_id),
    CONSTRAINT fk_req_offered   FOREIGN KEY (offered_book_id)   REFERENCES books(book_id) ON DELETE SET NULL,
    CONSTRAINT chk_req_not_self CHECK (requester_id <> owner_id),
    INDEX idx_req_requester (requester_id),
    INDEX idx_req_owner     (owner_id),
    INDEX idx_req_book      (requested_book_id),
    INDEX idx_req_status    (request_status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. EXCHANGE_HISTORY  (audit trail: one row per status change)
-- ---------------------------------------------------------------------
CREATE TABLE exchange_history (
    history_id      INT AUTO_INCREMENT PRIMARY KEY,
    request_id      INT NOT NULL,
    book_id         INT NOT NULL,
    student_id      INT NOT NULL,                  -- the student who performed the action
    exchange_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exchange_status ENUM('PENDING','ACCEPTED','REJECTED','COMPLETED','CANCELLED') NOT NULL,
    details         VARCHAR(255) NULL,
    CONSTRAINT fk_hist_request FOREIGN KEY (request_id) REFERENCES exchange_requests(request_id),
    CONSTRAINT fk_hist_book    FOREIGN KEY (book_id)    REFERENCES books(book_id),
    CONSTRAINT fk_hist_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    INDEX idx_hist_request (request_id),
    INDEX idx_hist_student (student_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- SAMPLE DATA
-- ---------------------------------------------------------------------
INSERT INTO students (full_name, email, college_id, phone, password_hash, created_at) VALUES
('Aarav Sharma', 'aarav.sharma@chitkara.edu.in', 'CU2510001', '9876500001', 'pbkdf2$65536$u0xo9EFbvOwUBQp+nRxqvA==$mZsLxpLtYnzDztpKPfSXo1mnUlylNDLCCbVJD0kO+V8=', '2026-08-01 10:00:00'),
('Priya Singh',  'priya.singh@chitkara.edu.in',  'CU2510002', '9876500002', 'pbkdf2$65536$ffrDo8TMPctyP6onLTm4RQ==$DwlRm3sI9ixJiDo3WCBb+YnsXE0SqL6Mg/pA+y8bC8U=', '2026-08-01 10:30:00'),
('Rohan Mehta',  'rohan.mehta@chitkara.edu.in',  'CU2510003', '9876500003', 'pbkdf2$65536$YaamF5cvWTynEXHPElUHNA==$Fl0Xf8fB9idLIs4jgWTH1x5m4F/Bnc4L+XOxpqXFIMQ=', '2026-08-02 09:15:00'),
('Simran Kaur',  'simran.kaur@chitkara.edu.in',  'CU2510004', '9876500004', 'pbkdf2$65536$nXmUw3Jqi4j6db1RSTxjtA==$N3HOOlqXq1ZT+qHYdP3KSrdLJ6nB2P1qbYXX5XzlWI8=', '2026-08-02 11:45:00'),
('Karan Verma',  'karan.verma@chitkara.edu.in',  'CU2510005', '9876500005', 'pbkdf2$65536$/JqR1cua0kASUqnkRSDjbQ==$ec+m23gZ8jvkavn+JtoUyne1CsmwaEqMcUZsb2RkoTU=', '2026-08-03 14:20:00'),
('Ananya Gupta', 'ananya.gupta@chitkara.edu.in', 'CU2510006', '9876500006', 'pbkdf2$65536$+TNTAU6mbSj2tnasaE4I8g==$U7qHTh47+MyMbnqmlOtTX1n4NiYV5XuGndy7HWUGEmo=', '2026-08-03 16:05:00');

INSERT INTO books (book_id, owner_id, title, author, subject, isbn, edition, book_condition, description, exchange_preference, availability, created_at) VALUES
( 1, 1, 'Java: The Complete Reference', 'Herbert Schildt', 'Programming', '9781260440232', '12th', 'GOOD', 'Covers core Java through Java 17. A few pencil notes in the margins of the collections chapters.', 'Any DBMS or Networks textbook', 'AVAILABLE', '2026-08-05 10:00:00'),
( 2, 1, 'Database System Concepts', 'Abraham Silberschatz', 'Databases', '9780078022159', '7th', 'NEW', 'Barely used. Great for the SQL and transaction chapters.', 'Computer Networks or Operating Systems', 'EXCHANGE_PENDING', '2026-08-05 10:10:00'),
( 3, 2, 'Introduction to Algorithms', 'Thomas H. Cormen', 'Algorithms', '9780262046305', '4th', 'GOOD', 'Hardcover, no missing pages. Highlighting in the sorting chapters.', NULL, 'AVAILABLE', '2026-08-05 11:00:00'),
( 4, 2, 'Operating System Concepts', 'Abraham Silberschatz', 'Operating Systems', '9781119800361', '10th', 'AVERAGE', 'Cover is worn but all pages are intact.', 'Any programming book', 'AVAILABLE', '2026-08-05 11:10:00'),
( 5, 2, 'Computer Networks', 'Andrew S. Tanenbaum', 'Networking', '9780132126953', '5th', 'GOOD', 'Clean copy with a few sticky tabs.', NULL, 'EXCHANGE_PENDING', '2026-08-05 11:20:00'),
( 6, 3, 'Higher Engineering Mathematics', 'B.S. Grewal', 'Mathematics', '9788174091956', '44th', 'AVERAGE', 'Widely used first-year reference. Some solved examples marked.', NULL, 'AVAILABLE', '2026-08-06 09:00:00'),
( 7, 3, 'Data Structures Using C', 'Reema Thareja', 'Programming', '9780198099307', '2nd', 'GOOD', 'Exchanged already.', NULL, 'EXCHANGED', '2026-08-06 09:10:00'),
( 8, 3, 'Digital Design', 'M. Morris Mano', 'Electronics', '9780134549897', '5th', 'NEW', 'Unused copy, still has the original wrapper.', 'Physics or Mathematics', 'AVAILABLE', '2026-08-06 09:20:00'),
( 9, 4, 'Engineering Physics', 'Gaur and Gupta', 'Physics', '9788187433347', '3rd', 'GOOD', 'First-year physics. Includes the numerical problem set.', NULL, 'AVAILABLE', '2026-08-07 12:00:00'),
(10, 4, 'Clean Code', 'Robert C. Martin', 'Programming', '9780132350884', '1st', 'GOOD', 'A must-read for writing maintainable Java.', 'Any algorithms book', 'AVAILABLE', '2026-08-07 12:10:00'),
(11, 4, 'Discrete Mathematics and Its Applications', 'Kenneth H. Rosen', 'Mathematics', '9781259676512', '8th', 'GOOD', 'Exchanged already.', NULL, 'EXCHANGED', '2026-08-07 12:20:00'),
(12, 5, 'Software Engineering', 'Ian Sommerville', 'Software Engineering', '9780133943030', '10th', 'NEW', 'Brand new. Good for SDLC and testing chapters.', NULL, 'AVAILABLE', '2026-08-08 15:00:00'),
(13, 5, 'Artificial Intelligence: A Modern Approach', 'Stuart Russell and Peter Norvig', 'Artificial Intelligence', '9780134610993', '4th', 'GOOD', 'Hardcover in good shape.', 'Machine learning books', 'AVAILABLE', '2026-08-08 15:10:00'),
(14, 6, 'Introduction to the Theory of Computation', 'Michael Sipser', 'Theory of Computation', '9781133187790', '3rd', 'AVERAGE', 'Some highlighting; binding is intact.', NULL, 'AVAILABLE', '2026-08-09 10:00:00'),
(15, 6, 'Python Crash Course', 'Eric Matthes', 'Programming', '9781593279288', '2nd', 'GOOD', 'Beginner-friendly Python with project chapters.', 'Any web development book', 'AVAILABLE', '2026-08-09 10:10:00');

INSERT INTO exchange_requests (request_id, requester_id, owner_id, requested_book_id, offered_book_id, message, request_status, request_date, updated_at) VALUES
(1, 2, 1,  2,    5, 'Hi Aarav, I would like your DBMS book. I can give you my Computer Networks textbook in exchange.', 'ACCEPTED',  '2026-08-10 09:00:00', '2026-08-10 18:30:00'),
(2, 4, 3,  7,   11, 'Could we swap Data Structures for my Discrete Mathematics book?',                                  'COMPLETED', '2026-08-11 10:00:00', '2026-08-14 16:00:00'),
(3, 5, 2,  3, NULL, 'I need the algorithms book for this semester. Happy to meet on campus.',                            'PENDING',   '2026-08-15 13:00:00', '2026-08-15 13:00:00'),
(4, 1, 4, 10,    1, 'Would you swap Clean Code for my Java Complete Reference?',                                         'PENDING',   '2026-08-16 08:45:00', '2026-08-16 08:45:00'),
(5, 6, 5, 12, NULL, 'Is the Software Engineering book still with you?',                                                  'REJECTED',  '2026-08-12 17:00:00', '2026-08-13 09:00:00'),
(6, 3, 6, 14, NULL, 'Interested in the Theory of Computation book.',                                                     'CANCELLED', '2026-08-12 19:00:00', '2026-08-12 20:00:00');

INSERT INTO exchange_history (request_id, book_id, student_id, exchange_date, exchange_status, details) VALUES
(1,  2, 2, '2026-08-10 09:00:00', 'PENDING',   'Exchange request sent'),
(1,  2, 1, '2026-08-10 18:30:00', 'ACCEPTED',  'Owner accepted the request'),
(2,  7, 4, '2026-08-11 10:00:00', 'PENDING',   'Exchange request sent'),
(2,  7, 3, '2026-08-11 18:00:00', 'ACCEPTED',  'Owner accepted the request'),
(2,  7, 3, '2026-08-14 16:00:00', 'COMPLETED', 'Owner marked the exchange as completed'),
(3,  3, 5, '2026-08-15 13:00:00', 'PENDING',   'Exchange request sent'),
(4, 10, 1, '2026-08-16 08:45:00', 'PENDING',   'Exchange request sent'),
(5, 12, 6, '2026-08-12 17:00:00', 'PENDING',   'Exchange request sent'),
(5, 12, 5, '2026-08-13 09:00:00', 'REJECTED',  'Owner rejected the request'),
(6, 14, 3, '2026-08-12 19:00:00', 'PENDING',   'Exchange request sent'),
(6, 14, 3, '2026-08-12 20:00:00', 'CANCELLED', 'Requester cancelled the request');

-- Keep AUTO_INCREMENT counters after the explicit ids used above
ALTER TABLE books             AUTO_INCREMENT = 16;
ALTER TABLE exchange_requests AUTO_INCREMENT = 7;
