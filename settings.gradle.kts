
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
       // maven { url = uri("https://maven.google.com") }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://www.jitpack.io" ) }
        // maven { url = uri("https://maven.google.com") }


    }
}

rootProject.name = "Calculator"
include(":app")
