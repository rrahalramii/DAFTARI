# Build on Windows in VS Code

1. Install Android Studio once so the Android SDK/JDK are available.
2. Make sure Java 17 is selected.
3. Open this folder in VS Code.
4. Open a PowerShell terminal in the project root.
5. Run `gradle assembleDebug` if Gradle is installed on PATH, or open the same folder in Android Studio and let Gradle sync.
6. The debug APK will be under `app/build/outputs/apk/debug/` after a successful build.

If your existing auction project already has a working Gradle wrapper, you can copy its wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) into this project without changing the source code.
