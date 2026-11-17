# Architecture, Database and Workflow

## 1. Layers
```
Browser  --HTTP-->  Filters  -->  Servlets  -->  Services  -->  DAOs  --JDBC-->  MySQL
                    (encoding,     (read form,    (validation,    (SQL only,
                     auth check)    render HTML)   rules,          PreparedStatement)
                                                   transactions)
```
| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `servlet`, `filter`, `util.HtmlUtil`, `webapp/` | Read request parameters, call a service, redirect or render HTML. No SQL and no business rules |
| Business | `service` | Validation, ownership checks, business rules, transaction boundaries |
| Data access | `dao` | One class per table, SQL via `PreparedStatement`, map `ResultSet` to model objects |
| Model | `model` | Plain objects (encapsulation) and enums |
| Infrastructure | `config.DBConnection`, `util.Tx`, `util.PasswordUtil`, `util.SessionUtil` | Connections, transactions, hashing, sessions |

Servlets render HTML on the server through `HtmlUtil`. Every dynamic value goes through `HtmlUtil.esc()` to prevent cross-site scripting. Static pages (landing, login, register) are plain HTML. Their error and success messages arrive in the URL and are written with `textContent` by `app.js`.

## 2. Request flow example: accepting a request
1. The owner clicks **Accept**, which POSTs `action=accept&requestId=7` to `/requests`.
2. `AuthenticationFilter` confirms a session exists.
3. `ExchangeRequestServlet.doPost` parses the id and calls `ExchangeService.accept(ownerId, 7)`.
4. `Tx.write` opens a connection with autocommit off, and the service then:
   1. locks the request row (`SELECT ... FOR UPDATE`), checks the owner and that it is `PENDING`;
   2. locks the requested book (and the offered book), checks they are `AVAILABLE`;
   3. sets the request to `ACCEPTED` and the book(s) to `EXCHANGE_PENDING`;
   4. inserts an `exchange_history` row;
   5. rejects all other pending requests for that book and logs each.
5. `Tx.write` commits. If any step throws, it calls `rollback()`, so there are never half-applied changes.
6. The servlet stores a flash message in the session and redirects to the Requests page.

Because rows are locked, two simultaneous accept or request actions on the same book are serialised by MySQL: the second one sees the book is no longer available and gets a clear message.

## 3. Request status lifecycle
```
              +--> REJECTED   (owner rejects, or auto-rejected when the book is promised elsewhere)
PENDING ------+--> CANCELLED  (requester cancels)
              +--> ACCEPTED --+--> COMPLETED  (owner confirms the hand-over)
                              +--> CANCELLED  (requester cancels; books are released)
```
Book availability: `AVAILABLE` -> `EXCHANGE_PENDING` (accepted) -> `EXCHANGED` (completed). Owners can manually toggle between `AVAILABLE` and `EXCHANGED`; `EXCHANGE_PENDING` is controlled only by the workflow.

## 4. Database schema
Database `book_exchange_db`, 4 tables, InnoDB, utf8mb4.

```
students 1 ----< books 1 ----< exchange_requests >---- 1 students (requester)
    |                                 |   \\
    |                                 |    >-- offered_book_id (optional) --> books
    +---------<  exchange_history >---+
```
| Table | Key columns | Constraints and indexes |
|---|---|---|
| `students` | `student_id` PK, `full_name`, `email`, `college_id`, `phone`, `password_hash`, `created_at` | UNIQUE email, UNIQUE college_id |
| `books` | `book_id` PK, `owner_id` FK, `title`, `author`, `subject`, `isbn`, `edition`, `book_condition`, `description`, `exchange_preference`, `availability`, `created_at` | FK to students; ENUMs for condition and availability; indexes on owner, title, author, subject, availability |
| `exchange_requests` | `request_id` PK, `requester_id` FK, `owner_id` FK, `requested_book_id` FK, `offered_book_id` FK (nullable, ON DELETE SET NULL), `message`, `request_status`, `request_date`, `updated_at` | CHECK requester <> owner; ENUM status; indexes on requester, owner, book, status |
| `exchange_history` | `history_id` PK, `request_id` FK, `book_id` FK, `student_id` FK, `exchange_date`, `exchange_status`, `details` | One row per status change (audit trail); indexes on request and student |

Notes on differences from the suggested design: the column is named `book_condition` because `CONDITION` is a reserved word in MySQL; `students` has an extra `college_id` (the student ID number from the registration form) and `phone` (profile contact information); `exchange_history` has an extra `details` column. Normalisation: every table stores one kind of fact, and joined data (names, titles) is fetched with JOINs instead of being copied.

