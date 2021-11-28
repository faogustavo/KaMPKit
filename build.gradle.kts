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

plugins {
    id("org.jetbrains.kotlinx.kover") version "0.4.2"
}

kover {
    isEnabled = true
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.JACOCO)
    jacocoEngineVersion.set("0.8.7")
    generateReportOnCheck.set(true)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Android is including the 0.8.3 version of JaCoCo that doesn't work
    // with kotlin 1.5+ - https://stackoverflow.com/a/69212737/6839227
    configurations.all {
        resolutionStrategy {
            eachDependency {
                if ("org.jacoco" == requested.group) {
                    useVersion("0.8.7")
                }
            }
        }
    }

    afterEvaluate {
        tasks.withType(Test::class).configureEach {
            extensions.configure<kotlinx.kover.api.KoverTaskExtension> {
                isEnabled = true
                excludes = listOf(".*BuildConfig.*")
                binaryReportFile.set(file("$buildDir/kover/$name/report.bin"))
            }
        }

        tasks.withType<kotlinx.kover.tasks.KoverHtmlReportTask>().configureEach {
            isEnabled = true
            htmlReportDir.set(file("$buildDir/kover/${name}/html"))
        }

        tasks.withType<kotlinx.kover.tasks.KoverXmlReportTask>().configureEach {
            isEnabled = true
            xmlReportFile.set(file("$buildDir/kover/${name}/report.xml"))
        }
    }
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
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
