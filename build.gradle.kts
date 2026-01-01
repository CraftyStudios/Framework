import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.google.devtools.ksp") version "2.3.2"
    `maven-publish`
}

group = "dev.crafty"
version = "1.0.11-SNAPSHOT"

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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.8.1")

    // db drivers
    implementation("org.postgresql:postgresql:42.7.7")

    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
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
    compilerOptions {
        javaParameters = true
    }
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
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

// generate sources jar
val sourcesJar by tasks.creating(org.gradle.jvm.tasks.Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets.main.get().kotlin)
}

publishing {
    publications {
        create<MavenPublication>("mavenLocal") {
            artifact(tasks.named("shadowJar")) {
                builtBy(tasks.named("shadowJar"))
            }

            artifact(sourcesJar)

            groupId = project.group.toString().replace(" ", "-")
            artifactId = project.name.lowercase().replace(" ", "-")
            version = project.version.toString().replace(" ", "-")
        }

        create<MavenPublication>("gpr") {
            artifact(tasks.named("shadowJar")) {
                builtBy(tasks.named("shadowJar"))
            }

            groupId = project.group.toString().replace(" ", "-")
            artifactId = project.name.lowercase().replace(" ", "-")
            version = project.version.toString().replace(" ", "-")
        }
    }

    repositories {
        maven {
            name = "SonatypeNexus"
            url = uri(
                if (version.toString().endsWith("-SNAPSHOT"))
                    "https://repo.craftystudios.net/repository/maven-snapshots/"
                else
                    "https://repo.craftystudios.net/repository/maven-releases/"
            )

            credentials {
                username = (findProperty("nexus.user") as String?)
                    ?: System.getenv("NEXUS_USERNAME")
                password = (findProperty("nexus.password") as String?)
                    ?: System.getenv("NEXUS_PASSWORD")
            }
        }

        mavenLocal()
    }

}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("") // remove the -all suffix
}

tasks.shadowJar {
    relocate("co.aikar.commands", "dev.crafty.framework.extern.acf.commands")
    relocate("co.aikar.locales", "dev.crafty.framework.extern.acf.locales")
}
