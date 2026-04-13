# 🛡️ Road Safety GIS — Android Application

> **A comprehensive road safety and navigation application** that helps users report road hazards, view them on a live map with color-coded circles, get directions, access emergency services, and much more.

---

## 📱 Screenshots & Features

### Core Features
| Feature | Description |
|---------|-------------|
| 🗺️ **Live Map** | Google Maps with color-coded circle overlays showing hazard zones |
| 📳 **Shake to Report** | Shake your phone on the map screen to quickly report a hazard |
| 🏎️ **Speed Monitor** | Live GPS speed display on the map (turns red over 80 km/h) |
| 🚨 **Road Alerts** | Filterable list of all reported hazards with type & severity badges |
| 📍 **Share Location** | Share your GPS location via SMS, WhatsApp, or any app |
| 🆘 **SOS Alert** | Emergency SOS with Morse code vibration pattern |
| 📞 **Emergency Call** | One-tap emergency dial to 112 |
| 🏥 **Nearby Services** | Find police stations, hospitals, and gas stations via Google Maps |
| 🌙 **Dark Mode** | Toggle dark theme from Profile → Preferences |
| 📊 **User Stats** | Track your reports submitted on your profile |

---

## 🏗️ Project Structure

```
app/src/main/java/com/example/saferoutegis/
├── MainActivity.java                    ← Splash screen (app entry point)
├── activities/
│   ├── LoginActivity.java               ← User login with email/password
│   ├── RegisterActivity.java            ← New user registration
│   ├── DashboardActivity.java           ← Home screen with all feature cards
│   ├── MapActivity.java                 ← Live map with markers & circles
│   ├── AlertsActivity.java              ← Filterable road hazard alerts list
│   ├── ReportIssueActivity.java         ← Submit a new road hazard report
│   ├── DirectionsActivity.java          ← Get directions between two points
│   └── ProfileActivity.java             ← User profile, dark mode, preferences
├── adapters/
│   └── AlertsAdapter.java              ← RecyclerView adapter for alert items
├── database/
│   └── DatabaseHelper.java             ← SQLite database (users + reports)
├── models/
│   ├── User.java                       ← User data model
│   └── Report.java                     ← Report data model with type constants
└── utils/
    ├── Constants.java                  ← App-wide constants and config
    ├── SessionManager.java             ← Login session via SharedPreferences
    ├── NotificationHelper.java         ← Notifications with vibration support
    ├── LocationHelper.java             ← GPS location utilities
    └── PasswordUtils.java             ← Password hashing utilities
```

---

## 🔧 Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Java** | Primary programming language |
| **Android SDK** (API 24–36) | Native Android development |
| **Material Design 3** | Modern UI components (cards, chips, switches) |
| **Google Maps SDK** | Map display, markers, circles, traffic layer |
| **Google Play Services Location** | GPS, FusedLocationProvider, speed tracking |
| **SQLite** (via `DatabaseHelper`) | Local database for users and reports |
| **OkHttp** | HTTP client for Directions API |
| **Glide** | Image loading for report photos |

---

## 🎨 Design System

### Color Palette
| Color | Hex | Usage |
|-------|-----|-------|
| Primary Blue | `#1565C0` | Headers, buttons, links |
| Light Blue | `#42A5F5` | Logo inner circle, accents |
| Gold/Amber | `#FFB300` | Location pin, accent highlights |
| Emergency Red | `#D32F2F` | SOS buttons, accident markers |
| Success Green | `#43A047` | Share button, safety tips |

### Map Circle Colors
| Issue Type | Circle Color | Marker Hue |
|-----------|-------------|------------|
| 🔴 Accident | `#E53935` (Red) | `HUE_RED` |
| 🟠 Pothole | `#F57C00` (Orange) | `HUE_ORANGE` |
| 🟡 Construction | `#FBC02D` (Amber) | `HUE_YELLOW` |
| 🔵 Traffic | `#1E88E5` (Blue) | `HUE_AZURE` |

### Circle Radius by Severity
| Severity | Radius |
|----------|--------|
| Low | 150 meters |
| Medium | 200 meters |
| High | 300 meters |

---

## 📂 Key Files Explained

