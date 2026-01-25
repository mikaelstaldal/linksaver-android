# Link Saver Android

A native Android client for [Link Saver](https://github.com/mikaelstaldal/linksaver), built with Kotlin and Jetpack Compose.

## Overview

Link Saver Android allows you to manage your saved links on the go (although it's not written in Go). 
It connects to a Link Saver API (which *is* written in Go) to fetch, add, edit, and delete links.

## Features

- View a list of saved links.
- Search for links.
- Add new links.
- Edit existing link titles.
- Delete links.
- Basic authentication support.
- Configurable API base URL.

## Requirements

- Android device or emulator running Android 7.0 (API level 24) or higher.
- Java Development Kit (JDK) 21.
- Android Studio Ladybug | 2024.2.1 or newer (recommended), or IntelliJ IDEA with Android plugin and Android SDK.

## Setup & Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mikaelstaldal/linksaver-android.git
    cd linksaver-android
    ```

2.  **Open in Android Studio:**
    Import the project as a Gradle project.

3.  **Configure API Settings:**
    Once the app is running, go to the Settings screen and provide:
    - **Base URL:** The URL of your LinkSaver instance (e.g., `https://links.example.com/`).
    - **Username:** Your username.
    - **Password:** Your password.

4.  **Build and Run:**
    Use the "Run" button in Android Studio or use Gradle from the command line:
    ```bash
    gradle assembleDebug
    ```

## Scripts

- `gradle assembleDebug`: Build the debug APK.
- `gradle bundleRelease`: Build the release App Bundle.
- `gradle test`: Run unit tests.
- `gradle connectedAndroidTest`: Run instrumented tests on a device/emulator.
- `gradle lint`: Run static analysis.

## Project Structure

```text
.
├── app/                        # Main application module
│   ├── src/main/kotlin/        # Kotlin source code
│   │   └── nu/staldal/linksaver/
│   │       ├── data/           # Data layer (API, Repository, Models)
│   │       ├── ui/             # UI layer (Compose screens, Theme)
│   │       └── MainActivity.kt # Main entry point
│   ├── src/main/res/           # Android resources (strings, themes, drawables)
│   └── build.gradle.kts        # Module-level Gradle configuration
├── gradle/                     # Gradle wrapper and version catalog
│   └── libs.versions.toml      # Dependency versions management
├── build.gradle.kts            # Project-level Gradle configuration
├── settings.gradle.kts         # Gradle project settings
└── TODO.md                     # Roadmap and planned features
```

## Configuration

The application uses Android DataStore to persist user settings locally on the device.
- `base_url`: The endpoint for the LinkSaver API.
- `username`: Credentials for Basic Auth.
- `password`: Credentials for Basic Auth.

## Tests

- **Unit Tests:** Located in `app/src/test`. Run with `./gradlew test`.
- **Instrumented Tests:** Located in `app/src/androidTest`. Run with `./gradlew connectedAndroidTest`.

## License

Copyright 2026 Mikael Ståldal.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
