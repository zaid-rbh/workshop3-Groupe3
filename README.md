# DevSecOps-project (VulnerableShop)

A small online shop built with Java and Spring Boot, **made insecure on purpose** for a university DevSecOps course.

> ## ⚠️ Intentionally vulnerable. Run it on your own computer only.
> Never deploy it on a server, a shared network or the internet.
> It only listens on `127.0.0.1` and uses fictional data.

**What is it for?** Practising source-code security review. Security analysis tools will be chosen and used
separately during the course. This project has **not** been scanned or audited by any tool.

---

## Quick start (about 10 minutes)

**You need:** Windows 11, [JDK 21](https://adoptium.net/), [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
(Community is fine) and an internet connection for the first run.

**1. Check Java.** Open PowerShell:

```powershell
java -version
```

It must say version `21`. If not, install JDK 21, then open a new PowerShell window.

**2. Open the project in IntelliJ.**
`File > Open` → choose the folder that contains `pom.xml` → `Trust Project`.
Wait until the progress bar at the bottom disappears (Maven is downloading libraries).

**3. Set Java 21.**
`File > Project Structure > Project` → set **SDK** to JDK 21 (use `Add SDK > Download JDK` if needed).

**4. Run.**
Open `src/main/java/com/devsecops/project/DevSecOpsProjectApplication.java` and click the green ▶ next to `main`.
When the console shows `Started DevSecOpsProjectApplication`, open **http://localhost:8080**.

Stop: red ■ in the Run window. Restart: green circular arrow.

> Prefer the command line and already have Maven? Run `mvn spring-boot:run` in the project folder.

---

## Demo accounts (all fictional)

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |
| `alice` | `alice123` | Customer |
| `bob` | `123456` | Customer |
| `charlie` | `qwerty` | Customer |

The admin pages are at http://localhost:8080/admin.

## Reset the database

The database is stored in the `data` folder. Stop the app, delete that folder, start the app again:

```powershell
Remove-Item -Recurse -Force .\data
```

Sample users, products and orders are re-created automatically. To also clear uploaded test files, delete `uploads`.

## Something went wrong?

| Problem | Fix |
|---|---|
| `java -version` is not 21 | Install JDK 21 and select it in `Project Structure` (step 3). |
| `release version 21 not supported` | IntelliJ is using an older JDK. Select JDK 21 (step 3). |
| Red imports or missing libraries | Check your internet, then open the **Maven** panel and click **Reload All Maven Projects**. |
| `Port 8080 was already in use` | Close the other program, or add `server.port=8081` to `application.properties`. |
| `Database may be already in use` | A previous run is still active. Stop it, then start again. |
| Broken data after a crash | Stop the app and reset the database (see above). |
| Error mentioning `requestMatchers` and several servlets | Add `spring.h2.console.enabled=false` to `application.properties`. |
| A page shows a stack trace | Expected for some invalid requests. Detailed errors are one of the intentional weaknesses. |

---

## What can you do in the app?

**Customers:** register and log in, edit your profile, browse and search products, read and write reviews,
use the cart, place a simulated order, see your order history, try the file-upload page.

**Administrators:** dashboard with statistics, list of users, add/edit/delete products, list of all orders.

## Technologies

Java 21 · Spring Boot 3.5 · Spring MVC · Thymeleaf · Spring Data JPA (Hibernate) · H2 database ·
Spring Security (weak on purpose) · Bootstrap 5 (bundled locally) · Maven

## Project layout

```text
DevSecOps-project/
├── pom.xml                       Maven configuration
├── docs/
│   └── INSTRUCTOR_NOTES.md       List of the weaknesses
└── src/main/
    ├── java/com/devsecops/project/
    │   ├── config/               Security, web settings, sample data
    │   ├── controller/           Web pages and API endpoints
    │   ├── service/              Business logic
    │   ├── repository/           Database access
    │   ├── model/                Database entities
    │   ├── dto/                  Form objects
    │   └── exception/            Error handling
    └── resources/
        ├── application.properties
        ├── static/               CSS and JavaScript
        └── templates/            Thymeleaf pages
```