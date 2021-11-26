// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
            as org.gradle.accessors.dm.LibrariesForLibs
        classpath(libs.bundles.gradlePlugins)
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlinx.kover") version "0.4.2"
}

kover {
    isEnabled = true
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ)
    generateReportOnCheck.set(true)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        enableExperimentalRules.set(true)
        verbose.set(true)
        filter {
            exclude { it.file.path.contains("build/") }
        }
    }

    afterEvaluate {
        tasks.named("check").configure {
            dependsOn(tasks.getByName("ktlintCheck"))
        }

        tasks.withType(Test::class).configureEach {
            extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
                isEnabled = true
                excludes = listOf(".*BuildConfig.*")
                binaryReportFile.set(file("$buildDir/kover/$name/report.bin"))
            }
        }

        tasks.withType(kotlinx.kover.tasks.KoverHtmlReportTask::class).configureEach {
            isEnabled = true
        }

        tasks.withType(kotlinx.kover.tasks.KoverXmlReportTask::class).configureEach {
            isEnabled = true
            xmlReportFile.set(file("$buildDir/kover/${name}/report.xml"))
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
