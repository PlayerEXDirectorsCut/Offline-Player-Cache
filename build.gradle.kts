import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    kotlin("jvm") version "1.9.22"
    id("net.minecraftforge.gradle") version "6.+"
    id("org.parchmentmc.librarian.forgegradle") version "1+"
}

group = "${properties["group"]}"
version = "${properties["mod_version"]}-${properties["loader"]}"

repositories {
    // Add KFF Maven repository
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    // FORGE
    implementation("thedarkcolour:kotlinforforge:4.10.0")
    "minecraft"("net.minecraftforge:forge:${properties["forge_version"]}")
}

val Project.minecraft: net.minecraftforge.gradle.common.util.MinecraftExtension
    get() = extensions.getByType()

minecraft.let {
    it.mappings("${properties["mappings_channel"]}", "${properties["mappings_version"]}")
    it.runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            mods {
                this.create("opc") {
                    source(sourceSets.main.get())
                }
            }
        }
        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            mods {
                this.create("opc") {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

tasks {
    val javaVersion = JavaVersion.VERSION_17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        if (JavaVersion.current().isJava9Compatible) {
            options.release.set(javaVersion.toString().toInt())
        }
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
        java.sourceCompatibility = javaVersion
        java.targetCompatibility = javaVersion
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    withType<Jar> {
        archiveBaseName.set("offline-player-cache")
        manifest {
            attributes(
                mapOf(
                    "Specification-Title" to project.name,
                    "Specification-Vendor" to "author",
                    "Specification-Version" to "1",
                    "Implementation-Title" to project.name,
                    "Implementation-Vendor" to "author",
                    "Implementation-Version" to project.version,
                    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd").format(Date())
                )
            )
        }
        finalizedBy("reobfJar")
    }
}
