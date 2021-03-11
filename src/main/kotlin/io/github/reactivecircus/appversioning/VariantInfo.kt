package io.github.reactivecircus.appversioning

import org.gradle.language.nativeplatform.internal.BuildType
import java.io.Serializable

class VariantInfo(
    val buildType: String?,
    val flavorName: String,
    val variantName: String
) : Serializable {
    val isDebugBuild: Boolean get() = buildType == BuildType.DEBUG.name
    val isReleaseBuild: Boolean get() = buildType == BuildType.RELEASE.name

    companion object {
        const val serialVersionUID = 1L
    }
}