## 5. JDBC usage map
| Concept | Where |
|---|---|
| `DriverManager.getConnection` | `config/DBConnection.java` |
| `PreparedStatement` (all queries) | every DAO |
| `ResultSet` mapping | `map(...)` methods in the DAOs |
| INSERT with generated keys | `StudentDAO.insert`, `BookDAO.insert`, `ExchangeRequestDAO.insert` |
| UPDATE | `BookDAO.update`, `updateAvailability`, `ExchangeRequestDAO.updateStatus`, `StudentDAO.updateProfile` |
| DELETE | `BookDAO.delete` |
| SELECT with dynamic filters | `BookDAO.search` (parameters list bound with `setObject`) |
| JOIN queries | `BookDAO.SELECT_WITH_OWNER`, `ExchangeRequestDAO.SELECT_JOINED` (5 tables), `ExchangeHistoryDAO.findForStudent` |
| GROUP BY | `ExchangeRequestDAO.countByStatus` |
| Row locking | `findByIdForUpdate` in `BookDAO` and `ExchangeRequestDAO` |
| Transactions, commit, rollback | `util/Tx.write` used by every state-changing service method |
| try-with-resources | all DAOs, `DBConnection` users, `Tx` |

## 6. OOP map
| Concept | Classes |
|---|---|
| Encapsulation (private fields, getters and setters) | all `model` classes |
| Constructors | `Student`, `Book`, `ExchangeRequest`, `ExchangeHistory` (no-arg and field constructors) |
| Inheritance | `Student`, `Book`, `ExchangeRequest`, `ExchangeHistory` extend `BaseEntity`; `ValidationException`, `BusinessRuleException`, `AuthorizationException`, `DatabaseException` extend `AppException`; servlets extend `HttpServlet` |
| Polymorphism | each model overrides `BaseEntity.getSummary()`; `doGet`/`doPost` overrides; DAOs used through `BaseDao` |
| Abstraction | abstract class `BaseEntity`; interface `BaseDao<T>`; functional interface `Tx.Work<T>` |
| Method overloading | `PasswordUtil.hash(String)` and `hash(String, byte[])`; `ValidationUtil.requireNonBlank` (2 and 3 arguments); `BookDAO.search` (all filters and plain); `BookService.search` |
| Enums with behaviour | `BookCondition`, `Availability`, `RequestStatus` (`isActive()`, `fromString()`) |
| Collections | `List` of books and requests, `Map<String,Integer>` (`LinkedHashMap`) for dashboard stats, `EnumMap<RequestStatus,Integer>` for profile stats, `Optional` from DAO lookups |
| Custom exceptions | `exception` package; messages are always safe to show users |

## 7. Exception handling
* DAOs throw `SQLException`. `Tx.read` and `Tx.write` convert it into `DatabaseException` with a friendly message; the technical cause is kept inside the exception, not shown.
* Validation problems throw `ValidationException`; broken business rules throw `BusinessRuleException`; wrong credentials or touching someone else's data throw `AuthorizationException`.
* Servlets catch `AppException`, store `e.getMessage()` as a flash notification, and redirect. Unexpected errors fall through to `web.xml` error pages (`error.html`), so no stack trace is shown.

## 8. Security measures
PBKDF2-HMAC-SHA256 with a random salt and 65,536 iterations (JDK only); `PreparedStatement` everywhere (SQL injection); HTML escaping of all output (XSS); session regenerated on login (session fixation); HttpOnly cookie; `Cache-Control: no-store` on protected pages; ownership checked in the service layer *and* in the SQL `WHERE owner_id = ?`; identical error message for unknown email and wrong password; DB password from environment variable.

## 9. Team division and integration points
| Member | Files they own |
|---|---|
| 1: Authentication, profile, dashboard | `StudentDAO`, `StudentService`, `Register/Login/Logout/Dashboard/ProfileServlet`, `AuthenticationFilter`, `PasswordUtil`, `SessionUtil`, `register.html`, `login.html` |
| 2: Books and search | `BookDAO`, `BookService`, `BookServlet`, `SearchBookServlet`, `Book`, `BookCondition`, `Availability`, `ValidationUtil` |
| 3: Exchange workflow and documentation | `ExchangeRequestDAO`, `ExchangeHistoryDAO`, `ExchangeService`, `ExchangeRequestServlet`, `HistoryServlet`, `ExchangeRequest`, `ExchangeHistory`, `RequestStatus`, `Tx`, `docs/` |
| Shared (agree changes together) | `schema.sql`, `BaseEntity`, `BaseDao`, `DBConnection`, `HtmlUtil`, `style.css`, `app.js` |

The contracts between members are the model classes and the public service methods, so changes there must be discussed first.
