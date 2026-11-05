pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.ornithemc.net/releases")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "gg.essential.loom")
                useModule("gg.essential:architectury-loom:${requested.version}")
        }
    }
}
plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
}
rootProject.name = "Mellow"
stonecutter {
    create(rootProject) {
        version("1.8.9-forge", "1.8.9").buildscript("build.forge.gradle.kts")
        version("1.8.9-ornithe", "1.8.9").buildscript("build.ornithe.gradle.kts")
        vcsVersion = "1.8.9-ornithe"
    }
}
