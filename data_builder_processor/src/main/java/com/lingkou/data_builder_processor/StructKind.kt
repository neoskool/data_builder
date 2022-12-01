package com.lingkou.data_builder_processor

import com.squareup.kotlinpoet.ANNOTATION
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.COLLECTION
import com.squareup.kotlinpoet.COMPARABLE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.ENUM
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.ITERABLE
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.NUMBER
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.THROWABLE
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_INT_ARRAY
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_LONG_ARRAY
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.U_SHORT_ARRAY

sealed class NullableKind(mNullable: Boolean, mClassName: TypeName) {
    val typeName: TypeName = mClassName.copy(nullable = mNullable)
}

sealed class StructKind(
    typeName: TypeName,
    private val nullable: Boolean,
    private val default: String? = null,
) : NullableKind(nullable, typeName) {
    open fun initializer(): String = if (nullable) "null" else (default ?: "null")

    open fun copyClassName(nullable: Boolean): TypeName = typeName.copy(nullable)

    val lateinitable: Boolean = !nullable && default == null

    val modifier: Array<KModifier> = if (lateinitable) { // 不可空并且他的默认值为null
        arrayOf(KModifier.PRIVATE, KModifier.LATEINIT)
    } else {
        arrayOf(KModifier.PRIVATE)
    }
}

sealed class PrimitiveStructKind(
    typeName: ClassName,
    nullable: Boolean,
    default: String,
) : StructKind(typeName, nullable, default)

class BooleanKind(nullable: Boolean) : PrimitiveStructKind(BOOLEAN, nullable, "false")
class ByteKind(nullable: Boolean) : PrimitiveStructKind(BYTE, nullable, "0")
class ShortKind(nullable: Boolean) : PrimitiveStructKind(SHORT, nullable, "0")
class IntKind(nullable: Boolean) : PrimitiveStructKind(INT, nullable, "0")
class LongKind(nullable: Boolean) : PrimitiveStructKind(LONG, nullable, "0L")
class CharKind(nullable: Boolean) : PrimitiveStructKind(CHAR, nullable, "\' \'")
class FloatKind(nullable: Boolean) : PrimitiveStructKind(FLOAT, nullable, "0f")
class DoubleKind(nullable: Boolean) : PrimitiveStructKind(DOUBLE, nullable, "0.0")
class StringKind(nullable: Boolean) : PrimitiveStructKind(STRING, nullable, "\"\"")

open class ArrayKind(
    nullable: Boolean,
    genericKind: StructKind = AnyKind(true),
    genericTypeName: TypeName = ARRAY.plusParameter(genericKind.typeName)
) : StructKind(genericTypeName, nullable)

class CharSequenceKind(nullable: Boolean) : StructKind(CHAR_SEQUENCE, nullable, "\"\"")
class ComparableKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(COMPARABLE.plusParameter(genericKind.typeName), nullable)

class ThrowableKind(nullable: Boolean) : StructKind(THROWABLE, nullable)
class AnnotationKind(nullable: Boolean) : StructKind(ANNOTATION, nullable)
class NumberKind(nullable: Boolean) : StructKind(NUMBER, nullable)
class IterableKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(ITERABLE.plusParameter(genericKind.typeName), nullable)

class CollectionKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(COLLECTION.plusParameter(genericKind.typeName), nullable)

// val a = ListKind(true, StingKind(false)) => List<String>?
class ListKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(LIST.plusParameter(genericKind.typeName), nullable)

class SetKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(SET.plusParameter(genericKind.typeName), nullable)

class MapKind(nullable: Boolean, keyGenericKind: StructKind, valueGenericKind: StructKind) :
    StructKind(MAP.parameterizedBy(keyGenericKind.typeName, valueGenericKind.typeName), nullable)

class MapEntry(nullable: Boolean, keyGenericKind: StructKind, valueGenericKind: StructKind) :
    StructKind(
        MAP_ENTRY.parameterizedBy(keyGenericKind.typeName, valueGenericKind.typeName),
        nullable
    )

class MutableIterableKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(MUTABLE_ITERABLE.plusParameter(genericKind.typeName), nullable)

class MutableCollectionKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(MUTABLE_COLLECTION.plusParameter(genericKind.typeName), nullable)

class MutableListKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(MUTABLE_LIST.plusParameter(genericKind.typeName), nullable)

class MutableSetKind(nullable: Boolean, genericKind: StructKind) :
    StructKind(MUTABLE_SET.plusParameter(genericKind.typeName), nullable)

