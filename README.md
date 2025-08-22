# Whisper Android 🎙️

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

A privacy-focused, offline speech-to-text Android application powered by OpenAI's Whisper model. All processing happens locally on your device - no internet connection required after initial setup.

## 🎯 Features

- **On-device transcription** - No internet required
- **ARM v8 optimized** - Optimized for modern Android devices
- **Multiple model support** - Tiny, Base, Small, Medium, Large models
- **Real-time recording** - Live audio capture with waveform visualization
- **File transcription** - Support for various audio file formats
- **Material Design 3** - Modern, accessible UI
- **Dark/Light themes** - Automatic and manual theme switching
- **Multi-language support** - Support for 99+ languages

## 🏗️ Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
├── presentation/     # UI layer (Compose, ViewModels)
├── domain/          # Business logic (Use cases, Models)
├── data/            # Data layer (Repositories, Data sources)
├── native/          # JNI bridge to whisper.cpp
└── di/              # Dependency injection (Hilt)
```

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Native**: whisper.cpp + JNI
- **Build**: Gradle (Kotlin DSL)
- **Testing**: JUnit, Espresso, Compose Testing

## 📋 Requirements

- **Android Studio**: Hedgehog 2023.1.1+
- **Android SDK**: API 24+ (Android 7.0+)
- **NDK**: 26.1.10909125
- **CMake**: 3.22.1+
- **JDK**: 17
- **Device**: ARM v8 (aarch64) architecture

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/whisper-android.git
cd whisper-android
```

### 2. Setup Environment

Ensure you have the following environment variables set:

```bash
ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
ANDROID_NDK_HOME=%ANDROID_HOME%\ndk\26.1.10909125
```

### 3. Add whisper.cpp Submodule

```bash
git submodule add https://github.com/ggml-org/whisper.cpp.git app/src/main/cpp/whisper.cpp
git submodule update --init --recursive
```

### 4. Build the Project

Using VS Code tasks (Ctrl+Shift+P → "Tasks: Run Task"):
- **Build Debug APK**
- **Install Debug APK**
- **Run Unit Tests**

Or using command line:

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

## 📱 Current Status

### ✅ Completed (Phase 1)
- [x] Project structure setup
- [x] Gradle configuration
- [x] Clean Architecture foundation
- [x] Material Design 3 theming
- [x] Hilt dependency injection setup
- [x] VS Code integration
- [x] Basic native layer placeholder

### 🚧 In Progress
- [ ] whisper.cpp integration
- [ ] JNI bindings implementation
- [ ] Audio recording system
- [ ] UI components

### 📅 Upcoming
- [ ] Model management
- [ ] Transcription engine
- [ ] Testing infrastructure
- [ ] Performance optimization

## 🧪 Testing

Run tests using VS Code tasks or command line:

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## 📖 Development Guide

### VS Code Integration

This project is optimized for VS Code development:

- **Tasks**: Pre-configured build, test, and lint tasks
- **Launch**: Android debugging configuration
- **Settings**: Kotlin and Android-specific settings

Use `Ctrl+Shift+P` and search for "Tasks" to see available commands.

### Project Structure

```
whisper-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/app/whisper/
│   │   │   ├── data/           # Data layer
│   │   │   ├── domain/         # Business logic
│   │   │   ├── presentation/   # UI layer
│   │   │   ├── native/         # JNI bridge
│   │   │   └── di/             # Dependency injection
│   │   ├── cpp/                # Native C++ code
│   │   └── res/                # Android resources
│   └── build.gradle.kts
├── .vscode/                    # VS Code configuration
├── TODO.md                     # Detailed task list
└── build.gradle.kts
```

## 🤝 Contributing

1. Check the [TODO.md](TODO.md) for current tasks
2. Follow the established architecture patterns
3. Write tests for new functionality
4. Ensure code follows Kotlin coding standards
5. Update documentation as needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [whisper.cpp](https://github.com/ggml-org/whisper.cpp) - The core speech-to-text engine
- [OpenAI Whisper](https://github.com/openai/whisper) - The original Whisper model
- [Material Design 3](https://m3.material.io/) - Design system and components

---

**Note**: This project is currently in active development. The native whisper.cpp integration is not yet complete. See [TODO.md](TODO.md) for detailed progress tracking.
