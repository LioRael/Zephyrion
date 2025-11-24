import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "2.0.27"
    kotlin("jvm") version "1.9.0"
}

val exposedVersion: String by project

taboolib {
    env {
        install(
            Basic,
            Bukkit,
            BukkitHook,
            BukkitNMS,
            BukkitNMSUtil,
            BukkitUI,
            BukkitUtil,
            CommandHelper,
            I18n,
            Kether,
            MinecraftChat,
            Metrics,

        )
        enableIsolatedClassloader  = true
    }
    version {
        taboolib = "6.2.4-abd325ee"
        skipKotlinRelocate = true
    }
    description{
        contributors {
            name("LioRael")
            name("Sting")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("ink.ptms.core:v12107:12107:universal")
    compileOnly("ink.ptms.core:v12107:12107:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("com.zaxxer:HikariCP:5.0.1")
    compileOnly("org.jetbrains.exposed:exposed-core:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.tabooproject.org/repository/releases")
            credentials {
                username = project.findProperty("taboolibUsername").toString()
                password = project.findProperty("taboolibPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = project.group.toString()
        }
    }
}