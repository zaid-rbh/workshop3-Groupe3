# Instructor notes (private)

This document lists every intentional weakness in VulnerableShop. **Do not publish it** in a public repository if
you want students or peers to discover the issues themselves.

Every weakness is also marked in the source code with a comment `VULNERABILITY #n`. Search for that text in IntelliJ
(`Ctrl+Shift+F`). Paths below are relative to `src/main/java/com/devsecops/project/` unless stated otherwise.

The CWE numbers are given as a reference for later discussion with analysis tools. No tool has been run on this code.

## Summary

| # | Category | Weakness | Main location |
|---|---|---|---|
| 1 | Injection | SQL injection in product search | `service/ProductService.java` `search()` |
| 2 | Injection | SQL injection in ORDER BY (`sort`) | `service/ProductService.java` `search()` |
| 3 | Authentication | Weak default credentials | `config/DataInitializer.java` `run()` |
| 4 | Authentication | MD5 password hashing without salt | `service/CryptoService.java` `md5()` |
| 5 | Authentication | User enumeration at login | `config/SecurityConfig.java` failure handler |
| 6 | Session | Insecure cookie flags, 24 h session | `resources/application.properties` |
| 7 | Session | Session fixation protection disabled | `config/SecurityConfig.java` |
| 8 | Session | CSRF protection disabled | `config/SecurityConfig.java` |
| 9 | Access control | IDOR on profiles (read and write) | `controller/ProfileController.java` |
| 10 | Access control | IDOR on orders and cart items | `controller/OrderController.java`, `CartController.java` |
| 11 | Access control | Admin area without role check | `config/SecurityConfig.java`, `controller/AdminController.java` |
| 12 | Access control | Mass assignment of `role` | `controller/ProfileController.java` `update()` |
| 13 | XSS | Stored XSS in reviews and descriptions | `templates/product-detail.html` |
| 14 | XSS | Reflected XSS in search | `templates/products.html` |
| 15 | Business logic | Client-controlled price, quantity, total | `CartController`, `CartService`, `OrderService` |
| 16 | Validation | No input validation, unchecked identifiers | `AuthController`, `RegisterForm`, `CartService` |
| 17 | Error handling | Stack traces and SQL shown to users | `exception/GlobalExceptionHandler.java`, `templates/error.html` |
| 18 | Secrets | Hard-coded fake keys and password | `config/AppSecrets.java` |
| 19 | Cryptography | AES/ECB with static key, weak random | `service/CryptoService.java`, `OrderService.java` |
| 20 | Logging | Sensitive data written to logs | `service/UserService.java`, `OrderService.java` |
| 21 | File handling | Unrestricted file upload, same-origin serving | `service/FileStorageService.java`, `config/WebConfig.java` |
| 22 | HTTP headers | Security headers disabled | `config/SecurityConfig.java` |
| 23 | CORS / API | Open CORS, unauthenticated API, full entity returned | `config/WebConfig.java`, `controller/ApiController.java` |
| 24 | Configuration | Debug defaults, H2 console, swallowed exception | `resources/application.properties`, `FileStorageService.java` |

## Details

### 1. SQL injection in product search (CWE-89)
* **Location:** `ProductService.search()`, called by `ProductController.list()` (`/products?q=`).
* **Why insecure:** the keyword is concatenated into a native SQL string, so input changes the structure of the query.
* **Secure approach:** parameterized queries (Spring Data derived or `@Query` with named parameters), never string concatenation.

### 2. SQL injection in ORDER BY (CWE-89)
* **Location:** same method, `sort` parameter (`/products?sort=`).
* **Why insecure:** column names cannot be bound as parameters, and the raw request value is appended to the query.
* **Secure approach:** map the request value to an allow-list of known columns (or use Spring `Sort` with validation).

### 3. Weak default credentials (CWE-521, CWE-1392)
* **Location:** `DataInitializer.run()` (`admin/admin123`, `bob/123456`, `charlie/qwerty`), also displayed on `login.html`.
* **Why insecure:** guessable passwords, shared in documentation and UI.
* **Secure approach:** no default credentials in production; force a password change; enforce a password policy.

