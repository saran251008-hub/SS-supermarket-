# SS Supermarket

A basic but fully working full-stack e-commerce web application built with **Java (Spring Boot)**, **Spring MVC**, **Spring Data JPA/Hibernate**, **Spring Security** and **MySQL**, with a server-rendered frontend using **HTML5, CSS3, JavaScript** and **Thymeleaf**.

This is the **Phase 1 foundation**. It implements real authentication, role-based authorization, a database-backed shopping cart and transactional checkout. Seller and Admin modules are built on top of this foundation in later phases.

---

## 1. Features

- User registration (BUYER / SELLER) with validation and BCrypt password hashing
- User login / logout with Spring Security (form login + REST API)
- Session management and role-based authorization
- Three roles: `BUYER`, `SELLER`, `ADMIN`
- MySQL persistence with Hibernate (tables auto-created)
- Product listing, search (by name) and category filtering
- Product details page with quantity selector
- Database-backed shopping cart (add / update quantity / remove / totals)
- Checkout with a transactional order creation flow:
  validate cart → check stock → create order + order items → reduce stock → clear cart
- Order success page and per-buyer order history
- Seller dashboard placeholder (product management in Phase 2)
- Admin dashboard placeholder with live stats
- Default admin account created automatically on startup
- 5 sample products seeded automatically when the database is empty
- Global error handling (REST + web), no stack traces exposed
- Responsive, modern UI (desktop / tablet / mobile)
- REST API under `/api/**` for auth, products, cart and orders

## 2. Technology Stack

| Layer      | Technology                                    |
|------------|-----------------------------------------------|
| Language   | Java 17                                       |
| Framework  | Spring Boot 3.3 (Spring MVC, Data JPA, Security, Validation, Thymeleaf) |
| Database   | MySQL 8 (H2 in-memory used only for tests)    |
| Build      | Maven                                         |
| Frontend   | HTML5, CSS3, vanilla JavaScript, Thymeleaf    |
| Security   | Spring Security, BCrypt, CSRF protection      |

## 3. Requirements

- Java 17 or 21 (JDK installed)
- Maven 3.8+
- MySQL 8.x running locally on port 3306

## 4. MySQL Setup

```sql
CREATE DATABASE IF NOT EXISTS sssupermarket;
```

The application also auto-creates the database on startup (via `createDatabaseIfNotExist=true`) when the configured user has the required privileges. Tables are created/updated automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

## 5. Configuration

Create an environment variable for the database password (never commit real passwords):

- Windows (PowerShell): `$env:DB_USERNAME = "root"; $env:DB_PASSWORD = "your-password"`
- Linux/macOS: `export DB_USERNAME=root; export DB_PASSWORD=your-password`

Defaults live in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sssupermarket?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=update
```

Tests run against an in-memory H2 database (`application-test.properties`) — no MySQL needed for `mvn test`.

## 6. Running the Application

```bash
# from the project root (SSSupermarket/)
mvn clean package          # build + run tests
mvn spring-boot:run        # or: java -jar target/sssupermarket-1.0.0.jar
```

Open http://localhost:8080

> **Note for local testing without MySQL:** `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true` runs against in-memory H2.

## 7. Default Admin Credentials

Created automatically the first time the application starts (stored only as a BCrypt hash):

```
Email:    admin@sssupermarket.com
Password: Admin@123
```

### Changing the admin password

Option 1 (recommended): generate a BCrypt hash of the new password and update the row in MySQL:

```sql
UPDATE users SET password = '<bcrypt-hash>' WHERE email = 'admin@sssupermarket.com';
```

You can generate a BCrypt hash with any standard tool (e.g. `htpasswd -bnBC 10 "" NewPassword` or an online bcrypt generator).

Option 2: an admin user-management UI is planned for Phase 2.

> The seed in `DataInitializer` only runs when the admin account does **not** exist, so editing it will not change an existing account.

## 8. Roles & Access

| URL / API            | Allowed                                             |
|----------------------|-----------------------------------------------------|
| `/`, `/products`, `/register`, `/login` | Public                                    |
| `/cart`, `/checkout`, `/order-success`   | Any authenticated user                   |
| `/orders`, `/api/orders/**`              | `ROLE_BUYER`                             |
| `/seller`, `/api/seller/**`              | `ROLE_SELLER`                            |
| `/admin`, `/api/admin/**`                | `ROLE_ADMIN`                             |

After login: BUYER → `/products`, SELLER → `/seller`, ADMIN → `/admin`.

## 9. API Endpoints

| Method | Endpoint                 | Description                          | Auth       |
|--------|--------------------------|--------------------------------------|------------|
| POST   | `/api/auth/register`     | Register BUYER/SELLER                | Public     |
| POST   | `/api/auth/login`        | Login (creates session)              | Public     |
| POST   | `/api/auth/logout`       | Logout / invalidate session          | Authenticated |
| GET    | `/api/auth/me`           | Current user info                    | Authenticated |
| GET    | `/api/products`          | List products (`?search=&category=`) | Public     |
| GET    | `/api/products/{id}`     | Product details                      | Public     |
| GET    | `/api/cart`              | View cart + server-computed totals   | Authenticated |
| POST   | `/api/cart`              | Add item `{productId, quantity}`     | Authenticated |
| PUT    | `/api/cart/{itemId}`     | Update quantity `{quantity}`         | Authenticated |
| DELETE | `/api/cart/{itemId}`     | Remove item                          | Authenticated |
| POST   | `/api/orders`            | Checkout (creates order)             | BUYER      |
| GET    | `/api/orders`            | My orders                            | BUYER      |
| GET    | `/api/orders/{id}`       | Order details (own only)             | BUYER      |

## 10. Project Structure

```
SSSupermarket/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/sssupermarket/
    │   │   ├── config/        SecurityConfig, DataInitializer, REST auth handlers
    │   │   ├── controller/    MVC + REST controllers, GlobalModelAdvice
    │   │   ├── service/       User, Product, Cart, Order, Auth services
    │   │   ├── repository/    Spring Data JPA repositories
    │   │   ├── entity/        User, Product, CartItem, Order, OrderItem, enums
    │   │   ├── dto/           Request/response DTOs
    │   │   ├── security/      UserDetailsService, CurrentUser, success handler
    │   │   ├── exception/     Custom exceptions + global handlers
    │   │   └── SSSupermarketApplication.java
    │   └── resources/
    │       ├── templates/     Thymeleaf pages (index, login, register, products, ...)
    │       ├── static/        css/, js/, images/
    │       └── application.properties (+ application-test.properties)
    └── test/java/com/sssupermarket/   Service + integration tests
```

## 11. Future Development (Phase 2+)

- **Seller:** product management (add / edit / delete), seller order dashboard
- **Admin:** user management, order management, product moderation
- **Reviews:** ratings, comments, review moderation
- **Bonus:** wishlist, order tracking, sales dashboard, AI chatbot

---

Built as a capstone project. Run, explore, and build Phase 2 on top of this foundation.
