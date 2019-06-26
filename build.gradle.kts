plugins {
  kotlin("jvm") version "1.3.72" apply false
}

tasks.getByName<Wrapper>("wrapper") {
  gradleVersion = "6.5.1"
}

subprojects {
  repositories {
    jcenter()
    maven("https://jitpack.io")
  }
}
