# iHeaven — Mobile Shopping Application & Web Admin Panel

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-brightgreen)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java%20%7C%20JS-orange)](https://www.java.com/)
[![Backend](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Payment](https://img.shields.io/badge/Payment-PayHere-blue)](https://www.payhere.lk/)

**iHeaven** is a full-featured, real-time e-commerce solution targeting mobile-first shoppers and retailers in Sri Lanka. It combines a modern native Android application built in Java with a responsive web-based admin management dashboard. Powered by Google Firebase, the system delivers instant, two-way data synchronization for catalog, order, user, and inventory updates without requiring bespoke backend server infrastructure.

---

## 📸 Overview & Key Highlights

- **Native Android App**: High performance with Material Design 3 UI, custom dynamic product attribute modifiers (storage, color options), cart operations, order tracking, and location services.
- **Real-Time Web Admin Panel**: Built with modern vanilla JavaScript and Firebase JS SDK v10 (ES Modules) for live order updates, user access management (ban/unban), catalog handling, and dashboard analytics.
- **Secure Local Payment Integration**: Embedded checkout supporting Sri Lankan card, mobile wallet, and banking networks via the PayHere Android SDK.
- **Hardware-Sensory & Background Services**:
  - **Shake-to-Clear Cart**: Uses device accelerometer (`SensorManager`) for quick cart cleanup via physical gesture.
  - **Automated Order Lifecycle Notifications**: Uses Android `AlarmManager` with high-priority channels to schedule accurate local tracking status notifications across key milestones (Packing, Picking, Out for Delivery, Delivered).

---

## 🏗️ System Architecture

The project follows a **3-tier cloud architecture** where both mobile client and web panel interact directly with Google Firebase Cloud Services using localized snapshots and transactional flows.

```
                  +-----------------------------------+
                  |        Web Admin Dashboard        |
                  |  (HTML5 / CSS3 / ES Module JS)   |
                  +-----------------+-----------------+
                                    |
                                    v
+-----------------------------------+-----------------------------------+
|                  Google Firebase Cloud Platform                      |
|                                                                       |
|  [ Cloud Firestore ]  <-->  [ Firebase Auth ]  <-->  [ Firebase FCM ]  |
+-----------------------------------+-----------------------------------+
                                    ^
                                    |
                  +-----------------+-----------------+
                  |       Android Mobile Client       |
                  |     (Java / Material Design 3)    |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  |        PayHere Payment SDK        |
                  +-----------------------------------+
```

---

## 🛠️ Technology Stack

| Layer / Feature | Technologies & Libraries Used |
| :--- | :--- |
| **Mobile Platform** | Android SDK (Java), Target API 26+, ViewBinding, Lombok |
| **UI Components** | Material Design 3, RecyclerView, ViewPager2, ChipGroup, CardView |
| **Media & UI Helpers** | Glide 4.x, DotsIndicator (`tbuonomo`) |
| **Backend & Realtime DB**| Google Firebase Cloud Firestore (`onSnapshot()` listeners) |
| **Auth & Messaging** | Firebase Authentication (Email/Password), Firebase Cloud Messaging (FCM) |
| **Payment Gateway** | PayHere Android SDK `v3.0.18` (integrated via JitPack) |
| **Hardware & Location** | Android `SensorManager` (Accelerometer), Google Fused Location Provider |
| **Background Tasks** | Android `AlarmManager` + `BroadcastReceiver` (`NotificationReceiver`) |
| **Admin Panel** | HTML5, CSS3, Vanilla JavaScript (ES6 Modules), Firebase JS SDK v10 |

---

## 🗄️ Firestore Database Structure

Data in Firestore is structured in the following main collections:

```
Firestore Root
├── users/{uid}                        # User profiles (name, email, mobile, status, fcmToken)
│   └── cart/{docId}                   # Sub-collection: Active cart items & selected variants
├── products/{productId}              # Product catalog (title, price, stock, ratings, attributes[])
├── categories/{categoryId}           # Categories list (categoryName, image URLs)
├── orders/{orderId}                  # Confirmed orders (userId, totals, items[], status timeline)
└── carousel/{docId}                  # Promotional carousel banners
```

---

## 🚀 Key Features

### 📱 Customer Mobile Application
1. **User Authentication**: Secure signup and signin backed by Firebase Auth.
2. **Interactive Catalog Browsing**: Search by keyword and filter dynamically by category.
3. **Dynamic Variant Selection**: Real-time recalculation of total cost based on selected storage capacities and color options.
4. **Transaction Safety**: Pending orders are created in memory and only committed to Firestore upon receipt of a verified successful result code (`RESULT_OK`) from the PayHere SDK.
5. **Order Tracking**: Detailed timeline view of active orders and order history.
6. **Physical Gesture Interaction**: Accelerometer-driven shake detection prompts users to easily clear their active shopping cart.
7. **Offline-Capable Scheduled Notifications**: Alarm-triggered local background status reminders post-checkout.

### 💻 Web Admin Dashboard
1. **Live Analytics**: Overview cards summarizing total customer count, catalog volume, total orders, and cumulative revenue.
2. **Customer Management**: Search customer lists and execute immediate account ban/unban commands.
3. **Product & Category CRUD**: Add, edit, update, or remove products (including custom variant pricing) directly from the browser.
4. **Order State Updates**: Real-time order processing that instantaneously reflects changes on the customer's mobile device screen within 2 seconds.

---

## 🛠️ Getting Started & Installation

### Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer recommended)
- **JDK 17** or higher
- **Android Device or Emulator** running API level 26 (Android 8.0) or higher
- A active **Firebase Project**
- A **PayHere Merchant Account** (Sandbox or Live)

---

### Setup Instructions

#### 1. Clone the Repository
```bash
git clone https://github.com/your-username/iHeaven.git
cd iHeaven
```

#### 2. Mobile App Setup (Android Studio)
1. Open the mobile project folder in Android Studio.
2. Register your Android app package name in your [Firebase Console](https://console.firebase.google.com/).
3. Download the `google-services.json` file provided by Firebase and place it inside the `app/` directory:
   ```
   iHeaven/app/google-services.json
   ```
4. Configure your PayHere credentials in your app code or gradle properties (`Merchant ID`, `Merchant Secret`).
5. Sync project with Gradle files and build the app.

#### 3. Web Admin Panel Setup
1. Navigate to the `admin/` directory.
2. Open `firebase.js` (or designated configuration file) and replace the configuration placeholder with your Web Firebase Config values:
   ```javascript
   const firebaseConfig = {
     apiKey: "YOUR_API_KEY",
     authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
     projectId: "YOUR_PROJECT_ID",
     storageBucket: "YOUR_PROJECT_ID.appspot.com",
     messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
     appId: "YOUR_APP_ID"
   };
   ```
3. Serve the admin panel locally using any standard development server (e.g., Live Server extension in VS Code, IntelliJ built-in web server, or `npx serve`).

---

## 🧪 Testing Summary

Testing was performed across both an **Android Emulator (Pixel 9a, API 36)** and **physical test devices**:

- **Payment Flow Validation**: Verified orders only persist to Firestore when payment status reports success. Cancellations discard pending memory state without modifying database tables.
- **Real-Time Synchronicity**: Verified changes made in the web admin panel propagate to active client views in under 2 seconds via Firestore WebSocket listeners.
- **Gesture Verification**: Confirmed shake thresholds successfully trigger prompt confirmation dialogs before modifying cart items.

---



