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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Digital_OBD-II"
include(":app")

// ─── Módulo de Áudio V6 Twin-Turbo ───────────────────────────────────────────
// Referencia o módulo engine-audio de outro diretório no filesystem.
// Desta forma não é preciso copiar código — ele fica em Engine_Sounds e é
// compartilhado como submódulo local neste projeto.
include(":engine-audio")