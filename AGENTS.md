# Compiling and Building
- Use JDK 21 to run Gradle and the build tooling.
    - On macOS, run `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` before any `gradlew` commands.
- JDK 21 is only the build runtime. The Minecraft 1.8.9 Forge mod and its shaded dependencies must remain compatible with Java 8 (class-file version 52).

- The Ornithe branch uses Java 25 for the Minecraft client and compilation because OneConfig v1 requires it. Keep Gradle itself on JDK 21. The Java 8 requirement above applies only to Forge artifacts.
