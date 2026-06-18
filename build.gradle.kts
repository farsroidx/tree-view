
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "ir.farsroidx"
version = "1.1.1"

intellijPlatform {

    this.buildSearchableOptions = false
    this.instrumentCode         = false
    this.projectName            = project.name

    this.publishing {
        this.token.set(System.getenv("PUBLISH_TOKEN"))
    }

    this.signing {
        this.certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        this.privateKey.set(System.getenv("PRIVATE_KEY"))
        this.password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
}

tasks.jar { archiveBaseName.set("TreeView") }

tasks {
    runIde {
        args = listOf(
            "P:/Java/Frsx"
        )
    }
}

tasks {

    withType<JavaCompile> {
        sourceCompatibility = JvmTarget.JVM_17.target
        targetCompatibility = JvmTarget.JVM_17.target
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(
                JvmTarget.JVM_17
            )
        }
    }

    patchPluginXml {
        sinceBuild.set("261")
        untilBuild = provider { null }
    }
}

tasks.register("export") {
    group = "build"
    description = "Builds and Signs plugin"
    dependsOn("clean", "buildPlugin", "signPlugin")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {

    testImplementation(libs.junit)

    intellijPlatform {

        local("S:\\IntelliJ IDEA\\idea")

        testFramework(TestFrameworkType.Platform)

        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.idea.maven")
    }
}