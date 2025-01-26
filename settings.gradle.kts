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
        google()
        mavenCentral()
    }
}

rootProject.name = "FLyricViewDemo"
include(":app")
include(":FLyric:FLyricParser")
include(":FLyric:FLyricUI")
include(":mediamixer")
include(":mediamixer2")
