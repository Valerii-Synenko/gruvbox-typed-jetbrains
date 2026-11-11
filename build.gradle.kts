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
    testImplementation("com.google.code.gson:gson:2.15.0")

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

// The Dark Hard theme and scheme are the Dark / Dark Medium ones shifted to the bg0_h background,
// generated at build time so the variants never drift apart. Edit only the Medium sources.
val generateHardVariants by tasks.registering {
    val resources = layout.projectDirectory.dir("src/main/resources")
    val schemeSource = resources.file("colors/Gruvbox Typed Dark Medium.xml")
    val themeSource = resources.file("themes/GruvboxTypedDark.theme.json")
    val outputDir = layout.buildDirectory.dir("generated/hardVariants")
    inputs.files(schemeSource, themeSource)
    outputs.dir(outputDir)

    doLast {
        // Every replacement must apply, so changes in the sources can't silently break the Hard variants
        fun String.swap(old: String, new: String): String {
            check(old in this) { "Hard variant generation: '$old' not found" }
            return replace(old, new)
        }

        fun write(path: String, text: String) = outputDir.get().file(path).asFile.apply {
            parentFile.mkdirs()
            writeText(text)
        }

        write(
            "colors/Gruvbox Typed Dark Hard.xml",
            schemeSource.asFile.readText()
                .swap("""<scheme name="Gruvbox Typed Dark Medium"""", """<scheme name="Gruvbox Typed Dark Hard"""")
                .swap("""">Gruvbox Typed Dark Medium</property>""", """">Gruvbox Typed Dark Hard</property>""")
                .swap("""Gruvbox Typed Dark Medium editor color scheme.""", """Gruvbox Typed Dark Hard editor color scheme (generated from Dark Medium).""")
                // bg0 -> bg0_h for every background that uses it
                .swap("""value="282828"""", """value="1d2021"""")
                // bg0_s -> bg0 for the caret row, so it stays visible on the darker background
                .swap("""<option name="CARET_ROW_COLOR" value="32302f" />""", """<option name="CARET_ROW_COLOR" value="282828" />"""),
        )

        write(
            "themes/GruvboxTypedDarkHard.theme.json",
            themeSource.asFile.readText()
                .swap(""""name": "Gruvbox Typed Dark",""", """"name": "Gruvbox Typed Dark Hard",""")
                .swap(""""editorScheme": "/colors/Gruvbox Typed Dark Medium.xml",""", """"editorScheme": "/colors/Gruvbox Typed Dark Hard.xml",""")
                // Islands (panels, editor) move to bg0_h, inputs and popups to bg0
                .swap(""""bg0": "#282828",""", """"bg0": "#1d2021",""")
                .swap(""""bg0_s": "#32302f",""", """"bg0_s": "#282828",""")
                // The gap between islands and the chrome must be darker than the islands, and the palette has
                // nothing darker than bg0_h, so the Hard variant uses an extra off-palette shade
                .swap(""""bg0_h": "#1d2021",""", """"bg0_h": "#141617","""),
        )
    }
}

sourceSets.main {
    resources.srcDir(generateHardVariants)
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
    // Release tags are created without a "v" prefix (see .github/workflows/build.yml)
    versionPrefix = ""
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}
