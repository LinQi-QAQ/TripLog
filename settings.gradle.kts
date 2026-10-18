pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://mirrors.cloud.tencent.com/gradle/")
        maven("https://mirrors.huaweicloud.com/repository/maven/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://repo.huaweicloud.com/repository/maven/")
    }
}

rootProject.name = "出行记"
include(":app")
