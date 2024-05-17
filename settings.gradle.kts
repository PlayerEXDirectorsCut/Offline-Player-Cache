pluginManagement {
    repositories {
        maven("https://maven.minecraftforge.net")
        maven { url = uri("https://maven.parchmentmc.org") }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "offline-player-cache"
