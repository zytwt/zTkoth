plugins {
    java
}

group = "com.ejemplo"
version = "1.0.0"

val paperVersion = providers.gradleProperty("paperVersion").orElse("1.21.11")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${paperVersion.get()}-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit") }
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveClassifier.set("paper-${paperVersion.get()}")
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
