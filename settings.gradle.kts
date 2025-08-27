pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // Remove the content filter
        mavenCentral()
    }
    versionCatalogs {
        create("testLibs") {
            from(files("gradle/libs.versions.toml"))
        }

    }
}

rootProject.name = "QuickQR"
include(":app")