### 1. `MainActivity.java` — Splash Screen
- **Purpose**: First screen the user sees when opening the app
- **What it does**: Shows the app logo with a bounce animation, app name, tagline, and a feature highlights row
- **Navigation**: After 2.8 seconds → Dashboard (if logged in) or Login (if not)
- **Calls**: `NotificationHelper.createNotificationChannel()` to set up alerts

### 2. `DashboardActivity.java` — Home Screen
- **Purpose**: Central hub for all app features
- **Sections**:
  - Gradient header with personalized greeting
  - Feature cards (Map, Traffic, Routes, Accidents, Potholes, Construction, Directions)
  - Emergency section (Call 112, SOS Alert, Share Location)
  - Nearby Services (Police, Hospital, Gas Station)
  - Safety Tips card
- **Bottom Navigation**: Home, Map, Report, Alerts, Profile

### 3. `MapActivity.java` — Live Map
- **Key Features**:
  - Colored circles around each hazard marker (radius scales with severity)
  - **Speed Monitor**: Live km/h display using `LocationCallback` with color warnings
  - **Shake to Report**: Uses `SensorEventListener` with accelerometer
  - **Share Location**: Sends Google Maps link via share intent
  - Traffic layer toggle
- **Important Methods**: `loadReportMarkers()`, `updateSpeed()`, `onShakeDetected()`

### 4. `AlertsActivity.java` — Road Alerts
- **Purpose**: Shows all reported hazards in a filterable list
- **Filter Chips**: All, Accidents, Potholes, Construction, Traffic
- **Uses**: `AlertsAdapter` with `ChipGroup` for filtering

### 5. `DatabaseHelper.java` — Local Database
- **Purpose**: SQLite database with two tables: `users` and `reports`
- **Key Methods**: `insertReport()`, `getAllReports()`, `getNearbyReports()`, `getReportCountByUserId()`

### 6. `NotificationHelper.java` — Notifications
- **Purpose**: Sends local notifications with vibration
- **Key Methods**: `showReportSubmitted()`, `showNearbyHazard()`, `vibrateDevice()`
- **Vibration Pattern**: `{0, 200, 100, 400}` (short buzz, pause, long buzz)

### 7. `ProfileActivity.java` — User Profile
- **Features**:
  - Avatar with user initial
  - Report statistics counter
  - Inline profile editing
  - **Dark Mode toggle** (uses `AppCompatDelegate.MODE_NIGHT_YES/NO`)
  - **Notification toggle**
  - App info and Rate App button

---

## 🚀 Setup & Configuration

### Prerequisites
- Android Studio Flamingo or later
- JDK 11+
- Google Maps API Key (with Maps SDK + Directions API enabled)

### Steps
1. Clone the repository
2. Open in Android Studio
3. Replace the API key in `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="google_maps_key">YOUR_API_KEY_HERE</string>
   ```
4. Enable these APIs in [Google Cloud Console](https://console.cloud.google.com/):
   - Maps SDK for Android
   - Directions API
5. Build and run on a device or emulator

### Permissions Used
| Permission | Purpose |
|-----------|---------|
| `ACCESS_FINE_LOCATION` | GPS location for map, speed, reports |
| `ACCESS_COARSE_LOCATION` | Approximate location fallback |
| `POST_NOTIFICATIONS` | Road hazard alert notifications |
| `VIBRATE` | SOS and alert vibration feedback |
| `CALL_PHONE` | Emergency dialing |
| `CAMERA` | Taking photos for reports |
| `READ/WRITE_EXTERNAL_STORAGE` | Saving report images |
| `INTERNET` | Google Maps, Directions API |

---

## 📋 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    email       TEXT UNIQUE NOT NULL,
    password    TEXT NOT NULL,
    phone       TEXT,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Reports Table
```sql
CREATE TABLE reports (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    title       TEXT NOT NULL,
    description TEXT,
    type        TEXT NOT NULL,     -- ACCIDENT | POTHOLE | CONSTRUCTION | TRAFFIC
    severity    INTEGER NOT NULL,  -- 0=Low, 1=Medium, 2=High
    latitude    REAL NOT NULL,
    longitude   REAL NOT NULL,
    image_path  TEXT,
    timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🤝 Contributing
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License
This project is developed for educational purposes as part of a Road Safety GIS initiative.

---

> **Built with ❤️ for safer roads**
