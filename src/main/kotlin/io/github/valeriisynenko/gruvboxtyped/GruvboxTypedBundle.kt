package io.github.valeriisynenko.gruvboxtyped

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.GruvboxTypedBundle"

object GruvboxTypedBundle : DynamicBundle(GruvboxTypedBundle::class.java, BUNDLE) {

    @JvmStatic
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
