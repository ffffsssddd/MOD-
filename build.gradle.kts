plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.bedrockac"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.geysermc.org/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/") // PacketEvents
    maven("https://jitpack.io")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Spigot API
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    
    // Floodgate (Required - Bedrock Detection)
    compileOnly(files("libs/floodgate-spigot.jar"))
    
    // PacketEvents (Enhanced Detection)
    implementation("com.github.retrooper:packetevents-spigot:2.2.1")
    
    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    
    // Caffeine Cache (Performance)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.github.retrooper.packetevents", "com.bedrockac.packetevents")
        relocate("com.zaxxer.hikari", "com.bedrockac.hikari")
        relocate("com.github.benmanes.caffeine", "com.bedrockac.caffeine")
        
        manifest {
            attributes["Main-Class"] = "com.bedrockac.BedrockAC"
        }
    }
    
    build {
        dependsOn(shadowJar)
    }
}