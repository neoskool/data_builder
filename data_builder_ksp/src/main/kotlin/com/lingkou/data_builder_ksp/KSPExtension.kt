package com.lingkou.data_builder_ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.KModifier
import kotlin.math.max
import kotlin.math.min

fun KSValueParameter.asPropertyModifiers(resolver: Resolver): Array<KModifier> {
    return if (isVariable(resolver))
        arrayOf(KModifier.PRIVATE)
    else
        arrayOf(KModifier.PRIVATE, KModifier.LATEINIT)
}


fun KSValueParameter.nullable(): Boolean {
    return type.resolve().nullability == Nullability.NULLABLE
}

fun KSValueParameter.isVariable(resolver: Resolver): Boolean {
    return nullable() || type.resolve().isPrimitiveForm(resolver)
}

fun KSType.isPrimitiveForm(resolver: Resolver): Boolean {
    val builtIns = resolver.builtIns

    return when(this) {
        builtIns.intType, builtIns.longType, builtIns.byteType, builtIns.charType,
        builtIns.booleanType, builtIns.floatType, builtIns.doubleType, builtIns.shortType -> {
            true
        }
        else -> false
    }
}

fun KSType.defaultPrimitiveStringValue(resolver: Resolver): String {
    val builtIns = resolver.builtIns
    return when(this) {
        builtIns.intType, builtIns.byteType, builtIns.shortType -> "0"
        builtIns.longType -> "0L"
        builtIns.byteType -> "0"
        builtIns.charType -> "\'\'"
        builtIns.booleanType -> "false"
        builtIns.floatType -> "0f"
        builtIns.doubleType -> "0.0"
        builtIns.stringType -> "\"\""
        else -> "null"
    }
}

fun String.firstUppercase(): String {
    if (isEmpty()) return this
    val first = first().uppercaseChar()
    return first + substring(1, length)
}