### 4. MD5 without salt (CWE-328, CWE-916)
* **Location:** `CryptoService.md5()`, used by `UserService.register()`, `DataInitializer` and the `PasswordEncoder` bean in `SecurityConfig`.
* **Why insecure:** MD5 is fast and broken; identical passwords give identical hashes; rainbow tables work.
* **Secure approach:** `BCryptPasswordEncoder` or Argon2 through `DelegatingPasswordEncoder`.

### 5. User enumeration (CWE-203)
* **Location:** the `AuthenticationFailureHandler` in `SecurityConfig.filterChain()` and the messages in `login.html`.
* **Why insecure:** different messages reveal whether a username exists.
* **Secure approach:** one generic message such as "Invalid username or password", and rate limiting.

### 6. Insecure session cookie and long lifetime (CWE-1004, CWE-614, CWE-613)
* **Location:** `application.properties` (`http-only=false`, `secure=false`, `timeout=24h`).
* **Why insecure:** scripts can read the cookie, it can travel over HTTP, and stolen sessions stay valid for a day.
* **Secure approach:** `HttpOnly`, `Secure`, `SameSite`, short idle timeout.

### 7. Session fixation (CWE-384)
* **Location:** `SecurityConfig` `sessionFixation(fix -> fix.none())`.
* **Why insecure:** the session id is kept after login, so an attacker who set it beforehand shares the session.
* **Secure approach:** keep the Spring default (`changeSessionId`).

### 8. CSRF disabled (CWE-352)
* **Location:** `SecurityConfig` `csrf.disable()`. Impact is visible in `AdminController.deleteProduct()` (state change through GET).
* **Why insecure:** another website can make a logged-in browser send state-changing requests.
* **Secure approach:** keep CSRF tokens enabled, use POST/DELETE for state changes, SameSite cookies.

### 9. IDOR on profiles (CWE-639)
* **Location:** `ProfileController.profile()` and `update()` (`/profile/{id}`).
* **Why insecure:** the id in the URL is trusted; any logged-in user can read or change any profile (including the decrypted fictional card number).
* **Secure approach:** derive the user from the authenticated principal, or verify ownership on every request.

### 10. IDOR on orders and cart items (CWE-639)
* **Location:** `OrderController.orderDetail()` (`/orders/{id}`); `CartController.update()/remove()` and `CartService`.
* **Why insecure:** no check that the order or cart item belongs to the caller.
* **Secure approach:** query by id and owner (for example `findByIdAndUserId`) and return 404/403 otherwise.

### 11. Admin area without role check (CWE-862, CWE-285)
* **Location:** `SecurityConfig` rule `/admin/**` -> `authenticated()` only; `AdminController`; the link is hidden in `layout.html` with `hasRole('ADMIN')`.
* **Why insecure:** hiding a menu item is not access control; any logged-in user can open `/admin`.
* **Secure approach:** `hasRole("ADMIN")` on the route and/or `@PreAuthorize` on the controller.

### 12. Mass assignment (CWE-915)
* **Location:** `ProfileController.update()` binds a full `User` and copies `role`.
* **Why insecure:** a user can add a `role=ADMIN` parameter to the profile form request.
* **Secure approach:** bind to a dedicated DTO containing only editable fields.

### 13. Stored XSS (CWE-79)
* **Location:** `product-detail.html` (`th:utext` on `r.author`, `r.content`, `product.description`); stored by `ProductService.addReview()`.
* **Why insecure:** HTML or script submitted in a review runs in every visitor's browser.
* **Secure approach:** `th:text` (automatic escaping), input sanitization where HTML is needed, Content-Security-Policy.

### 14. Reflected XSS (CWE-79)
* **Location:** `products.html` (`th:utext="${query}"`).
* **Why insecure:** the search term from the URL is printed unescaped.
* **Secure approach:** `th:text`; contextual output encoding.

