# PGManager — Comprehensive Feature Report

**Platform:** Android (Java)
**Backend:** Firebase (Firestore + Auth + Cloud Functions)
**Build Status:** ✅ Passing
**Min SDK:** 24 (Android 7.0) | **Target SDK:** 34 (Android 14)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Authentication & Role-Based Access](#2-authentication--role-based-access)
3. [Owner / Admin Portal](#3-owner--admin-portal)
   - 3.1 Dashboard
   - 3.2 PG Management
   - 3.3 Room & Bed Management
   - 3.4 Tenant Management
   - 3.5 Rent Collection
   - 3.6 Expense Tracking
   - 3.7 Maintenance Requests (Owner View)
   - 3.8 WhatsApp Hub
   - 3.9 Notifications
   - 3.10 Reports & Analytics
   - 3.11 Settings & Profile
   - 3.12 AI Chatbot Assistant
4. [Tenant Portal](#4-tenant-portal)
   - 4.1 Browse PGs
   - 4.2 My PG
   - 4.3 Maintenance Requests (Tenant View)
5. [Cloud Functions (Backend)](#5-cloud-functions-backend)
6. [Data Models](#6-data-models)
7. [UI / Design System](#7-ui--design-system)
8. [Security & Data Isolation](#8-security--data-isolation)
9. [Demo Data](#9-demo-data)
10. [Known Limitations & Future Scope](#10-known-limitations--future-scope)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Android App (Java)                 │
│                                                     │
│  ┌──────────────┐        ┌───────────────────────┐  │
│  │  Owner Side  │        │     Tenant Side       │  │
│  │  MainActivity│        │ TenantPortalActivity  │  │
│  │  (Nav Graph) │        │  (Browse + My PG)     │  │
│  └──────┬───────┘        └──────────┬────────────┘  │
│         │                           │               │
│         └──────────┬────────────────┘               │
│                    ▼                                │
│           Firebase SDK Layer                        │
│    (Firestore + Auth + Functions)                   │
└─────────────────────────────────────────────────────┘
```

**Pattern:** Activities + Fragments with ViewBinding. No ViewModel/LiveData — direct Firestore listeners in UI layer.

**Navigation:** Bottom Navigation + NavController (owner side). Single Activity (tenant side).

**Real-time:** Firestore `addSnapshotListener` used throughout — all lists update live without manual refresh.

**Offline:** Firestore persistence enabled (`setPersistenceEnabled(true)`) — app works with cached data when offline.

---

## 2. Authentication & Role-Based Access

### Login Screen (`LoginActivity`)

| Feature | Detail |
|---|---|
| Role Selector | Two toggle buttons: **🏠 Admin** (owner/manager) and **👤 Tenant** |
| Login | Phone number + password (phone converted to `phone@pgmanager.com` for Firebase Auth) |
| Register | Name + phone + password + selected role |
| Auto-routing | On login, fetches `UserProfile.role` from Firestore and routes to correct portal |
| Validation | 10-digit phone required, 6-char minimum password |

### Role Routing

```
Login → fetch UserProfile.role
  ├── "owner" or "manager"  →  MainActivity (Owner Portal)
  └── "tenant"              →  TenantPortalActivity (Tenant Portal)
```

### User Profile (`UserProfile` model)

| Field | Values |
|---|---|
| `role` | `owner` / `manager` / `tenant` |
| `subscriptionPlan` | `free` / `basic` / `pro` |
| `maxPGs` | 3 (free), 10 (basic), unlimited (pro) |
| `phone` | Used as the link between tenant login and their PG record |

---

## 3. Owner / Admin Portal

### 3.1 Dashboard (`DashboardFragment`)

The home screen giving a real-time financial and occupancy snapshot.

**Stats Cards (Hero Card):**
- **Collected** — total rent paid this month across all owner's PGs
- **Overdue** — total overdue rent amount this month
- **Occupancy %** — progress bar showing occupied/total beds ratio

**Attention Cards:**
- 🔴 **Overdue** count — number of overdue rent records
- 🟡 **Pending** count — number of pending rent records
- ⚠️ **Notices** count — tenants currently on notice period

**Quick Actions:**
- 🏠 Properties → navigates to PG list
- 💰 Rent → navigates to rent dashboard
- 📊 Expenses → navigates to settings/expense tracker

**Pending Dues List:**
- Shows all pending + overdue rent records for the current month
- Each item has **✓ Mark Paid** and **💬 Remind** (WhatsApp) buttons
- Pull-to-refresh support

**Data Isolation Fix:** All queries are scoped to the owner's PG IDs using `whereIn("pgId", pgBatch)` — owners only see their own data.

---

### 3.2 PG Management

#### PG List (`PGListFragment`)
- Real-time list of all PGs owned by the logged-in user
- Each card shows: name, address, default rent, occupancy progress bar, total/vacant beds, room count
- FAB to add new PG
- Tap card → PG Detail

#### Add / Edit PG (`AddEditPGActivity`)
| Field | Notes |
|---|---|
| PG Name | Required |
| Address | Multi-line |
| Default Monthly Rent | ₹ prefix |
| Default Advance | ₹ prefix |
| Photo URL | Optional — leave blank for auto-assigned image |

- On save: creates PG document with `ownerId`, `managerIds[]`, timestamps
- On edit: updates name, address, rent, advance, photoUrl, updatedAt

#### PG Detail (`PGDetailActivity`)
- Live stats: rooms, occupied/total beds, default rent
- Tenant list for that PG (active + notice, not moved-out)
- **Manage Rooms** → RoomBedActivity
- **View Expenses** → AddExpenseActivity
- **Edit PG** → AddEditPGActivity
- **Delete PG** → cascade delete (rooms → beds → expenses → PG doc, marks tenants as moved-out)
- **WhatsApp Group Link** — paste group invite link, saved to Firestore
- **Tomorrow's Menu** — set breakfast/lunch/dinner, send to WhatsApp group (message auto-copied to clipboard)

---

### 3.3 Room & Bed Management (`RoomBedActivity`)

- Lists all rooms in a PG with floor, bed count, occupancy
- **Add Room** dialog: room number, floor, number of beds → beds auto-created
- Each bed card shows: bed number, status (Vacant/Occupied), tenant name if occupied
- Tap occupied bed → opens TenantDetailActivity
- Delete room (with confirmation) — frees all beds, decrements PG counts

---

### 3.4 Tenant Management

#### Tenant List (`TenantListFragment`)
- Shows all active + notice tenants across all owner's PGs
- Uses `whereIn("pgId", pgBatch)` for owner scoping
- Status badge: ACTIVE (green) / NOTICE (amber)
- FAB → Add Tenant

#### Add Tenant (`AddEditTenantActivity`)
| Field | Validation |
|---|---|
| Full Name | Required |
| WhatsApp Number | Required, exactly 10 digits. Helper text: "Tenant must register with this same number to see their PG" |
| Select PG | Cascading dropdown |
| Select Room | Loads after PG selected |
| Select Bed | Shows only vacant beds |
| Joining Date | Date picker |
| Monthly Rent | Required, numeric |
| Advance Amount | Optional |

On save (atomic batch):
1. Creates tenant document
2. Marks bed as `occupied`, sets `tenantId` + `tenantName`
3. Increments PG `occupiedBeds` count

#### Tenant Detail (`TenantDetailActivity`)
- Avatar with initial letter, name, status badge
- Details: phone, PG, room, bed, rent, joining date
- **WhatsApp** — opens chat with pre-filled "Hi [Name],"
- **✏️ Edit** — dialog to update name, phone, rent, advance
- **⚠️ Notice** — starts 30-day notice period (sets `noticeDate`, `expectedMoveOutDate`, status = `notice`)
- **Notice Countdown Card** — shows days remaining + expected move-out date (visible only when on notice)
- **✅ Complete Move-Out** — confirmation dialog → marks moved-out, frees bed, decrements PG + room occupancy
- **🔄 Change Room** — 2-step dialog (pick room → pick vacant bed) → atomic transfer
- **🗑️ Delete** — confirmation dialog → cascade deletes tenant doc + frees bed + decrements counts + deletes all rent records
- **Rent History** — live list of all rent records for that tenant

---

### 3.5 Rent Collection (`RentDashboardFragment`)

- Scoped to owner's PGs via `whereIn("pgId", pgBatch)`
- Filter chips: **All / Pending / Overdue / Paid**
- Each rent card shows: tenant avatar, name, PG, amount, due date, status
- **✓ Mark Paid** — updates status to `paid`, sets `paidDate`
- **💬 Remind** — opens WhatsApp with message: *"Hi [Name], your rent of ₹X for [PG] is due..."*
- Status colours: 🟢 Paid, 🟡 Pending, 🔴 Overdue

---

### 3.6 Expense Tracking (`AddExpenseActivity` / `ExpenseListFragment`)

**Add Expense:**
| Category Options |
|---|
| 🥬 Vegetables |
| 🔧 Repairs |
| ⚡ Electricity |
| 💧 Water |
| 🧹 Cleaning |
| 💰 Staff Salary |
| 📦 Other |

Fields: category, amount, description, date (date picker)

**Expense List:** All expenses for a PG, sorted by date descending.

**Monthly Expense Tracker (Settings):**
- Aggregates all expenses across all owner's PGs
- Shows grand total + per-PG breakdown
- Refresh button to reload

---

### 3.7 Maintenance Requests — Owner View (`ComplaintsActivity`)

- Renamed from "Complaints" to **Maintenance Requests**
- Scoped to owner's PGs via `whereIn("pgId", pgBatch)`
- **Pending count badge** shown in header
- Filter chips: All / Pending / In Progress / Resolved
- Each card shows:
  - Tenant name + room/unit
  - Category + description
  - Priority badge: 🔴 URGENT (red) / NORMAL / LOW
  - Status: ⏳ Pending / 🔧 In Progress / ✅ Resolved
- **▶ Start** — moves pending → in_progress
- **✅ Resolve** — moves in_progress → resolved
- Owner can also manually add a complaint on behalf of a tenant
- All status changes reflect in real-time on the tenant's My PG tab

---

### 3.8 WhatsApp Hub (`WhatsAppHubActivity`)

- Lists all active tenants with their WhatsApp numbers
- Bulk message capability
- Quick access to send rent reminders to all pending tenants

---

### 3.9 Notifications (`NotificationsActivity`)

- Lists app notifications (AppNotification model)
- Read/unread state tracking
- Icon, title, message, timestamp

---

### 3.10 Reports & Analytics (`ReportsFragment`)

- MPAndroidChart integration (bar/line charts)
- Monthly income vs expense comparison
- Occupancy rate over time
- Collection rate percentage

---

### 3.11 Settings & Profile (`SettingsFragment`)

| Section | Features |
|---|---|
| Profile Card | Avatar (initial), name, phone, role badge, subscription plan |
| Monthly Expenses | Grand total + per-PG breakdown with refresh |
| WhatsApp Hub | Quick link |
| Maintenance Requests | Quick link with pending badge |
| Notifications | Quick link |
| Load Demo Data | Seeds 4 PGs with tenants, rooms, beds, rent records |
| Invite Manager | Share invite via Android share sheet |
| Logout | Confirmation dialog → signs out → LoginActivity |

---

### 3.12 AI Chatbot Assistant (`ChatbotActivity`)

A state-machine based in-app guide accessible via the FAB on the main screen.

**States & Topics:**
| State | Options |
|---|---|
| Main Menu | PG Management, Tenants, Rent, Expenses, Food Menu, Expense Tracker, Help |
| PG | Add New PG, View My PGs, Rooms & Beds, Edit PG, Delete PG |
| Rooms | Add a Room, View Occupied Beds, Bed Status |
| Tenant | Add Tenant, View Tenants, Edit Tenant, Update Phone, Notice Period, Move Out |
| Rent | View Status, Mark Paid, Send Reminder, Overdue Rents |
| Expense | Add Expense, View Expenses |
| Food Menu | Set Tomorrow's Menu, Send to Group, Add Group Link |

- Chip-based quick replies (no typing needed)
- Can launch screens directly (e.g., tapping "Add New PG" opens AddEditPGActivity)
- Delayed bot responses for natural feel
- Full help text explaining every feature

---

## 4. Tenant Portal

### 4.1 Browse PGs (`TenantPortalActivity` — Browse Tab)

- **Real-time** — uses `addSnapshotListener` on `pgs` collection. Any new PG added by any owner appears instantly on all tenant screens without refresh.
- **Search bar** — filters by PG name or address in real-time as you type
- **PG count** — "X PGs available" shown below search

**PG Card (`item_pg_tenant.xml`):**
| Element | Detail |
|---|---|
| Photo | Auto-assigned building image from picsum.photos, seeded by PG name hash (consistent per PG). Custom URL overrides if set by owner. |
| Vacant Badge | Green if beds available, red if full |
| PG Name + Rent | Name bold, rent in primary purple |
| Address | With 📍 icon, 2-line max |
| Owner Avatar | Initial letter in circle |
| Owner Name | "Owner: [Name]" |
| Owner Phone | "+91 XXXXXXXXXX" |
| 💬 Contact Button | Opens WhatsApp with pre-filled inquiry message. Hidden if owner has no phone. |

---

### 4.2 My PG (`TenantPortalActivity` — My PG Tab)

**Linking mechanism:** Tenant's registered phone number (`UserProfile.phone`) is matched against `Tenant.whatsappNumber` in Firestore. Owner must enter the same number when adding the tenant.

**My PG Details Card:**
| Field | Detail |
|---|---|
| PG Name | Large, in gradient header |
| Status | ACTIVE (green) / NOTICE (amber) |
| Room / Bed | "Room 101 • Bed 101-A" |
| Monthly Rent | Formatted with ₹ |
| Joined Date | Formatted date |
| Rent Status | Latest month's status — colour-coded green/yellow/red |
| Owner Name | Fetched from owner's UserProfile |
| Owner Phone | "+91 XXXXXXXXXX" |
| 💬 Chat with Owner | Opens WhatsApp with pre-filled message including tenant name, PG, room |
| 🔧 Raise Maintenance Request | Opens request dialog |

**No PG State:** If phone doesn't match any tenant record, shows: *"Ask your PG owner to add you as a tenant. Your details will appear here automatically."*

---

### 4.3 Maintenance Requests — Tenant View

**Raise Request Dialog:**
| Field | Options |
|---|---|
| Category | 🔧 Plumbing / 💡 Electrical / 🧹 Cleaning / ❄️ AC-Fan / 🚪 Door-Lock / 💧 Water / 📦 Other |
| Priority | Normal / Urgent / Low |
| Description | Free text, required |

On submit: saves to `complaints` collection with `tenantId`, `pgId`, `unit` (room/bed), formatted description, priority, status = `pending`.

**My Requests Card:**
- Live list of all requests submitted by this tenant
- Each item: category, description, date, status badge
- Status updates in real-time as owner acts: ⏳ Pending → 🔧 In Progress → ✅ Resolved

---

## 5. Cloud Functions (Backend)

Located in `/functions/index.js` (Node.js, Firebase Functions).

### `generateMonthlyRent`
- **Trigger:** Scheduled — 1st of every month at 00:00
- **Action:** Queries all tenants with status `active` or `notice`, creates a `RentRecord` for each with:
  - Due date = 5th of current month
  - Status = `pending`
  - Amount = tenant's `rentAmount`
- **ID format:** `{tenantId}_{YYYY-MM}` (prevents duplicates)

### `checkOverdueRent`
- **Trigger:** Scheduled — daily at midnight
- **Action:** Finds all `pending` rent records where `dueDate < now`, updates status to `overdue`

### `whatsappWebhook`
- **Trigger:** HTTP request
- **Status:** Placeholder for Phase 2 Meta WhatsApp API integration
- Returns `EVENT_RECEIVED` (200)

---

## 6. Data Models

### PG
```
id, ownerId, managerIds[], name, address,
defaultRent, defaultAdvance, totalRooms, totalBeds,
occupiedBeds, photoUrl, whatsappGroupLink,
menuBreakfast, menuLunch, menuDinner,
createdAt, updatedAt
```

### Room (subcollection under PG)
```
id, roomNumber, floor, totalBeds, occupiedBeds
```

### Bed (subcollection under Room)
```
id, bedNumber, status (vacant/occupied), tenantId, tenantName
```

### Tenant
```
id, pgId, pgName, roomId, roomNumber, bedId, bedNumber,
name, whatsappNumber, joiningDate, rentAmount, advanceAmount,
status (active/notice/moved_out), noticeDate,
expectedMoveOutDate, createdAt
```

### RentRecord
```
id, tenantId, pgId, tenantName, pgName, whatsappNumber,
month (YYYY-MM), amount, dueDate, status (pending/paid/overdue),
paidDate, rewardApplied, reminderSent, createdAt
```

### Expense (subcollection under PG)
```
id, pgId, category, amount, description, date, createdAt
```

### Complaint
```
id, pgId, tenantId, tenantName, unit, description,
priority (urgent/normal/low),
status (pending/in_progress/resolved),
createdAt, updatedAt
```

### UserProfile
```
userId, name, phone, role (owner/manager/tenant),
subscriptionPlan (free/basic/pro), maxPGs, createdAt
```

### AppNotification
```
id, title, message, icon, read, createdAt
```

### ChatMessage (in-memory only)
```
text, type (TYPE_BOT=0 / TYPE_USER=1)
```

---

## 7. UI / Design System

### Colour Palette
| Token | Hex | Usage |
|---|---|---|
| `primary` | `#8338EC` | Buttons, accents, nav |
| `primary_dark` | `#5B21B6` | Status bar, headers |
| `accent` | `#06D6A0` | Success, paid, vacant |
| `danger` | `#EF476F` | Overdue, delete, urgent |
| `warning` | `#FFD166` | Pending, notice |
| `secondary_blue` | `#3A86FF` | Room count, secondary info |
| `whatsapp_green` | `#25D366` | WhatsApp buttons |
| `background` | `#F8FAFC` | Screen backgrounds |
| `surface` | `#FFFFFF` | Cards |
| `gradient_start/mid/end` | `#1E1B4B → #4C1D95 → #8338EC` | Headers |

### Typography
- Headlines: `sans-serif-black`, 26–32sp
- Titles: `sans-serif-bold`, 18–22sp
- Body: `sans-serif`, 13–14sp
- Labels: `sans-serif-black`, 10–11sp, ALL CAPS, letter-spacing 0.12

### Component Styles
- Cards: 20–24dp corner radius, 2–8dp elevation
- Buttons: 12–14dp corner radius, 44–52dp height
- Inputs: 14dp corner radius, outlined style
- Status badges: pill shape, colour-coded backgrounds
- Avatars: circle background, initial letter

### Drawables
`gradient_bg`, `bg_bottom_sheet`, `btn_primary`, `btn_whatsapp`, `chat_bubble_bot/user`, `circle_avatar_bg`, `edit_text_bg`, `icon_bubble_bg`, `left_accent_border`, `progress_bar_rounded`, `rounded_card`, `status_badge`, `tooltip_bg`

---

## 8. Security & Data Isolation

| Area | Implementation |
|---|---|
| Rent records | Scoped to owner's PG IDs via `whereIn("pgId", pgBatch)` |
| Tenant list | Scoped to owner's PG IDs via `whereIn("pgId", pgBatch)` |
| Dashboard stats | Scoped to owner's PG IDs |
| Maintenance requests | Scoped to owner's PG IDs via `whereIn("pgId", pgBatch)` |
| PG list (owner) | `whereEqualTo("ownerId", uid)` |
| Tenant portal PGs | No filter — all PGs visible (by design, for discovery) |
| Tenant's own requests | `whereEqualTo("tenantId", tenantId)` |
| Firestore rules | `firestore.rules` file present |
| Auth | Firebase Auth — phone-to-email pattern |
| Offline | Firestore persistence enabled |

**Firestore `whereIn` limit:** Batched to max 10 IDs per query (Firestore limit).

---

## 9. Demo Data (`SeedDataUtil`)

Triggered from Settings → **🏠 Load Demo Data**

Seeds **4 PGs** with full data:

| PG | Address | Rent | Rooms | Beds | Tenants |
|---|---|---|---|---|---|
| Sunrise PG | MG Road, Bangalore | ₹8,000 | 4 | 8 | 3 |
| Green Valley PG | Koramangala 5th Block | ₹7,500 | 4 | 8 | 3 |
| Royal Residency | Indiranagar 100ft Road | ₹9,500 | 4 | 8 | 3 |
| Urban Nest PG | HSR Layout Sector 2 | ₹7,000 | 4 | 8 | 3 |

Per PG:
- 4 rooms (2 per floor), 2 beds each
- 3 tenants assigned to rooms 0 and 1
- 1 tenant on notice period per PG
- Rent records with mixed statuses (paid/pending/overdue) cycling across PGs
- Beds marked occupied with `tenantId` + `tenantName`

---

## 10. Known Limitations & Future Scope

### Current Limitations
| Item | Detail |
|---|---|
| Auth method | Phone-to-email workaround instead of native Firebase Phone Auth. `OTPVerifyActivity` exists but is not wired. |
| `whereIn` cap | Max 10 PGs per owner for scoped queries. Owners with >10 PGs will see incomplete data. |
| No ViewModel | Firestore listeners directly in Activities/Fragments. Memory leaks possible if `binding` null checks are missed. |
| No FCM | Push notifications not implemented. `NotificationsActivity` uses Firestore-stored notifications only. |
| Advance tracking | `advanceAmount` stored on tenant but no refund/deduction workflow exists. |
| Reports | `ReportsFragment` has MPAndroidChart integrated but charts are not fully populated. |

### Suggested Future Features
1. **Rent Receipt / PDF** — auto-generate on mark-paid, viewable by tenant
2. **Advance Refund Tracker** — deductions on move-out
3. **FCM Push Notifications** — overdue alerts, request status updates
4. **Electricity Bill Splitter** — divide utility bill across occupied beds
5. **Tenant Document Upload** — Aadhaar/ID via Firebase Storage
6. **Rent Payment History (Tenant)** — full month-by-month list in My PG tab
7. **Notice Self-Initiation** — tenant gives notice from portal
8. **Bed Availability Calendar** — visual vacancy calendar
9. **Multi-owner Manager Role** — managers scoped to specific PGs via `managerIds[]`
10. **WhatsApp Bot (Phase 2)** — `whatsappWebhook` Cloud Function placeholder ready for Meta API

---

*Report generated for PGManager v1.0.0 — Build SUCCESSFUL*
