<div align="center">

<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
<img src="https://img.shields.io/badge/JavaFX-21-0096C8?style=for-the-badge&logo=java&logoColor=white" />
<img src="https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white" />
<img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" />
<img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />

# 🐷 Piggy Pro — Personal Expense Tracker

**A desktop application for smart, visual personal finance management**

*Built with JavaFX 21 · Graphic Era University — Project T-131*

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Running the Application](#-running-the-application)
- [Team](#-team)
- [Architecture](#-architecture)
- [License](#-license)

---

## 🔍 Overview

**Piggy Pro** is a full-featured personal expense tracker built as a desktop application using JavaFX 21. It allows users to securely log in, manage income and expenses, set budgets, visualise spending trends with charts, and export reports in both PDF and Excel formats.

The project was developed by **Team Tech Tonics** (Project ID: T-131) as part of the curriculum at **Graphic Era University, Dehradun**.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 **Secure Authentication** | BCrypt-hashed password login and registration |
| 📊 **Interactive Dashboard** | At-a-glance summary of balance, income, and expenses |
| 💸 **Transaction Management** | Add, edit, delete, and filter income/expense transactions |
| 🏷️ **Category Management** | Organise transactions with custom categories |
| 📈 **Analytics & Charts** | Visualise spending with bar, pie, and line charts rendered on Canvas |
| 🎯 **Budget Tracking** | Set monthly budgets per category with progress indicators |
| 📄 **Report Export** | Export detailed reports as **PDF** (iText) or **Excel** (Apache POI) |
| 🔄 **Smooth Transitions** | Fade-in/out scene transitions managed via `SceneManager` singleton |
| 🗄️ **Local Database** | All data stored locally in an embedded SQLite database |

---

## 🖥️ Screenshots

> *Screenshots will be added here. Run the app locally to see all screens.*

The application includes the following screens:
- **Login / Registration** — Secure entry point
- **Dashboard** — Financial summary and recent transactions
- **Transactions** — Full transaction ledger with filters
- **Analytics** — Spending charts and trend visualisations
- **Budgets** — Category-based budget management
- **Reports** — Date-range reports with export options

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **UI Framework** | JavaFX 21 (FXML + CSS) |
| **Language** | Java 21 |
| **Database** | SQLite (via `sqlite-jdbc`) |
| **Password Hashing** | jBCrypt |
| **PDF Export** | iText |
| **Excel Export** | Apache POI |
| **Logging** | SLF4J 1.7.36 |
| **Build Tool** | Apache Maven |

---

## 📁 Project Structure

```
Piggy-Pro/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/piggy/pro/
│       │       ├── controllers/        # FXML controllers for all 6 screens
│       │       ├── dao/                # Data Access Objects (SQLite queries)
│       │       ├── models/             # Entity classes (Transaction, Budget, User, etc.)
│       │       ├── services/           # Business logic layer
│       │       ├── utils/              # Helpers, exporters (PDF/Excel), chart utils
│       │       ├── SceneManager.java   # Singleton: scene switching with fade transitions
│       │       └── SessionManager.java # Singleton: logged-in user session state
│       └── resources/
│           └── com/piggy/pro/
│               ├── fxml/               # FXML layout files for each screen
│               ├── css/                # Unified application stylesheet
│               └── icons/              # Application icons and assets
├── piggy_pro.db                        # SQLite database (auto-created on first run)
├── pom.xml                             # Maven build configuration
└── README.md
```

---

## ✅ Prerequisites

Before running Piggy Pro, make sure you have the following installed:

- **Java Development Kit (JDK) 21** — [Download here](https://adoptium.net/)
- **Apache Maven 3.8+** — [Download here](https://maven.apache.org/download.cgi)
- **Git** — [Download here](https://git-scm.com/)

> ⚠️ **Important:** Do **not** use IntelliJ IDEA's green Run button to launch the app. Use `mvn javafx:run` as described below, or the JavaFX modules will not be resolved correctly.

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/vinayak-1010/Piggy-Pro.git
cd Piggy-Pro
```

### 2. Verify Your Java Version

```bash
java -version
# Should output: openjdk version "21.x.x" or similar
```

### 3. Build the Project

```bash
mvn clean install -DskipTests
```

This will download all dependencies (JavaFX, SQLite, BCrypt, iText, POI, SLF4J) from Maven Central.

---

## ▶️ Running the Application

```bash
mvn javafx:run
```

On first launch, the SQLite database (`piggy_pro.db`) will be automatically created in the project root directory with all required tables.

**Register** a new account on the Login screen to get started.

---

## 🔧 Common Issues & Fixes

| Problem | Fix |
|---|---|
| `StaticLoggerBinder` warnings at startup | Expected — SLF4J is pinned to 1.7.36 for Apache POI compatibility. Warnings are harmless. |
| `UnsatisfiedLinkError` for JavaFX | Ensure you're running via `mvn javafx:run`, not directly via `java` or IntelliJ's run button. |
| Database not found / schema errors | Delete `piggy_pro.db` and re-run — it will be recreated fresh. |
| PDF / Excel export fails | Verify `iText` and `Apache POI` dependencies resolved correctly via `mvn dependency:tree`. |

---

## 👥 Team

**Team Tech Tonics** — Project ID: T-131
Graphic Era University, Dehradun

| Member | Role & Contributions |
|---|---|
| **Vinayak Singh** *(Team Lead)* | UI/UX design, all screen controllers, SceneManager & SessionManager, unified CSS stylesheet, Maven build config |
| **Satyam Sethi** | Core backend architecture, authentication module (BCrypt), SQLite schema design |
| **Sanchit Khajuria** | Expense & category module, transaction DAOs and services |
| **Utkarsh Gunwant Joshi** | Reports module, analytics charts (Canvas), PDF & Excel export |

---

## 🏗️ Architecture

Piggy Pro follows a **layered MVC architecture**:

```
┌─────────────────────────────────────────┐
│              JavaFX Views               │
│         (FXML + CSS Stylesheet)         │
├─────────────────────────────────────────┤
│             Controllers                 │
│    (One per screen, handle UI logic)    │
├─────────────────────────────────────────┤
│              Services                   │
│       (Business logic layer)            │
├─────────────────────────────────────────┤
│            DAOs (Data Access)           │
│       (SQL queries via JDBC)            │
├─────────────────────────────────────────┤
│          SQLite Database                │
│         (piggy_pro.db)                  │
└─────────────────────────────────────────┘

Singletons:
  SceneManager   — manages screen navigation with fade transitions
  SessionManager — holds the authenticated user's session state
```

Key design decisions:
- **No `module-info.java`** — omitted to maintain compatibility with non-modular JARs (jBCrypt, iText, POI).
- **Charts drawn on `Canvas`** — not `Region` subclasses, for full rendering control.
- **Icons loaded via `getClass().getResource()`** with silent fail-safe to handle missing assets gracefully.
- **SLF4J pinned to `1.7.36`** — prevents `StaticLoggerBinder` conflicts introduced by SLF4J 2.x with Apache POI.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by Team Tech Tonics · Graphic Era University

</div>
