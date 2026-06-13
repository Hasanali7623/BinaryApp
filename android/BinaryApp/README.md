# BinaryApp — Android Authentication & Onboarding

> **Cybersecurity SaaS Platform** | Kotlin + Room SQLite + MVVM + Navigation Component

---

## 📁 Project Location

```
android/BinaryApp/
```

Open this folder directly in **Android Studio Electric Eel** or newer.

---

## 🏗️ Architecture

```
MVVM Clean Architecture
├── UI Layer          → Activities, Fragments, ViewBinding
├── ViewModel Layer   → AuthViewModel, AuthViewModelFactory
├── Repository Layer  → AuthRepository
└── Data Layer        → Room Database, DAOs, Entities
```

---

## 📂 Full Folder Structure

```
BinaryApp/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/binaryapp/
│       │   ├── BinaryApp.kt                     ← Application class
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── BinaryAppDatabase.kt     ← Room Database
│       │   │   │   ├── dao/
│       │   │   │   │   ├── UserDao.kt
│       │   │   │   │   ├── OtpDao.kt
│       │   │   │   │   ├── TrustedDeviceDao.kt
│       │   │   │   │   └── SessionDao.kt
│       │   │   │   └── entities/
│       │   │   │       ├── User.kt
│       │   │   │       ├── OtpVerification.kt
│       │   │   │       ├── TrustedDevice.kt
│       │   │   │       └── Session.kt
│       │   │   └── repository/
│       │   │       └── AuthRepository.kt
│       │   ├── ui/auth/
│       │   │   ├── MainActivity.kt              ← Single Activity host
│       │   │   ├── login/LoginFragment.kt
│       │   │   ├── register/RegisterFragment.kt
│       │   │   ├── verify/
│       │   │   │   ├── EmailVerificationFragment.kt
│       │   │   │   ├── VerifySuccessFragment.kt
│       │   │   │   └── VerifyFailedFragment.kt
│       │   │   ├── trustdevice/TrustDeviceFragment.kt
│       │   │   ├── accessgranted/AccessGrantedFragment.kt
│       │   │   ├── biometric/BiometricFragment.kt
│       │   │   ├── devicepairing/DevicePairingFragment.kt
│       │   │   ├── forgotpassword/ForgotPasswordFragment.kt
│       │   │   └── resetpassword/
│       │   │       ├── ResetLinkSentFragment.kt
│       │   │       ├── CreateNewPasswordFragment.kt
│       │   │       └── PasswordChangedFragment.kt
│       │   ├── utils/
│       │   │   ├── HashUtils.kt                 ← SHA-256 password hashing
│       │   │   ├── OtpUtils.kt                  ← OTP generation
│       │   │   ├── ValidationUtils.kt           ← Form validation
│       │   │   ├── SessionManager.kt            ← SharedPreferences session
│       │   │   └── DeviceUtils.kt               ← Device info
│       │   └── viewmodel/
│       │       ├── AuthViewModel.kt             ← Central ViewModel
│       │       └── AuthViewModelFactory.kt
│       └── res/
│           ├── anim/                            ← Slide & fade animations
│           ├── drawable/                        ← Icons, backgrounds, hero images
│           ├── font/inter.xml
│           ├── layout/                          ← 13 Fragment layouts + 1 activity
│           ├── mipmap-*/ic_launcher*.xml
│           ├── navigation/nav_auth.xml          ← Navigation graph
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml                      ← Version catalog
│   └── wrapper/gradle-wrapper.properties
├── gradle.properties
└── settings.gradle.kts
```

---

## 🖥️ Screens Implemented (13 Screens)

| # | Screen | Fragment |
|---|--------|----------|
| 1 | Login | `LoginFragment` |
| 2 | Register | `RegisterFragment` |
| 3 | Email Verification (OTP) | `EmailVerificationFragment` |
| 4 | Verify Success | `VerifySuccessFragment` |
| 5 | Verify Failed | `VerifyFailedFragment` |
| 6 | Trust Device | `TrustDeviceFragment` |
| 7 | Access Granted | `AccessGrantedFragment` |
| 8 | Biometric Setup | `BiometricFragment` |
| 9 | Device Pairing | `DevicePairingFragment` |
| 10 | Forgot Password | `ForgotPasswordFragment` |
| 11 | Reset Link Sent | `ResetLinkSentFragment` |
| 12 | Create New Password | `CreateNewPasswordFragment` |
| 13 | Password Changed | `PasswordChangedFragment` |

---

## 🗄️ Database (Room SQLite)

| Table | Fields |
|-------|--------|
| `users` | id, fullName, email, passwordHash, role, isVerified, createdAt |
| `otp_verifications` | id, userId, otpCode, expiresAt, isUsed, createdAt |
| `trusted_devices` | id, userId, deviceName, deviceModel, browser, location, lastLogin, trusted |
| `sessions` | id, userId, loginTime, logoutTime, deviceInfo, location, isActive |

---

## 🔐 Authentication Flow

```
Login/Register → Email OTP Verification → Verify Success
     ↓                                         ↓
Forgot Password → Reset Link Sent → Create New Password → Success
                                         
Trust Device → Access Granted → Biometric Setup → Device Pairing
```

---

## 🎨 Design System

| Token | Value |
|-------|-------|
| Background | `#030816` |
| Neon Blue | `#00B4FF` |
| Success Green | `#00E676` |
| Error Red | `#FF3B3B` |
| Glass Card BG | `#0D1A2E` |
| Text Primary | `#FFFFFF` |
| Text Secondary | `#8BA3C4` |

---

## 🚀 How to Open & Run

1. Open **Android Studio** → **Open** → select `android/BinaryApp/`
2. Wait for Gradle sync to complete
3. Connect Android device (API 26+) or start an emulator
4. Click **Run ▶**

> **Note:** The app uses demo OTP display via `Toast` messages for testing. In production, integrate an email service (SendGrid, AWS SES, Firebase).

---

## ⚙️ Tech Stack

- **Language:** Kotlin 1.9.24
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.7 with version catalog
- **Architecture:** MVVM + Clean Architecture
- **Database:** Room 2.6.1 (SQLite)
- **Navigation:** Navigation Component 2.7.7
- **UI:** Material Design 3 + XML Layouts + ViewBinding
- **Biometrics:** AndroidX Biometric 1.1.0
- **Async:** Kotlin Coroutines 1.8.1
