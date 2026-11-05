pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "JioJoinLite"
include(":app")

// PJSIP Android service and its bundled Java/JNI bindings.
include(":sipservice")
project(":sipservice").projectDir = file("third_party/pjsip-android/sipservice")