class MutableMapKind(nullable: Boolean, keyGenericKind: StructKind, valueGenericKind: StructKind) :
    StructKind(
        MUTABLE_MAP.parameterizedBy(keyGenericKind.typeName, valueGenericKind.typeName),
        nullable
    )

class MutableMapEntryKind(nullable: Boolean, keyGenericKind: StructKind, valueGenericKind: StructKind) :
    StructKind(
        MUTABLE_MAP_ENTRY.parameterizedBy(
            keyGenericKind.typeName,
            valueGenericKind.typeName
        ), nullable
    )

class BooleanArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = BOOLEAN_ARRAY)
class ByteArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = BYTE_ARRAY)
class CharArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = CHAR_ARRAY)
class ShortArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = SHORT_ARRAY)
class IntArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = INT_ARRAY)
class LongArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = LONG_ARRAY)
class FloatArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = FLOAT_ARRAY)
class DoubleArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = DOUBLE_ARRAY)

class UByteKind(nullable: Boolean) : StructKind(U_BYTE, nullable)
class UShortKind(nullable: Boolean) : StructKind(U_SHORT, nullable)
class UIntKind(nullable: Boolean) : StructKind(U_INT, nullable)
class ULongKind(nullable: Boolean) : StructKind(U_LONG, nullable)

class UByteArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = U_BYTE_ARRAY)
class UShortArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = U_SHORT_ARRAY)
class UIntArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = U_INT_ARRAY)
class ULongArrayKind(nullable: Boolean) : ArrayKind(nullable, genericTypeName = U_LONG_ARRAY)

class EnumKind(nullable: Boolean, genericKind: StructKind) : StructKind(ENUM.parameterizedBy(genericKind.typeName), nullable)
class AnyKind(nullable: Boolean) : StructKind(ANY, nullable)
object NothingKind : StructKind(NOTHING, false, "Nothing")
object UnitKind : StructKind(UNIT, false, "Unit")

class CustomKind(
    nullable: Boolean,
    packageName: String,
    genericKind: GenericKind? = null,
    default: String? = null,
    vararg simpleName: String,
) : StructKind(
    typeName = ClassName(packageName, *simpleName).apply {
        if (genericKind != null) {
            parameterizedBy(*genericKind.parameterized)
        }
    },
    nullable = nullable,
    default = default,
)

class AliasKind(
    typeName: TypeName,
    nullable: Boolean,
    default: String? = null
) : StructKind(typeName, nullable, default)

class GenericKind(vararg kinds: StructKind) {
    val parameterized: Array<out TypeName> = Array(kinds.size) { index -> kinds[index].typeName }
}

fun String.asStructKind(
    nullable: Boolean,
    isMutable: Boolean = false,
    isUField: Boolean = false,
    genericNullableList: MutableList<Boolean>? = null,
): StructKind {
    if (this.isEmpty()) return UnitKind

    val primitiveKind = asPrimitiveKind(nullable, isUField)
    if (primitiveKind != null) {
        return primitiveKind
    }

    val otherKind = asOtherKind(nullable)
    if (otherKind != null) {
        return otherKind
    }

    val arrayKind = asArrayKind(nullable, isUField, genericNullableList)
    if (arrayKind != null) {
        return arrayKind
    }

    val genericKind = asGenericKind(nullable, isMutable, genericNullableList)
    if (genericKind != null) {
        return genericKind
    }

    val packageName = substringBeforeLast('.')
    val simpleName = substringAfterLast('.')
    return CustomKind(nullable, packageName = packageName, simpleName = arrayOf(simpleName))
}

fun String.asPrimitiveKind(nullable: Boolean, isUField: Boolean): StructKind? = when {
    this == "java.lang.String" -> StringKind(nullable)
    this == "java.lang.Integer" -> {
        if (isUField) UIntKind(nullable) else IntKind(nullable)
    }
    this == "long" -> {
        if (isUField) ULongKind(nullable) else LongKind(nullable)
    }
    this == "boolean" -> BooleanKind(nullable)
    this == "float" -> FloatKind(nullable)
    this == "double" -> DoubleKind(nullable)
    this == "byte" -> {
        if (isUField) UByteKind(nullable) else ByteKind(nullable)
    }
    this == "short" -> {
        if (isUField) UShortKind(nullable) else ShortKind(nullable)
    }
    else -> null
}

