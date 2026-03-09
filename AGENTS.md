### Project Overview
LinkSaver is an Android application built with Kotlin and Jetpack Compose. It uses Retrofit for API communication and DataStore for persistent settings.

### Build/Configuration Instructions
- **SDK Versions**: 
  - `compileSdk`: 36
  - `targetSdk`: 36
  - `minSdk`: 24
- **Java Version**: Project uses Java 21 (`sourceCompatibility` and `targetCompatibility`).
- **Gradle**: Uses Kotlin DSL (`.gradle.kts`) and version catalogs (`libs.versions.toml`).
- **Invoking Gradle**: This project does *not* use Gradle wrapper, assume that the `gradle` command is available in PATH instead. 
- **Dependencies**:
  - UI: Jetpack Compose with Material 3.
  - Networking: Retrofit 2.11.0 and OkHttp 4.12.0.
  - Data Storage: Preferences DataStore 1.1.2.
  - Navigation: Navigation Compose 2.8.5.

### Testing Information
#### Configuring and Running Tests
- **Unit Tests**: Located in `app/src/test/kotlin/`. Run via `gradle test` or using the IDE's test runner.
- **Instrumentation Tests**: Located in `app/src/androidTest/kotlin/`. Run via `gradle connectedAndroidTest`.

#### Adding New Tests
- Use **JUnit 4** for unit tests.
- Use **Espresso** or **Compose Test library** for UI tests.
- Example unit test:
  ```kotlin
  class SimpleTest {
      @Test
      fun addition_isCorrect() {
          assertEquals(4, 2 + 2)
      }
  }
  ```

### Additional Development Information
- **Code Style**: Follow standard Kotlin coding conventions. The project uses Jetpack Compose for all UI components.
- **Architecture**:
  - `LinkRepository` handles data operations, API client creation, and settings management using DataStore.
  - `LinkApi` defines the Retrofit interface for the backend service.
  - UI screens are implemented as Composable functions in the `nu.staldal.linksaver.ui` package.
- **API Communication**: The app uses Basic Authentication, configured via an interceptor in `LinkRepository.getApi()`.
- **Debugging**: Networking logs are available via a network interceptor in `LinkRepository` with the tag `LinkRepository`.

### Build and Run
- **Building**: Build the app via `gradle assembleDebug`
- **Running**: Run the app via `gradle installDebug` and then launch it from Android Studio or via `adb shell am start -n <package_name>/nu.staldal.linksaver.MainActivity`
