import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.13.1")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim) })
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        // Get the latest available change notes from the changelog file
        val changelog = project.changelog
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// The Dark Hard scheme is the Dark Medium scheme on the bg0_h background, generated so the two never drift apart
val generateHardScheme by tasks.registering {
    val source = layout.projectDirectory.file("src/main/resources/colors/Gruvbox Typed Dark Medium.xml")
    val outputDir = layout.buildDirectory.dir("generated/hardScheme")
    inputs.file(source)
    outputs.dir(outputDir)

    doLast {
        val medium = source.asFile.readText()
        val hard = medium
            .replace("""<scheme name="Gruvbox Typed Dark Medium"""", """<scheme name="Gruvbox Typed Dark Hard"""")
            .replace("""">Gruvbox Typed Dark Medium</property>""", """">Gruvbox Typed Dark Hard</property>""")
            .replace("""Gruvbox Typed Dark Medium editor color scheme.""", """Gruvbox Typed Dark Hard editor color scheme (generated from Dark Medium).""")
            // bg0 -> bg0_h for every background that uses it
            .replace("""value="282828"""", """value="1d2021"""")
            // bg0_s -> bg0 for the caret row, so it stays visible on the darker background
            .replace("""<option name="CARET_ROW_COLOR" value="32302f" />""", """<option name="CARET_ROW_COLOR" value="282828" />""")
        check(hard != medium && "Gruvbox Typed Dark Hard" in hard) { "Dark Hard scheme generation did not apply" }

        outputDir.get().file("colors/Gruvbox Typed Dark Hard.xml").asFile.apply {
            parentFile.mkdirs()
            writeText(hard)
        }
    }
}

sourceSets.main {
    resources.srcDir(generateHardScheme)
}

tasks {
    test {
        // ./gradlew test --tests '*ReadmeScreenshotsTest*' -Pscreenshots regenerates the README images
        if (providers.gradleProperty("screenshots").isPresent) {
            systemProperty("gruvbox.screenshots.dir", layout.projectDirectory.dir(".github/readme").asFile.absolutePath)
            systemProperty("gruvbox.demo.file", layout.projectDirectory.file("demo/type_hints.py").asFile.absolutePath)
        }
    }

    runIde {
        // Open the demo file used for README screenshots
        args(layout.projectDirectory.dir("demo").asFile.absolutePath)
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}
