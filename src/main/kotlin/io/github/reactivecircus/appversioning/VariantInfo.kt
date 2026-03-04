package io.github.reactivecircus.appversioning

import java.io.Serializable

class VariantInfo(
    val buildType: String?,
    val flavorName: String,
    val variantName: String
) : Serializable {
    val isDebugBuild: Boolean get() = buildType == "debug"
    val isReleaseBuild: Boolean get() = buildType == "release"

    companion object {
        private const val serialVersionUID = 1L
    }
}
