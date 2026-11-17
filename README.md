# Student Peer-to-Peer Book Exchange System (BookSwap)

Programming in Java (25CSE0202), Chitkara University. A web application where students list academic books, search for books other students have listed, and exchange them through a request, accept or reject, and completion workflow. There are no payments.

**Stack:** HTML5, CSS3, a little JavaScript, Core Java, Java Servlets, JDBC, MySQL 8, Apache Tomcat 9, Maven. No Spring, Hibernate, Node or React.

---

## 1. Problem statement
Students pay full price for textbooks they use for one semester, while the same books sit unused with the previous batch. Finding the right book today depends on personal contacts and scattered messaging groups. There is no central, reliable record of who has which book, who asked for it, and whether the exchange actually happened.

## 2. Objectives
1. Give students one place to register, list books and search for books.
2. Support a complete exchange workflow: request, accept or reject, complete or cancel.
3. Prevent conflicts (own-book requests, duplicate requests, two students getting the same book) using database transactions.
4. Keep a permanent history of every request and status change.
5. Demonstrate Core Java, OOP, JDBC, MySQL, collections and exception handling in a clean layered design.

**Scope:** college students exchanging books on one campus. **Out of scope:** payments, delivery, chat, mobile app, e-mail notifications.

## 3. Features
| Module | What it does |
|---|---|
| Registration and login | Validated form, unique email and student ID, PBKDF2 hashed passwords, session login and logout, protected pages |
| Dashboard | Live counts from MySQL: books listed, available, incoming and outgoing requests, completed exchanges, recent books |
| My books | Add, edit, delete, change availability. Only the owner can modify a book |
| Explore | Search by title or author, filter by subject and condition, book cards, own books hidden |
| Requests | Send a request (optionally offering your own book), owner accepts or rejects, requester cancels, owner completes |
| History | Filter by status, activity log of every status change, records kept after completion |
| Profile | Update name and phone, change password, statistics, listed books |

**Business rules enforced in `ExchangeService`, inside one transaction:** no request for your own book; one active request per student per book; only available books can be requested; only the owner can accept, reject or complete; accepting reserves the book (and the offered book) and automatically rejects competing pending requests; completing marks both books Exchanged; cancelling an accepted request releases the books.

## 4. Project structure
```
book-exchange/
+-- pom.xml
+-- database/schema.sql              schema + sample data
+-- docs/                            architecture, tests, progress report
+-- src/main
    +-- java/com/bookexchange/
    |   +-- config/    DBConnection
    |   +-- model/     BaseEntity, Student, Book, ExchangeRequest, ExchangeHistory, 3 enums
    |   +-- dao/       BaseDao, StudentDAO, BookDAO, ExchangeRequestDAO, ExchangeHistoryDAO
    |   +-- service/   StudentService, BookService, ExchangeService
    |   +-- servlet/   Register, Login, Logout, Dashboard, Book, SearchBook, ExchangeRequest, Profile, History
    |   +-- filter/    AuthenticationFilter, EncodingFilter
    |   +-- exception/ AppException + 4 subclasses
    |   +-- util/      ValidationUtil, PasswordUtil, Tx, SessionUtil, HtmlUtil
    +-- resources/db.properties
    +-- webapp/        index/login/register/error .html, css/style.css, js/app.js, WEB-INF/web.xml
```

## 5. Requirements
* JDK 17 or newer
* Maven 3.8+
* MySQL 8.0.16+ (the schema uses a CHECK constraint)
* **Apache Tomcat 9.x**. Use 9, not 10 or 11: Tomcat 10+ switched to the `jakarta.*` packages and this project uses `javax.servlet`.

## 6. Database setup
```bash
mysql -u root -p < database/schema.sql
```
This creates `book_exchange_db`, the four tables, and sample data. **It drops and recreates the tables each time**, so running it again resets the data.

Database credentials are read from environment variables first, then from `src/main/resources/db.properties`:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/book_exchange_db?...` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | empty |

Recommended: keep the password out of the source code.
* Windows: create `TOMCAT_HOME\bin\setenv.bat` containing `set "DB_PASSWORD=your_password"`
* Linux/macOS: create `TOMCAT_HOME/bin/setenv.sh` containing `export DB_PASSWORD=your_password`

(Or put it in your local `db.properties` and never commit that change.)

## 7. Build and run
```bash
mvn clean package            # produces target/book-exchange.war
```
**Plain Tomcat:** copy `target/book-exchange.war` to `TOMCAT_HOME/webapps/`, run `bin/startup.bat` (or `startup.sh`), open **http://localhost:8080/book-exchange/**.

**IntelliJ IDEA:** Ultimate: Run > Edit Configurations > Tomcat Server > Local, add the artifact `book-exchange:war exploded`, application context `/book-exchange`. Community: install the *Smart Tomcat* plugin, point it at Tomcat 9, set context path `/book-exchange`.

**VS Code:** install *Extension Pack for Java* and *Community Server Connectors*, add your Tomcat 9 in the Servers view, right-click it, *Add Deployment*, choose `target/book-exchange.war`, then *Start Server*. (Set `DB_PASSWORD` in `setenv` as above before starting.)

## 8. Sample accounts
Password for every sample student: **`Password@123`**

| Name | Email | What they have in the sample data |
|---|---|---|
| Aarav Sharma | aarav.sharma@chitkara.edu.in | Owner of an **accepted** exchange (can *Mark as completed*); sent a pending request to Simran |
| Priya Singh | priya.singh@chitkara.edu.in | Requester in that accepted exchange (sees Aarav's e-mail); has a **pending incoming** request from Karan |
| Rohan Mehta | rohan.mehta@chitkara.edu.in | Owner in a **completed** exchange; sent a cancelled request |
| Simran Kaur | simran.kaur@chitkara.edu.in | Requester in the completed exchange; has a **pending incoming** request from Aarav |
| Karan Verma | karan.verma@chitkara.edu.in | Has a **pending outgoing** request (can cancel it); rejected a request from Ananya |
| Ananya Gupta | ananya.gupta@chitkara.edu.in | Sent a request that was rejected; received a request that was cancelled |

**Quick demo path**
1. Log in as **Aarav**, open *Requests*, and use *Mark as completed* on Priya's accepted request. Both books become *Exchanged*.
2. Log in as **Simran**, open *Requests*, and accept Aarav's request for *Clean Code*. The book becomes *Exchange Pending*.
3. Log in as **Priya**, accept or reject Karan's request for *Introduction to Algorithms*.
4. Log in as **Karan** and look at *History* and the activity log.
5. Register a brand-new account, add a book, and request someone else's book.

## 9. Known limitations
* Tested for compilation only in the authoring environment. Run the checklist in `docs/TESTING.md` on your own MySQL and Tomcat before the demo.
* No CSRF tokens on forms (acceptable for a prototype; add a per-session token for production).
* Runs over HTTP; use HTTPS and secure cookies when deployed.
* No pagination on long lists, no e-mail or in-app notifications, no chat, no book photos.
* Only the book owner can mark an exchange completed. If the owner never does, the books stay *Exchange Pending* until the requester cancels.
* A book that has ever appeared in a request cannot be deleted (history is preserved). Mark it *Exchanged* instead.
* No automated unit tests; testing is by the manual test cases in `docs/TESTING.md`.
