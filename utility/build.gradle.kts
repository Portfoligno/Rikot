plugins {
  maven
  `java-library`
  kotlin("jvm")
}

dependencies {
  implementation(kotlin("stdlib"))

  api("io.arrow-kt", "arrow-core-data", "0.10.5")
}
