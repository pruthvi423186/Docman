
# Docman - Intelligent Document Manager

A premium, modern Android Document Manager that seamlessly integrates with **Google Drive** and **ML Kit**. Built with Jetpack Compose, this app allows you to scan, upload, manage, and organize your documents with a beautiful Glassmorphism-inspired UI.

## ✨ Features

*   **Google Drive Integration:** Seamlessly sync, upload, download, and delete files directly from your Google Drive account.
*   **Built-in Document Scanner:** Powered by Google ML Kit, scan physical documents using your device's camera and instantly convert them to high-quality PDFs.
*   **Premium UI/UX:** Built entirely with Jetpack Compose featuring custom animations, a dynamic glassmorphism background, and support for both Light and Dark themes.
*   **Smart Search & Organization:** Easily search through your files, filter by date, and view your documents in a clean hierarchy.
*   **Offline Support:** View recently accessed files and cached data even when you are offline.
*   **Secure Authentication:** Secure Google Sign-In via the latest Android Credential Manager API.

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose, Material Design 3
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Dagger Hilt
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **APIs:** Google Drive API v3, Google Sign-In (Credential Manager), ML Kit Document Scanner
*   **Image Loading:** Coil

## 🚀 Getting Started

If you want to clone this repository and run it locally, you will need to provide your own Google Cloud API credentials.

### Prerequisites
1. Android Studio (Latest Version)
2. A Google Cloud Console account.

### Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/pruthvi423186/Docman.git
    ```

2.  **Create a Google Cloud Project:**
    *   Go to the [Google Cloud Console](https://console.cloud.google.com/).
    *   Enable the **Google Drive API**.
    *   Set up your **OAuth Consent Screen**.
    *   Create **OAuth 2.0 Client IDs** for a Web Application (to get your Web Client ID).

3.  **Add your API Keys:**
    *   Create a file named `local.properties` in the root directory of the project (this file is ignored by Git for security).
    *   Add your Web Client ID to the file like this:
        ```properties
        GOOGLE_WEB_CLIENT_ID="your-web-client-id.apps.googleusercontent.com"
        ```

4.  **Build and Run:**
    *   Sync the project with Gradle files.
    *   Run the app on an emulator or physical device.

## 📥 Download

You can download the latest compiled APK from the [Releases](../../releases) tab to install and test the app directly on your Android device without compiling the code.

## 🔒 Privacy & Security

This app requires access to your Google Drive to manage your files and your Camera to scan documents. All data is handled directly between your device and Google's servers. API keys are safely excluded from this repository.

<img width="1260" height="2800" alt="1000020437" src="https://github.com/user-attachments/assets/837b6d92-3ea9-42f8-bcf9-8e0875a907f8" />
<img width="1260" height="2800" alt="1000020439" src="https://github.com/user-attachments/assets/0ff8c4ad-2fb4-4c72-abc7-cd08f0daa639" />
<img width="1260" height="2800" alt="1000020441" src="https://github.com/user-attachments/assets/4ce5073b-25c2-4ed8-bf94-4e5dce7bdf52" />
<img width="1260" height="2800" alt="1000020440" src="https://github.com/user-attachments/assets/1ee10908-899a-4189-91cd-5a6817c54793" />
<img width="1260" height="2800" alt="1000020438" src="https://github.com/user-attachments/assets/47d585a8-0d17-4aba-9cfc-20980335050b" />
