# Test Cases, Screenshot Checklist and Progress Report

Setup: load `database/schema.sql`, deploy the WAR, open `http://localhost:8080/book-exchange/`. Sample password for everyone: `Password@123`.
Mark each row Pass or Fail when you run it, and keep the table as evidence of testing.

## A. Test cases
| ID | Area | Steps | Expected result |
|---|---|---|---|
| T01 | Register | Fill all fields correctly, submit | Redirected to login with "Account created" message |
| T02 | Register | Leave a field empty (turn off browser validation or send via curl) | Message "... is required." |
| T03 | Register | Email `abc@` | "Please enter a valid email address." |
| T04 | Register | Use `aarav.sharma@chitkara.edu.in` | "An account with this email already exists." |
| T05 | Register | Use an existing student ID with a new email | "An account with this student ID already exists." |
| T06 | Register | Password `abc` | Password-length message; `abcdefgh` gives letter and digit message |
| T07 | Register | Password and confirm differ | Browser blocks, or server says they do not match |
| T08 | Passwords | In MySQL: `SELECT password_hash FROM students;` | Values start with `pbkdf2$`, no plain text |
| T09 | Login | Correct credentials | Dashboard opens with "Welcome back, <first name>" |
| T10 | Login | Wrong password, and unknown email | Same message "Invalid email or password." |
| T11 | Access control | Logged out, open `/dashboard`, `/books`, `/requests` | Redirected to login with "Please log in" |
| T12 | Logout | Click Log out, then press the browser Back button | Login page; protected page is not shown |
| T13 | Dashboard | Log in as Aarav | Counts match the database: books listed 2, available to exchange 1, incoming 0, outgoing 1, completed 0 |
| T14 | Add book | Fill the form, save | "Book added", appears in My books and on Explore for other users |
| T15 | Add book | Leave title empty, invalid ISBN `123` | Form redisplays with the error and the typed values kept |
| T16 | Edit book | Edit own book, change condition | Updated values in the list |
| T17 | Ownership | Log in as Aarav, open `/books?action=edit&id=3` (Priya's book) | "You can only edit your own books." |
| T18 | Ownership | Send a POST with `action=delete&id=3` while logged in as Aarav (curl or dev tools) | "You can only delete your own books." Book still exists |
| T19 | Delete | Add a new book, delete it | Removed |
| T20 | Delete | Delete a book that has request history (e.g. book 7 as Rohan) | Refused with explanation |
| T21 | Availability | Switch an available book to Exchanged and back | Status updates; Exchange Pending books show "Managed in Requests" |
| T22 | Search | Search `algorithms` | Matches by title |
| T23 | Search | Search `Silberschatz`, "Author only" | Matches by author |
| T24 | Filter | Subject = Mathematics, Condition = Good | Only matching available books |
| T25 | Search | Search text `' OR 1=1 --` | Shows "No books match", no error (prepared statements) |
| T26 | Explore | Log in as Priya | None of Priya's own books appear; Exchange Pending / Exchanged books never appear |
| T27 | Request | As Karan, request book 4 (Priya's OS book), offer one of your books, add a message | "Exchange request sent"; appears under Outgoing as Pending |
| T28 | Request rule | Request the same book again (open `/requests?action=new&bookId=4` and submit) | "You already have an active request for this book." |
| T29 | Request rule | Open `/requests?action=new&bookId=<your own book id>` | "You cannot request your own book." |
| T30 | Request rule | Open the request form for an Exchange Pending book (id 2) | "not available for exchange" |
| T31 | Incoming | Log in as Priya | Karan's request shows his name, student ID, e-mail, offered book and message |
| T32 | Accept | Priya accepts a request | Request Accepted; the book becomes Exchange Pending; offered book also Exchange Pending |
| T33 | Conflict | Two students request the same book; owner accepts the first | The other request is automatically Rejected with a log entry |
| T34 | Reject | Owner rejects a pending request | Status Rejected; book remains Available |
| T35 | Authorization | As a non-owner, POST `action=accept&requestId=3` | "Only the book owner can accept requests." |
| T36 | Complete | As Aarav, mark the accepted exchange completed | Request Completed; both books Exchanged; dashboard "Completed" increases |
| T37 | Complete | As Priya (requester), POST `action=complete` | "Only the book owner can mark an exchange as completed." |
| T38 | Cancel | As Karan, cancel a pending request | Status Cancelled; Cancel button disappears |
| T39 | Cancel accepted | Requester cancels an Accepted request | Books return to Available |
| T40 | History | Open History, use each status tab | Rows filter correctly; completed items remain visible |
| T41 | Activity log | Perform T32 and check the log | One entry per status change, with who and when |
| T42 | Profile | Change name and phone | Saved; sidebar shows new name |
| T43 | Password | Change with wrong current password | "Your current password is incorrect." |
| T44 | Password | Change correctly, log out, log in with the new one | Works; old password fails |
| T45 | Database down | Stop MySQL, open Dashboard | Friendly error message, no stack trace |
| T46 | Concurrency | Open the same pending request in two browser sessions as owner; click Accept in one, then Reject in the other | Second action says the request is already accepted |
| T47 | XSS | Add a book titled `<script>alert(1)</script>` | Displayed as text, no alert |
| T48 | Responsive | Resize to about 400 px wide | Sidebar becomes a Menu button; cards stack; tables scroll sideways |

## B. Screenshot checklist for evaluation
1. Landing page
2. Registration page with a validation error
3. Login page
4. Dashboard with real statistics
5. Add book form, then My books list
6. Edit book form
7. Explore page with search and filters applied
8. Request exchange form
9. Incoming requests (owner view) with Accept and Reject
10. Outgoing requests including an Accepted request showing owner contact
11. Exchange history with the status tabs and activity log
12. Profile page
13. MySQL Workbench / CLI: `SHOW TABLES;` and a `SELECT * FROM exchange_requests;`
14. MySQL row showing a hashed password
15. Mobile-width view
16. Project structure in the IDE and the Maven `BUILD SUCCESS` output

## C. Milestone progress report
**Completed features (all seven modules)**
* Registration, login, logout, session protection (`AuthenticationFilter`), PBKDF2 password hashing
* Dashboard with live statistics and recent books
* Book add, edit, delete, availability change, owner-only enforcement
* Explore with title and author search, subject and condition filters, book cards
* Exchange workflow: send (with optional offered book and message), accept, reject, cancel, complete, with transactions and automatic conflict rejection
* Exchange history with status filters and an activity log
* Profile: update details, change password, statistics
* Database script with constraints, indexes and sample data; custom exceptions; validation; responsive UI; documentation

**Verification status**
* All Java sources compile cleanly. PBKDF2 hashes in the sample data were verified with `PasswordUtil`.
* Still to do by the team: run test cases T01 to T48 on a real MySQL and Tomcat and record the results.

**Pending**
* Run and record the manual test table above; capture screenshots.

**Current limitations**
* No CSRF tokens, HTTP only, no pagination, no notifications or chat, no book images, no automated unit tests.

**Planned improvements**
* JUnit tests for `ValidationUtil`, `PasswordUtil` and the service layer
* CSRF token per session; HTTPS
* E-mail or in-app notifications when a request arrives or changes
* Pagination and sorting on Explore; book cover upload
* "Forgot password" flow; admin role for moderation
