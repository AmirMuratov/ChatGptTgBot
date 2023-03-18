plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("bot.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.telegram:telegrambots:6.5.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.slf4j:jul-to-slf4j:2.0.6")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.14.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}