fun String.asArrayKind(
    nullable: Boolean,
    isUField: Boolean,
    genericNullableList: MutableList<Boolean>?,
): StructKind? = when {
    this == "boolean[]" -> BooleanArrayKind(nullable)
    this == "char[]" -> CharArrayKind(nullable)
    this == "byte[]" -> {
        if (isUField) UByteArrayKind(nullable) else ByteArrayKind(nullable)
    }
    this == "short[]" -> {
        if (isUField) UShortArrayKind(nullable) else ShortArrayKind(nullable)
    }
    this == "int[]" -> {
        if (isUField) UIntArrayKind(nullable) else IntArrayKind(nullable)
    }
    this == "long[]" -> {
        if (isUField) ULongArrayKind(nullable) else LongArrayKind(nullable)
    }
    this == "float[]" -> FloatArrayKind(nullable)
    this == "double[]" -> DoubleArrayKind(nullable)
    MembersFieldFilter.isJavaArray(this) -> {
        val beforeStructKindStr = substringBeforeLast('[', "")
        if (beforeStructKindStr.isEmpty()) {
            null
        } else {
            val genericNullable = genericNullableList?.getOrNull(0) ?: false
            val deepGenericNullable = genericNullableList?.subList(1, genericNullableList.lastIndex)
            val structKind = beforeStructKindStr.asStructKind(genericNullable, genericNullableList = deepGenericNullable)
            ArrayKind(nullable, genericKind = structKind)
        }
    }
    else -> null
}

fun String.asOtherKind(nullable: Boolean): StructKind? = when {
    this == "java.lang.CharSequence" -> CharSequenceKind(nullable)
    this == "java.lang.Throwable" -> ThrowableKind(nullable)
    this == "java.lang.annotation.Annotation" -> AnnotationKind(nullable)
    this == "java.lang.Number" -> NumberKind(nullable)
    this == "java.lang.Void" -> NothingKind
    else -> null
}

fun String.asGenericKind(
    nullable: Boolean,
    isMutable: Boolean,
    genericNullableList: MutableList<Boolean>?,
): StructKind? {
    if (!MembersFieldFilter.isGeneric(this)) return null
    val originKindStr = substringBefore('<')
    val genericKindStr = substringAfter('<')
        .substringBeforeLast('>')

    val genericKinds: List<StructKind> = if (genericKindStr.contains(',')) {
        val splitGenericKinds = genericKindStr.split(',')
        val genericChildrenList = genericNullableList?.subList(0, splitGenericKinds.size)
        val deepChildrenList = genericNullableList?.subList(
            kotlin.math.min(genericNullableList.size, splitGenericKinds.size + 1),
            genericNullableList.size
        )
        splitGenericKinds.mapIndexed { index, s ->
            s.asStructKind(
                nullable = genericChildrenList?.getOrNull(index) ?: false,
                genericNullableList = deepChildrenList
            )
        }
    } else {
        listOf(
            genericKindStr.asStructKind(
                nullable = genericNullableList?.getOrNull(0) ?: false,
                genericNullableList = genericNullableList?.subList(
                    kotlin.math.min(genericNullableList.lastIndex, 1),
                    genericNullableList.lastIndex
                )
            )
        )
    }


    val originKind = when (originKindStr) {
        "java.lang.Enum" -> EnumKind(nullable, genericKinds.first())
        "java.lang.Comparable" -> ComparableKind(nullable, genericKinds.first())
        "java.lang.Iterable" -> {
            if (isMutable)
                MutableIterableKind(nullable, genericKinds.first())
            else
                IterableKind(nullable, genericKinds.first())
        }
        "java.util.Collection" -> {
            if (isMutable)
                MutableCollectionKind(nullable, genericKinds.first())
            else
                CollectionKind(nullable, genericKinds.first())
        }
        "java.util.List" -> {
            if (isMutable)
                MutableListKind(nullable, genericKinds.first())
            else
                ListKind(nullable, genericKinds.first())
        }
        "java.util.Set" -> {
            if (isMutable)
                MutableSetKind(nullable, genericKinds.first())
            else
                SetKind(nullable, genericKinds.first())
        }
        "java.util.Map" -> {
            if (isMutable)
                MutableMapKind(nullable, genericKinds.component1(), genericKinds.component2())
            else
                MapKind(nullable, genericKinds.component1(), genericKinds.component2())
        }
        "java.util.Map.Entry" -> {
            if (isMutable)
                MutableMapEntryKind(nullable, genericKinds.component1(), genericKinds.component2())
            else
                MapEntry(nullable, genericKinds.component1(), genericKinds.component2())
        }
        else -> null
    }
    return originKind
}