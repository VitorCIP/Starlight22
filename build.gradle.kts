plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}
group = "br.starlight"
version = "1.0.0"
repositories { maven("https://repo.papermc.io/repository/maven-public/") }
dependencies { paperweight.paperDevBundle("26.3.build.+") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
