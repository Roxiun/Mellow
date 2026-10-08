import dev.architectury.pack200.java.Pack200Adapter

plugins {
    java
    id("gg.essential.loom") version "1.9.31"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.roxiun"
version = providers.gradleProperty("mod_version").get()
base.archivesName.set("Mellow-1.8.9-forge")
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    withSourcesJar()
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

repositories {
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://api.modrinth.com/maven")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.architectury.dev/")
    maven("https://repo.essential.gg/repository/maven-public")
}
val shade by configurations.creating
configurations.implementation { extendsFrom(shade) }
val hypixelBundle by configurations.creating { isTransitive = false }
dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    shade("com.squareup.okhttp3:okhttp:4.9.3") { exclude(group = "org.jetbrains.kotlin") }
    shade("org.tukaani:xz:1.9")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")
    modImplementation("net.hypixel:mod-api-forge:1.0.2") { isTransitive = false }
    hypixelBundle("net.hypixel:mod-api-forge-tweaker:1.0.2")
    modCompileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    shade("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    annotationProcessor("com.google.code.gson:gson:2.8.9")
    annotationProcessor("com.google.guava:guava:21.0")
    modCompileOnly("maven.modrinth:vanillahud:hvJoY3aU")
    if (providers.gradleProperty("compatMods").isPresent)
        modRuntimeOnly("maven.modrinth:vanillahud:hvJoY3aU")
    testImplementation("junit:junit:4.13.2")
}
loom {
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("mixins.mellow.json")
    }
    mixin.defaultRefmapName.set("mixins.mellow.refmap.json")
    runs {
        named("client") {
            programArgs("--tweakClass", "com.roxiun.mellow.launch.MellowTweaker")
            property("mixin.debug.export", "true")
            vmArgs.remove("-XstartOnFirstThread")
        }
        remove(getByName("server"))
    }
}
val generatedVersion = layout.buildDirectory.dir("generated/version")
val generateVersion by tasks.registering {
    inputs.property("version", project.version)
    outputs.dir(generatedVersion)
    doLast {
        val output = generatedVersion.get().file("com/roxiun/mellow/BuildVersion.java").asFile
        output.parentFile.mkdirs()
        output.writeText("package com.roxiun.mellow; public final class BuildVersion { public static final String VERSION = \"${project.version}\"; private BuildVersion() {} }")
    }
}
sourceSets {
    main {
        java.srcDir(rootProject.file("src/forge/main/java"))
        java.srcDir(generateVersion)
        resources.srcDir(rootProject.file("src/forge/main/resources"))
    }
    test { java.srcDir(rootProject.file("src/forge/test/java")) }
}
tasks.processResources {
    from({ zipTree(hypixelBundle.singleFile) }) {
        include("HypixelModAPI-1.0.2.jar")
        into("META-INF/mellow")
        rename { "HypixelModAPI-1.0.2.bin" }
    }
    val props = mapOf("id" to "mellow", "name" to "Mellow", "version" to project.version,
        "java" to 8, "java_level" to "JAVA_8", "mcVersionStr" to "1.8.9")
    inputs.properties(props)
    filesMatching(listOf("mcmod.info", "mixins.mellow.json")) { expand(props) }
}
tasks.jar {
    manifest.attributes(mapOf(
        "ModSide" to "CLIENT", "ForceLoadAsMod" to true, "TweakOrder" to "0",
        "MixinConfigs" to "mixins.mellow.json", "TweakClass" to "com.roxiun.mellow.launch.MellowTweaker"
    ))
    archiveClassifier.set("without-deps")
}
tasks.shadowJar {
    configurations = listOf(shade)
    archiveClassifier.set("dev")
}
tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    archiveClassifier.set("")
}
tasks.test { useJUnit() }

// Offline client checks are included only in explicitly requested development runs.
if (providers.gradleProperty("clientTest").isPresent) {
    val clientTest = sourceSets.create("clientTest") {
        java.srcDir(rootProject.file("src/forge/clientTest/java"))
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + output
    }
    loom.mods.register("mellow-client-tests") { sourceSet(clientTest) }
    val smokeDir = layout.buildDirectory.dir("client-test/${if (providers.gradleProperty("compatMods").isPresent) "compat" else "base"}").get().asFile
    loom.runs.named("client") { runDir(project.relativePath(smokeDir)) }
    tasks.named<JavaExec>("runClient") {
        dependsOn(clientTest.classesTaskName)
        classpath += clientTest.runtimeClasspath
        systemProperty("mellow.smokeResult", smokeDir.resolve("smoke-result.txt").absolutePath)
        systemProperty("mellow.compatTest", providers.gradleProperty("compatMods").isPresent)
        providers.gradleProperty("lwjgl2Dir").orNull?.let { directory ->
            val lwjgl = file(directory)
            val runtimeFiles = classpath.files.filter {
                !it.name.startsWith("lwjgl-") && !it.name.startsWith("lwjgl_util-")
            }
            classpath = files(runtimeFiles, fileTree(lwjgl) { include("*.jar"); exclude("*natives*") })
            doFirst {
                copy {
                    from(lwjgl.resolve("natives"))
                    into(rootProject.file(".gradle/loom-cache/natives/1.8.9"))
                }
            }
        }
        doFirst {
            smokeDir.mkdirs()
            smokeDir.resolve("smoke-result.txt").delete()
            if (providers.gradleProperty("compatMods").isPresent) {
                copy { from(classpath.filter { it.name.contains("vanillahud", ignoreCase = true) }); into(smokeDir.resolve("mods")) }
            }
        }
        doLast {
            val result = smokeDir.resolve("smoke-result.txt")
            check(result.isFile && result.readText() == "PASS") { "Client smoke test did not pass; inspect the client log" }
        }
    }
}
