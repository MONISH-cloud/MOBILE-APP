# PGManager 🏠

PGManager is a modern Android application designed to simplify and automate Paying Guest (PG) and hostel management operations. The app provides separate portals for Owners, Managers, and Tenants with real-time synchronization using Firebase.

Built using **Java**, **Android Studio**, and **Firebase**, PGManager helps reduce manual paperwork by digitizing rent tracking, room allocation, complaint handling, expense management, and communication.

---

# 📱 Features

## 🔐 Authentication & Role-Based Access

* Secure Firebase Authentication
* Role-based login system:

  * Owner
  * Manager
  * Tenant
* Separate dashboards for admins and tenants
* Phone-number-based login workflow

---

## 🏢 PG & Property Management

* Add and manage multiple PGs
* Store:

  * PG name
  * Address
  * Default rent
  * Property images
* Real-time occupancy tracking
* Automatic bed status updates

---

## 🛏️ Room & Bed Allocation

* Create floors and rooms
* Add multiple beds
* Allocate beds to tenants
* Track:

  * Vacant beds
  * Occupied beds
* Atomic room allocation using Firestore WriteBatch

---

## 👨‍🎓 Tenant Management

* Add/Edit/Delete tenants
* Manage tenant lifecycle:

  * Move-in
  * Active stay
  * Notice period
  * Move-out
* Store:

  * Phone number
  * Joining date
  * Rent amount
  * Assigned room

---

## 💰 Rent Management

* Automated monthly rent generation
* Overdue payment detection
* Mark rent as paid
* Real-time rent updates
* WhatsApp rent reminders

---

## 🛠️ Complaint Management

* Raise maintenance requests
* Complaint categories:

  * Plumbing
  * Electrical
  * AC/Fan
  * Water
* Status updates:

  * Pending
  * In Progress
  * Resolved
* Priority levels:

  * Low
  * Normal
  * Urgent

---

## 📊 Expense Tracking

* Add property expenses
* Expense categories:

  * Repairs
  * Electricity
  * Salary
  * Maintenance
* Monthly expense overview
* Financial summaries

---

## 🤖 AI Chatbot Assistant

* Smart assistant for owners
* Quick navigation commands
* Interactive chip-based actions
* Natural language interaction

---

## 📡 Real-Time Synchronization

* Firebase Firestore integration
* Live updates using Snapshot Listeners
* Offline persistence support
* Instant synchronization across users

---

# 🛠️ Tech Stack

## Frontend

* Java
* Android Studio
* XML Layouts
* Material Design 3
* ViewBinding

## Backend

* Firebase Firestore
* Firebase Authentication
* Firebase Cloud Functions

## Libraries & APIs

* Glide 4.16
* MPAndroidChart
* WhatsApp Intent Integration

---

# 🏗️ System Architecture

```text
Android Application (Java)
          |
          v
Firebase Authentication
          |
          v
Firebase Firestore
          |
          v
Firebase Cloud Functions
```

---

# 📂 Project Structure

```text
PGManager/
│
├── app/
│   ├── java/com/pgmanager/
│   │   ├── activities/
│   │   ├── fragments/
│   │   ├── adapters/
│   │   ├── models/
│   │   ├── firebase/
│   │   ├── chatbot/
│   │   └── utils/
│   │
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── mipmap/
│   │
│   └── AndroidManifest.xml
│
├── functions/
│   ├── index.js
│   └── package.json
│
├── gradle/
├── build.gradle
└── README.md
```

---

# 🗄️ Database Design

## Main Firestore Collections

### users/

Stores:

* uid
* name
* phone
* role

### pgs/

Stores:

* pgId
* name
* address
* defaultRent

### rooms/

Stores:

* roomId
* floor
* totalBeds

### beds/

Stores:

* bedId
* status
* tenantId

### tenants/

Stores:

* tenantId
* roomNumber
* rentAmount
* tenantStatus

### rentRecords/

Stores:

* amount
* dueDate
* paymentStatus

### complaints/

Stores:

* category
* priority
* status
* description

---

# ⚙️ Installation Guide

## Prerequisites

Make sure you have installed:

* Android Studio
* Java JDK
* Firebase Project
* Android SDK 34+

---

## 🚀 Setup Instructions

### 1. Clone Repository

```bash
git clone https://github.com/your-username/PGManager.git
```

---

### 2. Open in Android Studio

* Launch Android Studio
* Click **Open Existing Project**
* Select the PGManager folder

---

### 3. Configure Firebase

Add the following Firebase services:

* Firebase Authentication
* Cloud Firestore
* Firebase Cloud Functions

Download and place:

```text
google-services.json
```

inside:

```text
app/
```

---

### 4. Sync Gradle

Click:

```text
Sync Project with Gradle Files
```

---

### 5. Run Application

* Connect Android device or emulator
* Click Run ▶️

---

# 🔥 Firebase Features Used

| Firebase Service   | Purpose                |
| ------------------ | ---------------------- |
| Firebase Auth      | Login & Authentication |
| Firestore          | Real-time Database     |
| Cloud Functions    | Backend Automation     |
| Snapshot Listeners | Live Updates           |

---

# 📦 Main Modules

## Authentication Module

Handles:

* Login
* Registration
* Role validation

## Owner Dashboard

Displays:

* Rent summaries
* Occupancy percentage
* Overdue records

## Tenant Portal

Provides:

* Stay details
* Rent history
* Complaint tracking

## Complaint System

Allows:

* Complaint creation
* Status tracking
* Real-time updates

## Expense Tracker

Tracks:

* Daily expenses
* Monthly reports
* Property profitability

---

# 📸 Screens Included

* Login Screen
* Dashboard Screen
* PG List Screen
* Add Tenant Screen
* Rent Dashboard
* Complaint Management
* Tenant Portal
* AI Chatbot Assistant

---

# 🧪 Testing

## Test Cases

| Test Case        | Expected Result                   |
| ---------------- | --------------------------------- |
| Valid Login      | User redirected to correct portal |
| Invalid Password | Access denied                     |
| Add Tenant       | Tenant added successfully         |
| Mark Rent Paid   | Status updated instantly          |
| Raise Complaint  | Complaint stored in Firestore     |
| Add Room         | Room & beds created               |

---

# 🚀 Performance Features

* Real-time Firestore listeners
* Offline persistence support
* Fast UI rendering
* Smooth fragment navigation
* Cloud automation using Firebase Functions

---

# 🔒 Security Features

* Role-based access control
* Scoped Firestore queries
* Owner-specific data isolation
* Atomic Firestore transactions

---

# ⚠️ Current Limitations

* Manager permissions are not fully separated
* No Firebase Cloud Messaging notifications yet
* Firestore whereIn query limitation for large datasets

---

# 🔮 Future Enhancements

## Planned Features

* Online Payment Gateway
* UPI Integration
* Firebase Cloud Messaging (FCM)
* AI Analytics Dashboard
* Biometric Login
* QR Visitor Management
* Advanced Reports & Charts

---

# 👨‍💻 Contributors

* Monish R
* Omkar Suresh Naik
* Pradeep M Doddakaragi
* Nikhil Sridara
* Manoj D
* Pasumarty Krishna Tanish

---

# 📚 References

1. Android Developer Documentation
2. Firebase Documentation
3. Material Design Guidelines
4. Glide Documentation
5. MPAndroidChart Documentation
6. Firebase Cloud Functions Documentation

---


**Mobile App Development Essentials (CS2306)**
RV University, Bengaluru
