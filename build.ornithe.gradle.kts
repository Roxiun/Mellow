import org.gradle.api.artifacts.transform.*
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.util.zip.ZipInputStream

/** Loom's development classpath needs the bundle's nested mods as separate artifacts. */
@CacheableTransform
abstract class UnpackModBundle : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        fun unpack(bytes: ByteArray, name: String) {
            outputs.file(name).writeBytes(bytes)
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.startsWith("META-INF/jars/") && entry.name.endsWith(".jar")) {
                        unpack(zip.readBytes(), entry.name.substringAfterLast('/'))
                    }
                    entry = zip.nextEntry
                }
            }
        }
        unpack(input.readBytes(), input.name)
    }
}

plugins {
    java
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("net.fabricmc.fabric-loom-remap") version "1.17.4"
    id("ploceus") version "1.17.4"
}

group = "com.roxiun"
version = providers.gradleProperty("mod_version").get()
base.archivesName.set("Mellow-1.8.9-ornithe")

repositories {
    mavenCentral()
    google { content { includeGroupByRegex("androidx.*"); includeGroupByRegex("com\\.android.*") } }
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://maven.ornithemc.net/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://api.modrinth.com/maven")
}

ploceus { setIntermediaryGeneration(2) }
configurations.configureEach { exclude(group = "org.lwjgl.lwjgl") }

val oneConfigBundle by configurations.creating {
    isTransitive = false
    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "unpacked-mod-bundle")
}
dependencies.registerTransform(UnpackModBundle::class) {
    from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "unpacked-mod-bundle")
}

// Exercise the dependency versions shipped by OneClient as well as newer releases.
val oneClientBaseline = providers.gradleProperty("oneClientBaseline").isPresent
val oneClientCurrent = providers.gradleProperty("oneClientCurrent").isPresent
val oneConfigVersion = if (oneClientCurrent) "sArr1CT5" else if (oneClientBaseline) "JmPNe6D8" else "lzo51827"
val vanillaHudVersion = if (oneClientCurrent) "85IIZdpz" else if (oneClientBaseline) "mIWg3d4V" else "Gpl9yiBF"

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings(ploceus.layeredMappings {
        mappings(rootProject.file("mappings/mcp-1.8.9.tiny"))
    })
    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    ploceus.dependOsl("0.21.1")
    modCompileOnly("org.polyfrost.oneconfig:1.8.9-ornithe:1.2.10")
    oneConfigBundle("maven.modrinth:oneconfig:$oneConfigVersion")
    for (module in listOf("config", "config-impl", "events", "hud", "ui", "utils", "internal", "poly-compose")) {
        compileOnly("org.polyfrost.oneconfig:$module:1.2.10")
    }
    modImplementation("pl.tomgirl:pylon:${if (oneClientCurrent) "0.2.0" else "0.1.7"}")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.13+kotlin.2.4.10")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    include("com.squareup.okhttp3:okhttp:4.9.3")
    include("com.squareup.okio:okio:2.8.0")
    implementation("net.hypixel:mod-api:1.0.2")
    implementation("org.tukaani:xz:1.9")
    include("org.tukaani:xz:1.9")
    modCompileOnly("maven.modrinth:hitbox:lF5nB8Es")
    modCompileOnly("maven.modrinth:polynametag:U0L3xRrU")
    modCompileOnly("maven.modrinth:vanillahud:Gpl9yiBF")
    modRuntimeOnly("maven.modrinth:compose-multiplatform:un1Ye4Ye")
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}
kotlin { jvmToolchain(25) }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
}
loom {
    runs {
        named("client") {
            property("mixin.debug.export", "true")
            if (System.getProperty("os.name").contains("Mac")) vmArg("-XstartOnFirstThread")
        }
        remove(getByName("server"))
    }
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
sourceSets {
    main {
        java.srcDir(rootProject.file("src/ornithe/main/java"))
        kotlin.srcDir(rootProject.file("src/ornithe/main/kotlin"))
        resources.srcDir(rootProject.file("src/ornithe/main/resources"))
    }
    test { java.srcDir(rootProject.file("src/ornithe/test/java")) }
}
tasks.test { useJUnit() }

// Resolve through Gradle's cached transform before Loom constructs its mod classpath.
oneConfigBundle.files.forEach { dependencies.add("modRuntimeOnly", files(it)) }

// Optional locally supplied rendering optimizer for reproducing OneClient combinations.
providers.gradleProperty("argentumJar").orNull?.let { argentumJar ->
    val optimizerBundle = configurations.create("optimizerBundle") {
        isTransitive = false
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "unpacked-mod-bundle")
    }
    dependencies.add(optimizerBundle.name, files(argentumJar))
    optimizerBundle.files.forEach { dependencies.add("modRuntimeOnly", files(it)) }
}

// Optional compatibility matrix: ./gradlew :1.8.9-ornithe:runClient -PcompatMods
if (providers.gradleProperty("compatMods").isPresent) {
    dependencies {
        modRuntimeOnly("maven.modrinth:hitbox:lF5nB8Es")
        modRuntimeOnly("maven.modrinth:polynametag:U0L3xRrU")
        modRuntimeOnly("maven.modrinth:vanillahud:$vanillaHudVersion")
    }
}

if (providers.gradleProperty("clientTest").isPresent) {
    val clientTest = sourceSets.create("clientTest") {
        java.srcDir(rootProject.file("src/ornithe/clientTest/java"))
        resources.srcDir(rootProject.file("src/ornithe/clientTest/resources"))
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + output
    }
    loom.mods.register("mellow-client-tests") { sourceSet(clientTest) }
    val testDirectory = layout.buildDirectory.dir(if (providers.gradleProperty("compatMods").isPresent) "client-test/compat${if (oneClientCurrent) "-current" else if (oneClientBaseline) "-oneclient" else ""}" else "client-test/base").get().asFile
    val result = testDirectory.resolve("smoke-result.txt")
    loom.runs.named("client") { runDir(testDirectory.absolutePath) }
    tasks.named<JavaExec>("runClient") {
        systemProperty("mellow.smokeResult", result.absolutePath)
        doFirst { result.delete() }
        doLast { check(result.isFile && result.readText() == "PASS") { "Client smoke test did not pass; inspect the client log" } }
        dependsOn(clientTest.classesTaskName)
        classpath += clientTest.runtimeClasspath
    }
}
