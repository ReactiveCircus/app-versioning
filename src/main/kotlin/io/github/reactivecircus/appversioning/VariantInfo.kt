package io.github.reactivecircus.appversioning

import java.io.Serializable

class VariantInfo internal constructor(
    val buildType: String?,
    val flavorName: String,
    val variantName: String
) : Serializable {
    val isDebugBuild: Boolean get() = buildType == "debug"
    val isReleaseBuild: Boolean get() = buildType == "release"

    internal companion object {
        @Suppress("Unused")
        private const val serialVersionUID = 1L
    }
}
