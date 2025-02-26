plugins {
    id("java")
}

group = "de.henrik"
version = "1.0"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "NURBSModelerGUI"
    }
}