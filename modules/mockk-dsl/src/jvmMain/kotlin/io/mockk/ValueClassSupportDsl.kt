package io.mockk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Provides value class support in the `mockk-dsl-jvm` subproject.
 *
 * This is marked as internal so that it won't clash with the other class in `mockk-agent-jvm`.
 *
 * TODO this class is copy-pasted and should be de-duplicated, see https://github.com/mockk/mockk/issues/857
 */
internal object ValueClassSupportDsl {

    /**
     * Underlying property value of a **`value class`** or self.
     *
     * The type of the return might also be a `value class`!
     */
    val <T : Any> T.boxedValue: Any?
        @Suppress("UNCHECKED_CAST")
        get() = if (!this::class.isValue_safe) {
            this
        } else {
            (this::class as KClass<T>).boxedProperty.get(this)
        }

    /**
     * Underlying property class of a **`value class`** or self.
     *
     * The returned class might also be a `value class`!
     */
    val KClass<*>.boxedClass: KClass<*>
        get() = if (!this.isValue_safe) {
            this
        } else {
            this.boxedProperty.returnType.classifier as KClass<*>
        }

    private val valueClassFieldCache = mutableMapOf<KClass<out Any>, KProperty1<out Any, *>>()

    /**
     * Underlying property of a **`value class`**.
     *
     * The underlying property might also be a `value class`!
     */
    private val <T : Any> KClass<T>.boxedProperty: KProperty1<T, *>
        get() = if (!this.isValue_safe) {
            throw UnsupportedOperationException("$this is not a value class")
        } else {
            @Suppress("UNCHECKED_CAST")
            valueClassFieldCache.getOrPut(this) {
                // value classes always have exactly one property with a backing field
                this.declaredMemberProperties.first { it.javaField != null }.apply { isAccessible = true }
            } as KProperty1<T, *>
        }

    /**
     * Returns `true` if calling [KClass.isValue] is safe.
     *
     * (In some instances [KClass.isValue] can throw an exception.)
     */
    private val <T : Any> KClass<T>.isValue_safe: Boolean
        get() = try {
            this.isValue
        } catch (_: UnsupportedOperationException) {
            false
        }
}
