plugins {
  maven
  `java-library`
  kotlin("jvm")
}

tasks.compileKotlin {
  kotlinOptions.freeCompilerArgs += "-Xinline-classes"
}

dependencies {
  implementation(kotlin("stdlib"))
  api(project(":runtime"))
  api(project(":utility"))

  api("com.squareup", "kotlinpoet", "1.6.0")
}
