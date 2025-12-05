plugins {
    java
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.geysermc.org/repository/maven-public/")
    maven("https://jitpack.io")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly(files("libs/floodgate-spigot.jar"))
    compileOnly(files("libs/geyser-spigot.jar"))
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.example.bedrockac.BedrockAC"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}
