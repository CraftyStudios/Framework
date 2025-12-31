import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.google.devtools.ksp") version "2.3.2"
    `maven-publish`
}

group = "dev.crafty"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")

    implementation("io.insert-koin:koin-core:4.2.0-beta2")

    implementation("org.reflections:reflections:0.10.2")

    implementation("dev.s7a:ktConfig:2.0.0-SNAPSHOT")
    ksp("dev.s7a:ktConfig-ksp:2.0.0-SNAPSHOT")

    implementation("com.esotericsoftware.kryo:kryo5:5.6.2")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// Publishing configuration: adds GitHub Packages (https://maven.pkg.github.com)
// Credentials are read from (in priority): gradle.properties (gpr.user/gpr.key) or
// environment variables USERNAME and TOKEN (the workflow sets these).
publishing {
    publications {
        create<MavenPublication>("gpr") {
            artifact(tasks.named("shadowJar")) {
                builtBy(tasks.named("shadowJar"))
            }

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }

    repositories {
        // Publish to GitHub Packages for this repo
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/CraftyStudios/framework") // has to be lowercase becuase of github
            credentials {
                username = (findProperty("gpr.user") as String?) ?: System.getenv("USERNAME")
                password = (findProperty("gpr.key") as String?) ?: System.getenv("TOKEN")
            }
        }

        // Also keep local publishing available for development
        mavenLocal()
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("") // remove the -all suffix
}