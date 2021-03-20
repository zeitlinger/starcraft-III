buildscript {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.0"
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.9"
}

//project.ext.assetsDir = File("assets");

tasks.withType<Test> {
   useJUnitPlatform()
}

//tasks.withType<KotlinCompileCommon>().configureEach {
//    kotlinOptions {
//        incremental = true
//        jvmTarget = "11"
//        freeCompilerArgs = freeCompilerArgs + "-Xjsr305=strict"
//    }
//}

dependencies {
    val gdxVersion = "1.9.9"
    val ktorVersion = "1.5.2"
    val logbackVersion = "1.2.3"
    val kotlinSerialization = "1.1.0"
    val kotestVersion = "4.4.2"

    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerialization")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")


    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")

    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-console-jvm:4.1.3.2")



    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")

}

repositories {
    jcenter()
    mavenCentral()
}

javafx {
    version = "15.0.1"
    modules = listOf( "javafx.controls")
}