### 15. Client-controlled price, quantity and total (CWE-472, CWE-602)
* **Location:** hidden `price` field in `product-detail.html`; `CartController.add()`; `CartService.addToCart()`; hidden `total` in `checkout.html`; `OrderService.placeOrder()`.
* **Why insecure:** the server trusts values that the client can edit (negative quantity, price 0.01, total 0.01).
* **Secure approach:** read prices from the database, validate quantity ranges, recompute totals on the server.

### 16. Missing validation and unchecked identifiers (CWE-20)
* **Location:** `RegisterForm`/`AuthController.register()` (no constraints); `CartService.addToCart()` uses `Optional.get()` and no stock check.
* **Why insecure:** invalid or malicious data is accepted and unexpected exceptions surface.
* **Secure approach:** Bean Validation (`@NotBlank`, `@Size`, `@Email`), explicit not-found handling, stock checks.

### 17. Verbose errors (CWE-209)
* **Location:** `GlobalExceptionHandler`, `templates/error.html`, `server.error.*` in `application.properties`.
* **Why insecure:** class names, SQL fragments and stack traces help an attacker understand the system.
* **Secure approach:** log details server-side; return a generic message and a correlation id.

### 18. Hard-coded secrets (CWE-798)
* **Location:** `AppSecrets` (`API_KEY`, `AES_KEY`, `ADMIN_BACKUP_PASSWORD`; all fake).
* **Why insecure:** secrets in source code end up in version control and every build.
* **Secure approach:** environment variables or a secrets manager; rotate keys.

### 19. Weak cryptography and randomness (CWE-327, CWE-330)
* **Location:** `CryptoService.encrypt()/decrypt()` (AES in ECB mode, static key); `OrderService.placeOrder()` uses `java.util.Random`.
* **Why insecure:** ECB leaks patterns and has no integrity; the key is public in the code; `Random` is predictable.
* **Secure approach:** AES-GCM with random IV and managed keys; `SecureRandom`; do not store card numbers at all.

### 20. Sensitive data in logs (CWE-532)
* **Location:** `UserService.register()` logs the clear password; `OrderService.placeOrder()` logs the card number.
* **Why insecure:** logs are widely readable and long-lived.
* **Secure approach:** never log secrets or payment data; mask where needed.

### 21. Unrestricted file upload (CWE-434)
* **Location:** `FileStorageService.save()`, `WebConfig.addResourceHandlers()`, `FileUploadController`.
* **Why insecure:** any type and size is accepted, files overwrite existing ones, and uploaded HTML is served from the application's origin (script execution in that origin).
* **Secure approach:** allow-list of types, content inspection, size limits, random file names, store outside the web root, serve with `Content-Disposition: attachment`.
* **Note:** the code deliberately keeps writes inside the `uploads` folder (path traversal is *not* part of this exercise).

### 22. Missing security headers (CWE-693, CWE-1021)
* **Location:** `SecurityConfig` `headers.disable()`.
* **Why insecure:** no clickjacking protection, no MIME sniffing protection, no CSP, no cache control for pages with personal data.
* **Secure approach:** keep Spring Security defaults and add a Content-Security-Policy.

### 23. Open CORS and exposed API (CWE-942, CWE-200)
* **Location:** `WebConfig.addCorsMappings()` (`allowedOrigins("*")`); `ApiController` (`/api/users/{id}` returns the full `User`).
* **Why insecure:** any origin can read the API from a browser; the API needs no login and leaks password hashes.
* **Secure approach:** restrict origins, require authentication, return DTOs without sensitive fields.

### 24. Debug configuration and poor exception handling (CWE-489, CWE-215, CWE-390)
* **Location:** `application.properties` (H2 console on, `show-sql`, SQL debug logging); `FileStorageService.save()` swallows the exception after `printStackTrace()`.
* **Why insecure:** the H2 console exposes the database; debug output leaks information; swallowed errors hide failures from operators.
* **Secure approach:** disable debug features outside development; handle and log exceptions properly with a logging framework.

## Suggested classroom use

1. Let students review the code for a fixed time and list suspected issues.
2. Compare with the table above.
3. Later, compare their findings with the output of the analysis tools chosen during the course (some issues, such as IDOR and business logic flaws, are much harder for tools to find than injection or hard-coded secrets).
