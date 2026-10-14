# Privacy Policy - Muajjin Salat Times App

**Last Updated:** October 2026

## 1. Overview

Muajjin is a privacy-focused Islamic Salat times application. We respect your privacy and are committed to protecting your personal data. This policy explains how Muajjin handles your information.

## 2. Data Collection

### 2.1 Location Data

- **Purpose:** To calculate accurate Salat times based on your geographical location
- **Usage:** Coordinates support local prayer/fasting calculations, Qibla and native schedule renewal.
- **Storage:** Saved coordinates and location labels are kept in local app/browser storage.
- **Network exception:** When automatic GPS detection requests a city/country name, coordinates are sent to OpenStreetMap Nominatim. IP fallback providers receive your IP address. Manual entry does not require these services; see section 3.2.

### 2.2 Permissions Requested

- **Location permission:** Used only for user-initiated GPS detection. Approximate/coarse permission is supported where the platform provides it. Requests are bounded and cancelled when obsolete; there is no continuous background location tracking.
- IP lookup and manual entry do **not** require GPS permission.
- **Notifications/exact alarms:** Android permissions and device settings control optional native notification delivery and precise transition alarms. Widgets use Android’s widget-host APIs. Permission denial does not disable local calculations/manual location.

### 2.3 No User Accounts

- Muajjin does not require user registration or account creation
- No personal identifiers (names, emails, phone numbers) are collected
- No authentication or login required

## 3. Data Storage & Security

### 3.1 Local Storage

- Settings, Qaza counts and customization use local storage; native schedule/configuration/presentation use Android private preferences.
- Custom translations stay local. Verified custom font binaries use IndexedDB; legacy base64 data is retained until successful durable migration.
- Existing network exceptions are IP lookup, optional GPS reverse geocoding, and GitHub update checks/downloads in the GitHub distribution. Muajjin has no account, analytics or advertising backend.
- Android’s manifest permits OS-managed app backup. What is backed up/restored depends on Android, device policy and user settings; Muajjin does not provide its own cloud backup.

### 3.2 Third-Party Services

**IP Geolocation Services** (fallback when GPS unavailable):

- ip-api.com (HTTP)
- ipwhois.app (HTTPS)
- These services receive your IP address to determine approximate location
- IP addresses are personal data; approximate location may be returned. The HTTP provider is not encrypted and may be blocked by platform policy; HTTPS is the existing fallback. Provider privacy policies apply.

**OpenStreetMap Nominatim API:**

- Used to reverse geocode coordinates into city/country names
- Receives the requested coordinates plus ordinary request information such as IP address and user-agent, not your Qaza history, saved settings or custom imports.
- Coordinates are personal data. Address lookup is optional for local calculations; failure retains valid coordinates without inventing a city/country.

## 4. Data Sharing & Selling

### 4.1 No Selling or Advertising

- We do not sell or rent personal data. Automatic detection and GitHub-only updates contact the services disclosed above; we do not claim those requests transmit no personal data.
- No data is used for advertising purposes
- No analytics or tracking services are integrated

### 4.2 No Advertising or Analytics SDKs

- No Google Analytics
- No Facebook SDK
- No Firebase Analytics
- No advertising networks

## 5. User Control & Rights

### 5.1 Location Control

- You can manually enter your location instead of using GPS
- Location permission can be revoked at any time in device settings
- The app functions fully with manual location entry

### 5.2 Data Deletion

- To delete all app data: Uninstall the app or clear app data in device settings
- Clearing/uninstalling removes the app’s active local data, but OS-managed backups/restores are governed by platform settings. Browser deployments also require clearing the relevant site’s localStorage/IndexedDB. We cannot promise uninstall erases every OS backup.

### 5.3 Data Access

- Settings, Qaza controls and translation/font controls expose the supported user data operations. System app/site storage controls can clear the complete local dataset.

## 6. Children's Privacy

- Muajjin is suitable for users of all ages
- No account or identity collection is required. Coordinates/IP addresses used in automatic detection are still personal data, regardless of age.

## 7. Data Retention

- Local app data persists until removed through app/site controls or uninstall, subject to platform backup/restore.
- Muajjin operates no cloud-storage service; Android OS-managed backup may be enabled by the user/device.

## 8. Changes to This Privacy Policy

- We may update this privacy policy from time to time
- Significant changes will be communicated through app updates
- Continued use of the app after changes constitutes acceptance

## 9. Open Source Transparency

- Muajjin is open source software
- Source code is available for public review on GitHub
- You can verify our privacy practices by inspecting the code

## 10. Contact Information

For privacy-related inquiries:

- **GitHub Issues:** https://github.com/muslimtechnician/muajjin/issues

## 11. App Version Information

- **App Name:** Muajjin
- **Package Name:** org.muslimtechnician.muajjin
- **Category:** Lifestyle/Religion
- **Target Audience:** General audience

---

**License:** This app is released under the MIT License. The privacy policy is part of the app's documentation.

**Language:** This privacy policy is written in English. Translations may be provided for user convenience, but the English version is the authoritative version.

---

_This policy describes the current Muajjin storage/network behavior; Android and third-party service policies also apply._
