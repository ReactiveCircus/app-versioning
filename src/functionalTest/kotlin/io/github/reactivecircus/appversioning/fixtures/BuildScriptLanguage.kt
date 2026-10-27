package io.github.reactivecircus.appversioning.fixtures

import com.autonomousapps.kit.GradleProject

enum class BuildScriptLanguage {
    Kts,
    Groovy;

    internal val dslKind: GradleProject.DslKind
        get() = if (this == Kts) GradleProject.DslKind.KOTLIN else GradleProject.DslKind.GROOVY
}
