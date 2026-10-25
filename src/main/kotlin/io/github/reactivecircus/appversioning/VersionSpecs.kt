package io.github.reactivecircus.appversioning

import groovy.lang.Closure
import org.codehaus.groovy.runtime.ConvertedClosure
import java.io.Serializable
import java.lang.reflect.Proxy

/**
 * Specification for generating `versionCode` from a [VersionContext].
 */
fun interface VersionCodeSpec : Serializable {
    fun VersionContext.versionCode(): Int

    companion object {
        /**
         * Converts a SemVer compliant tag to an integer using positional notation:
         * `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`.
         */
        val SemVer: VersionCodeSpec = VersionCodeSpec { tag.toVersionCodeBySemVer() }
    }
}

/**
 * Specification for generating `versionName` from a [VersionContext].
 */
fun interface VersionNameSpec : Serializable {
    fun VersionContext.versionName(): String

    companion object {
        /**
         * Uses the name of the latest git tag as the `versionName`.
         */
        val TagName: VersionNameSpec = VersionNameSpec { tag.name }
    }
}

private fun GitTag.toVersionCodeBySemVer(): Int {
    val prefix = if (name.startsWith("v")) "v" else ""
    val semVer = checkNotNull(runCatching { toSemVer(prefix = prefix) }.getOrNull()) {
        """
            Could not generate versionCode as "$name" does not follow semantic versioning.
            Please either ensure latest git tag follows semantic versioning, or provide a custom spec for generating versionCode using the `versionCode` property.
        """.trimIndent()
    }
    return runCatching {
        semVer.toVersionCode()
    }.getOrElse {
        throw IllegalStateException(
            """
                Could not generate versionCode from "$name" as the SemVer cannot be represented as an Integer.
                This is usually because MAJOR or MINOR version is greater than 99, as by default maximum of 2 digits is allowed for MINOR and PATCH components of a SemVer tag.
                Another reason might be that the overall positional notation of the SemVer (MAJOR * 10000 + MINOR * 100 + PATCH) is greater than the maximum value of an integer (2147483647).
                As a workaround you can provide a custom spec for generating versionCode using the `versionCode` property.
            """.trimIndent(),
            it,
        )
    }
}

internal fun VersionCodeSpec.asSerializable(): VersionCodeSpec {
    if (Proxy.isProxyClass(javaClass)) {
        val handler = Proxy.getInvocationHandler(this)
        if (handler is ConvertedClosure) {
            @Suppress("UNCHECKED_CAST")
            return (handler.delegate as Closure<Int>).toVersionCodeSpec()
        }
    }
    return this
}

internal fun VersionNameSpec.asSerializable(): VersionNameSpec {
    if (Proxy.isProxyClass(javaClass)) {
        val handler = Proxy.getInvocationHandler(this)
        if (handler is ConvertedClosure) {
            @Suppress("UNCHECKED_CAST")
            return (handler.delegate as Closure<CharSequence>).toVersionNameSpec()
        }
    }
    return this
}

internal fun Closure<Int>.toVersionCodeSpec(): VersionCodeSpec {
    val holder = SerializableClosure(this)
    return VersionCodeSpec { holder.call(this) }
}

internal fun Closure<CharSequence>.toVersionNameSpec(): VersionNameSpec {
    val holder = SerializableClosure(this)
    return VersionNameSpec { holder.call(this).toString() }
}

private class SerializableClosure<T>(
    closure: Closure<T>,
) : Serializable {
    private val dehydrated: Closure<T> = closure.dehydrate()

    fun call(context: VersionContext): T =
        dehydrated.rehydrate(context, context, context)
            .also { it.resolveStrategy = Closure.DELEGATE_FIRST }
            .call(context)

    @Suppress("unused")
    private fun writeReplace(): Any = SerializableClosure(dehydrated)

    companion object {
        @Suppress("unused")
        private const val serialVersionUID = 1L
    }
}
