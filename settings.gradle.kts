include(":app", ":shared")
rootProject.name = "KaMPKit"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/jb-coverage/maven") }
    }
}

enableFeaturePreview("VERSION_CATALOGS")
