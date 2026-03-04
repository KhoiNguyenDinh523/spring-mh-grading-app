# MH Solution — Grading System

A full-stack web application built with **Java Spring Boot 3**, **Spring Security**, **MySQL**, **Thymeleaf**, and **Nginx**.

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [API & Route Reference](#2-api--route-reference)
3. [Security Implementation](#3-security-implementation)
4. [Windows Localhost Setup Guide (Test First)](#4-windows-localhost-setup-guide-test-first)
   - 4.1 Install Java JDK 17
   - 4.2 Install Maven
   - 4.3 Install MySQL on Windows
   - 4.4 Create the Database
   - 4.5 Configure the Application
   - 4.6 Build the JAR
   - 4.7 Run and Test in Browser
5. [Deploy to Linux VM with Nginx](#5-deploy-to-linux-vm-with-nginx)
6. [Spring Boot Concepts for Beginners](#6-spring-boot-key-concepts-for-beginners)
7. [Common Issues & Fixes](#7-common-issues--fixes)

---

## 1. Project Structure

```
test_self_web/
├── README.md
├── nginx/
│   └── nginx.conf                    # Nginx reverse-proxy config (used in VM deploy)
├── mh-solution-static/
│   └── index.html                    # Domain 1 (abc.com) — static company site
└── grading-system/                   # Domain 2 (xyz.com) — Spring Boot app
    ├── pom.xml                       # Maven: lists all Java libraries (dependencies)
    ├── init-db.sql                   # MySQL setup script
    └── src/
        └── main/
            ├── java/com/mhsolution/grading/
            │   ├── GradingSystemApplication.java    # Entry point
            │   ├── config/
            │   │   ├── SecurityConfig.java          # ★ CORE security rules (RBAC, login)
            │   │   └── DataInitializer.java         # Creates default admin on startup
            │   ├── entity/           # Maps Java classes to DB tables (JPA)
            │   │   ├── Role.java
            │   │   ├── User.java
            │   │   └── Assignment.java
            │   ├── repository/       # Database query interfaces (Spring Data JPA)
            │   │   ├── UserRepository.java
            │   │   └── AssignmentRepository.java
            │   ├── dto/              # Form data transfer objects (input validation)
            │   │   ├── RegisterRequest.java
            │   │   ├── GradeRequest.java
            │   │   └── UpdateUserRequest.java
            │   ├── service/          # Business logic
            │   │   ├── UserService.java
            │   │   ├── AssignmentService.java
            │   │   └── impl/
            │   │       ├── UserServiceImpl.java
            │   │       └── AssignmentServiceImpl.java
            │   ├── security/
            │   │   └── CustomUserDetailsService.java
            │   ├── controller/       # HTTP request handlers
            │   │   ├── AuthController.java
            │   │   ├── ApplicantController.java
            │   │   ├── GraderController.java
            │   │   └── AdminController.java
            │   └── exception/
            │       └── ResourceNotFoundException.java
            └── resources/
                ├── application.properties           # DB connection, file paths, settings
                ├── static/css/style.css
                └── templates/                       # Thymeleaf HTML pages
                    ├── fragments/layout.html
                    ├── login.html
                    ├── register.html
                    ├── applicant/ (dashboard, upload)
                    ├── grader/ (dashboard, grade)
                    └── admin/ (dashboard, users, edit-user)
```

---

## 2. API & Route Reference

### Public Routes
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/` | Redirect to `/login` |
| GET | `/login` | Login page |
| POST | `/login` | Process login (Spring Security handles) |
| GET | `/register` | Registration form |
| POST | `/register` | Process registration |
| POST | `/logout` | Logout (invalidates session) |

### Applicant Routes (requires `ROLE_APPLICANT`)
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/applicant/dashboard` | View own uploaded assignments + grades |
| GET | `/applicant/upload` | Upload form |
| POST | `/applicant/upload` | Submit file upload (multipart) |
| GET | `/applicant/download/{id}` | Download own file (**IDOR-safe**) |

### Grader Routes (requires `ROLE_GRADER` or `ROLE_ADMIN`)
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/grader/dashboard` | View ALL submitted assignments |
| GET | `/grader/grade/{id}` | Grading form for an assignment |
| POST | `/grader/grade` | Submit score + comment |
| GET | `/grader/download/{id}` | Download any assignment file |

### Admin Routes (requires `ROLE_ADMIN`)
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/admin/dashboard` | Overview: stats, recent assignments |
| GET | `/admin/users` | List all users |
| GET | `/admin/users/{id}/edit` | Edit user form |
| POST | `/admin/users/{id}/edit` | Save user changes |
| POST | `/admin/users/{id}/delete` | Delete user + all their assignments |
| POST | `/admin/users/{id}/toggle` | Enable / disable account |
| POST | `/admin/assignments/{id}/delete` | Delete an assignment |

---

## 3. Security Implementation

### Authentication
- **Session-based** login managed by Spring Security
- Passwords hashed with **BCrypt** (strength 12)
- Session timeout: **30 minutes**
- Maximum **1 active session per user**

### Authorization (RBAC — Role-Based Access Control)
Enforced at **two levels**: URL rules in `SecurityConfig` + `@PreAuthorize` on `AdminController`

| Role | What they can do |
|------|-----------------|
| `ROLE_ADMIN` | Everything: full user CRUD, role assignment, view/delete all assignments |
| `ROLE_GRADER` | View & download all assignments, submit grades |
| `ROLE_APPLICANT` | Upload files, view & download **only their own** files |

### IDOR Protection
- Repository uses `findByIdAndUploader(id, user)` — Applicants can **never** retrieve another user's assignment, even if they guess the ID
- Returns **HTTP 404** (not 403) to avoid leaking information about other records

### File Upload Security
| Check | Detail |
|-------|--------|
| Extension whitelist | pdf, doc, docx, txt, zip, png, jpg, jpeg |
| Max size | 10 MB |
| Storage | **Outside web root**: `C:/uploads/grading-system/` |
| Filename on disk | **UUID-based** (prevents guessing & path traversal) |
| Served via | Spring controller (never as a publicly accessible static URL) |

---

## 4. Windows Localhost Setup Guide (Test First)

> Follow this entire section on your **Windows machine** before touching any VM.
> You will be able to open the app at `http://localhost:8080` in your browser.

---

### 4.1 — Install Java JDK 17

Spring Boot 3 requires **Java 17 or higher**.

1. Go to: **https://adoptium.net/temurin/releases/?version=17**
2. Select: **Windows**, **x64**, **JDK**, `.msi` installer
3. Download and run the `.msi` — click **Next** through everything.  
   ✅ Check the option **"Add to PATH"** if shown.
4. Open a **new** Command Prompt (Start → type `cmd` → Enter) and verify:

```cmd
java -version
```

Expected output (version may differ slightly):
```
openjdk version "17.0.x" ...
```

If you see an error, restart your PC and try again.

---

### 4.2 — Install Maven

Maven is the build tool that downloads all Java libraries from the internet and compiles the project.

1. Go to: **https://maven.apache.org/download.cgi**
2. Download the **Binary zip archive**: `apache-maven-3.9.x-bin.zip`
3. Extract it to `C:\maven\` (so you have `C:\maven\apache-maven-3.9.x\`)
4. Add Maven to your PATH:
   - Press `Win + S` → search **"Environment Variables"** → click **"Edit the system environment variables"**
   - Click **"Environment Variables..."** button
   - Under **System variables** → find `Path` → click **Edit**
   - Click **New** → type: `C:\maven\apache-maven-3.9.x\bin`  
     *(adjust the version number to match what you downloaded)*
   - Click **OK** on all dialogs
5. Open a **new** Command Prompt and verify:

```cmd
mvn -version
```

Expected output:
```
Apache Maven 3.9.x ...
Java version: 17.x.x ...
```

---

### 4.3 — Install MySQL on Windows

1. Go to: **https://dev.mysql.com/downloads/installer/**
2. Download **MySQL Installer for Windows** (the ~450 MB full installer)
3. Run the installer:
   - Choose **"Developer Default"** setup type → Next
   - Click **Execute** to install all components → wait (takes a few minutes)
   - On the **Accounts and Roles** screen:
     - Set a **root password** — write it down! (e.g., `Root@12345`)
   - Continue clicking **Next** and **Finish** until complete
4. After installation, open **MySQL Workbench** (it was installed automatically) to confirm MySQL is running.

Alternatively, verify via Command Prompt:
```cmd
mysql -u root -p
```
Type your root password when prompted. You should see the `mysql>` prompt.

> If `mysql` is not recognized, add MySQL to PATH:
> Add `C:\Program Files\MySQL\MySQL Server 8.0\bin` to System PATH (same steps as Maven above).

---

### 4.4 — Create the Database

**Option A — Using the SQL script (Command Prompt):**

```cmd
cd "c:\Users\admin\Desktop\Intern\MH Solution\test_self_web\grading-system"
mysql -u root -p < init-db.sql
```
Type your MySQL root password when prompted.

**Option B — Using MySQL Workbench:**

1. Open **MySQL Workbench** → connect to `localhost` with `root`
2. Click the **SQL** tab (lightning bolt icon or File → New Query Tab)
3. Paste and run:

```sql
CREATE DATABASE IF NOT EXISTS grading_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'grading_user'@'localhost'
    IDENTIFIED BY 'db_password_here';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX
    ON grading_db.* TO 'grading_user'@'localhost';

FLUSH PRIVILEGES;
```

4. Click the **lightning bolt** (⚡) to run. You should see green checkmarks.

> **Remember:** Replace `db_password_here` above with a real password you choose — you'll use the same password in the next step.

---

### 4.5 — Configure the Application

Open this file in any text editor (Notepad, VS Code, etc.):

```
c:\Users\admin\Desktop\Intern\MH Solution\test_self_web\grading-system\src\main\resources\application.properties
```

Find these lines and update them:

```properties
# Change 'your_mysql_password' to the ROOT password you set during MySQL install,
# OR use 'db_password_here' if you created the grading_user above.

# If using root (simpler for local testing):
spring.datasource.username=root
spring.datasource.password=Root@12345        ← your actual MySQL root password

# If you created grading_user:
# spring.datasource.username=grading_user
# spring.datasource.password=db_password_here

# Upload directory — this folder will be created automatically on Windows:
app.upload.dir=C:/uploads/grading-system
```

**Create the upload directory manually:**

Open Command Prompt and run:
```cmd
mkdir C:\uploads\grading-system
```

> This folder stores uploaded assignment files. It is intentionally **outside** the project folder for security.

---

### 4.6 — Build the JAR File

The `target/` folder and `.jar` file **do not exist yet** — Maven creates them when you build.

Open **Command Prompt**, navigate to the project:

```cmd
cd "c:\Users\admin\Desktop\Intern\MH Solution\test_self_web\grading-system"
```

Run the build command:

```cmd
mvn clean package -DskipTests
```

**What this does, step by step:**
- `clean` → deletes old build files
- `package` → downloads all libraries from the internet (first time only, ~2–5 minutes), compiles your Java code, and packages everything into a single executable `.jar` file
- `-DskipTests` → skips running unit tests (faster for first build)

**Expected output (at the end):**
```
[INFO] BUILD SUCCESS
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1:23 min
```

After this, you will see the `target/` folder created with the JAR inside:
```
grading-system/target/grading-system-1.0.0.jar   ← this is the runnable app
```

> **If you see `BUILD FAILURE`:**
> - Check your internet connection (Maven downloads libraries from the internet)
> - Make sure Java 17 is installed: `java -version`
> - Make sure Maven is installed: `mvn -version`
> - Read the red `[ERROR]` lines in the output — they tell you exactly what's wrong

---

### 4.7 — Run the Application and Test in Browser

**Step 1 — Start the app:**

In Command Prompt (still in the `grading-system` folder):

```cmd
java -jar target\grading-system-1.0.0.jar
```

> 💡 **Alternative (during development — no need to rebuild JAR every time):**
> ```cmd
> mvn spring-boot:run
> ```
> This is faster for development. You only need to run `mvn package` when you want a deployable JAR.

**Step 2 — Wait for startup:**

Watch the console output. When you see a line like this, the app is ready:
```
Started GradingSystemApplication in 4.321 seconds
```

**Step 3 — Open in browser:**

Open your browser (Chrome, Edge, Firefox) and go to:

```
http://localhost:8080
```

You will be redirected to the **login page**.

---

### 4.8 — First Login & Testing

**Default Admin Account** (created automatically on first startup):
| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `Admin@123456` |

> ⚠️ **Change this password** via Admin → Manage Users → Edit after your first login!

**Recommended test sequence:**

1. **Login as admin** → `http://localhost:8080/login`
   - Username: `admin`, Password: `Admin@123456`
   - You are redirected to `/admin/dashboard`

2. **Register an Applicant** → Click logout, go to `http://localhost:8080/register`
   - Choose role: **Applicant**
   - Login as the Applicant → go to `http://localhost:8080/applicant/dashboard`
   - Click **Upload New File** → upload a PDF or DOCX

3. **Register a Grader** → Logout, register again with role: **Grader**
   - Login as Grader → `http://localhost:8080/grader/dashboard`
   - You should see the assignment uploaded by the Applicant
   - Click **Grade** → enter a score and comment → Submit

4. **Login as Applicant again** → Go to dashboard
   - You should see your file now shows the score and comment from the Grader

5. **Test IDOR protection:**
   - While logged in as **Applicant A**, try visiting:  
     `http://localhost:8080/applicant/download/1` (or another ID)
   - You should only be able to download files you uploaded yourself
   - Accessing another user's file ID returns HTTP 404

6. **Admin CRUD:**
   - Login as `admin` → `/admin/users`
   - Edit a user, change their role, disable their account

---

### 4.9 — Test the Static Site Locally

The static company site (`mh-solution-static/index.html`) is a plain HTML file.

Simply open it directly in your browser — no server needed:

1. Open File Explorer → navigate to:  
   `c:\Users\admin\Desktop\Intern\MH Solution\test_self_web\mh-solution-static\`
2. Double-click `index.html`

It opens in your browser. The **"Access Grading System"** button links to `http://xyz.com` — this will only work once Nginx + VM are set up. For local testing, manually go to `http://localhost:8080`.

---

## 5. Deploy to Linux VM with Nginx

> Only do this after you've successfully tested on localhost (Section 4).

### Prerequisites on the VM
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk maven mysql-server nginx
```

Verify:
```bash
java -version    # should show 17.x.x
mvn -version
mysql --version
nginx -version
```

### MySQL Setup on VM
```bash
sudo systemctl start mysql
sudo systemctl enable mysql
sudo mysql_secure_installation   # set root password

# Run the DB init script
cd /path/to/test_self_web/grading-system
sudo mysql -u root -p < init-db.sql
```

### Build on the VM (or copy the pre-built JAR)

**Option A — Build on VM:**
```bash
cd /path/to/test_self_web/grading-system
mvn clean package -DskipTests
```

**Option B — Copy JAR from Windows:**  
On your Windows machine, the JAR is at:
```
grading-system\target\grading-system-1.0.0.jar
```
Copy it to the VM using WinSCP, FileZilla, or `scp`:
```bash
scp "grading-system-1.0.0.jar" user@<VM_IP>:/home/user/grading-system.jar
```

### Create upload directory on VM
```bash
sudo mkdir -p /uploads/grading-system
sudo chown -R $USER:$USER /uploads/grading-system
```

### Update application.properties for VM
Change the upload path:
```properties
app.upload.dir=/uploads/grading-system
```

### Deploy static site
```bash
sudo cp -r /path/to/test_self_web/mh-solution-static /var/www/
```

### Configure Nginx
```bash
sudo cp /path/to/test_self_web/nginx/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t          # test config syntax
sudo systemctl reload nginx
sudo systemctl enable nginx
```

### Run the Spring Boot JAR
```bash
# Run in foreground (for testing):
java -jar grading-system-1.0.0.jar

# Run in background (for production):
nohup java -jar grading-system-1.0.0.jar > app.log 2>&1 &
```

Or as a systemd service (recommended for production):
```bash
sudo nano /etc/systemd/system/grading.service
```
Paste:
```ini
[Unit]
Description=MH Solution Grading System
After=network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -jar /home/ubuntu/grading-system-1.0.0.jar
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```
Then:
```bash
sudo systemctl daemon-reload
sudo systemctl start grading
sudo systemctl enable grading
sudo systemctl status grading   # should show "active (running)"
```

### Configure /etc/hosts on your Windows machine (for domain testing)
Add to `C:\Windows\System32\drivers\etc\hosts` **(as Administrator)**:
```
<VM_IP_ADDRESS>   abc.com
<VM_IP_ADDRESS>   xyz.com
```
Replace `<VM_IP_ADDRESS>` with your VM's actual IP (e.g., `192.168.56.101`).

Find the VM's IP with:
```bash
ip addr show   # on the VM
```

Then open:
- `http://abc.com` → MH Solution landing page
- `http://xyz.com` → Grading System login

---

## 6. Spring Boot Key Concepts for Beginners

| Concept | File | What It Does |
|---------|------|--------------|
| `@SpringBootApplication` | `GradingSystemApplication.java` | Starts the entire application |
| `@Entity` + `@Table` | `User.java`, `Assignment.java` | Maps a Java class to a MySQL table |
| `JpaRepository` | `UserRepository.java` | Generates SQL queries from method names automatically |
| `@Service` | `UserServiceImpl.java` | Contains business logic (not web, not DB — the "middle" layer) |
| `@Controller` | `AuthController.java` etc. | Handles HTTP requests, returns HTML page names |
| `@Configuration` | `SecurityConfig.java` | Tells Spring "here are my app settings as Java code" |
| `SecurityFilterChain` | `SecurityConfig.java` | Defines who can access which URL |
| `@PreAuthorize` | `AdminController.java` | Double-checks role BEFORE the method runs |
| `th:each`, `th:if` | HTML templates | Thymeleaf: loops and conditions in HTML |
| `@Valid` + `BindingResult` | All POST controllers | Validates form input automatically |
| `@Transactional` | Service layer | Wraps DB operations: if one fails, ALL are rolled back |
| `BCryptPasswordEncoder` | `SecurityConfig.java` | Hashes passwords so they're never stored as plain text |

---

## 8. Maintenance & Configuration Changes

### How to apply changes to `application.properties`
If you modify your database password, upload directory, or server port:

1.  **Stop the app:** Press `Ctrl + C` in your Command Prompt.
2.  **Rebuild (if using JAR):** Run `mvn clean package -DskipTests`. 
    *   *Note: The `clean` command automatically deletes your old `target/` folder so you don't have to do it manually.*
3.  **Restart:** Run `java -jar target/grading-system-1.0.0.jar`.

> **Tip:** During development, use `mvn spring-boot:run`. You still need to restart the app after changes, but you don't need to wait for the full `package` process.

---

## 9. Common Issues & Fixes

| Problem | Cause | Fix |
|---------|-------|-----|
| `java: command not found` | Java not installed or not in PATH | Install JDK 17, restart Command Prompt |
| `mvn: command not found` | Maven not in PATH | Add Maven `bin` folder to System PATH, restart Command Prompt |
| `BUILD FAILURE` during `mvn package` | Missing dependency download | Check internet connection, try `mvn clean package -DskipTests` again |
| `Access denied for user 'root'@'localhost'` | Wrong MySQL password in `application.properties` | Update `spring.datasource.password` to your actual password |
| `Communications link failure` (MySQL) | MySQL not running | Start MySQL: open Services → find MySQL → Start |
| App starts but browser shows error | Port 8080 in use | Change `server.port=8081` in `application.properties`, visit `localhost:8081` |
| Upload fails with "No such file" | Upload directory doesn't exist | Run: `mkdir C:\uploads\grading-system` |
| Login works but shows wrong page | Old session cookie | Clear browser cookies (Ctrl+Shift+Del) and log in again |
| `Connection refused` on VM | Spring Boot not running | Check `sudo systemctl status grading` or run JAR manually |
| `nginx: [emerg] bind() to 80 failed` | Port 80 in use on VM | Run `sudo systemctl stop apache2` or change Nginx port |
| `abc.com` / `xyz.com` not loading | Hosts file not edited | Edit `C:\Windows\System32\drivers\etc\hosts` **as Administrator** |
