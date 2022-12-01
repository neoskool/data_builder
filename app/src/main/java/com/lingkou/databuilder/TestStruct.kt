package com.lingkou.databuilder

import com.lingkou.data_builder_annotations.FieldAlias
import com.lingkou.data_builder_annotations.GenericNullable
import com.lingkou.data_builder_annotations.Mutable
import com.lingkou.data_builder_annotations.StructCreator
import com.lingkou.data_builder_ksp.StructBuilder

@StructCreator(companionable = true)
data class TestStruct(
    val test1: String,
    val test2: Int?,
    val test4: Boolean,
    val test3: Long,
    val testEnum: TestEnum,
    @Mutable
    @GenericNullable(nullables = [true])
    val testMutableList: MutableList<String?>,

    @Mutable
    @GenericNullable(nullables = [true, true])
    val testMutableMap: MutableMap<String?, String?>,

    @FieldAlias(Dog::class) private val testInterface1: InterfaceAnimal?,
    @FieldAlias(Cat::class) private val testInterface2: InterfaceAnimal?,
) {
    companion object {

    }
}
@StructBuilder
data class TestKSPStruct(
    val test1: String,
    val test2: Int?,
    val test3: Boolean = true,
    val test4: Long = 10L,
    val testEnum5: TestEnum = TestEnum.TEST,
    val testMutableList: MutableList<String?>,
    val testMutableMap: MutableMap<String?, String?>,

    private val testInterface1: InterfaceAnimal?,
    private val testInterface2: InterfaceAnimal?,
) {
    companion object {

    }
}

@StructBuilder
data class TestGeneric <T> (
    val test1: String,
    val testB: T
)

@StructBuilder
data class TestGeneric2 <out T: InterfaceAnimal> (
    val test1: String,
    val testB: T
)

interface InterfaceAnimal

interface Dog : InterfaceAnimal

interface Cat : InterfaceAnimal

enum class TestEnum {
    TEST
}