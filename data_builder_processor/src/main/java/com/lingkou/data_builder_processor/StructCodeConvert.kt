package com.lingkou.data_builder_processor

import com.lingkou.data_builder_annotations.StructCreator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class StructCodeConvert(
    private val element: Element,
    private val members: List<Member>,
    private val kaptKotlinGeneratedDir: String,
    private val annotations: MutableSet<out TypeElement>,
    private val roundEnv: RoundEnvironment,
    private val processingEnv: ProcessingEnvironment,
) {
    private val className: String = element.simpleName.toString()
    private val packageName: String = processingEnv.elementUtils.getPackageOf(element).toString()
    private val builderFileName: String = "${className}Extension"
    private val builderClassName: String = "${className}Builder"

    private val declaringClass: ClassName = ClassName(packageName, className)
    private val builderClass: ClassName = ClassName(packageName, builderClassName)

    fun buildFile() = FileSpec.builder(packageName = packageName, fileName = builderFileName)
        .addExtensionFunction()
        .addBuilderClass()
        .build()
        .writeTo(File(kaptKotlinGeneratedDir))


    private fun FileSpec.Builder.addExtensionFunction() = apply {
        val annotation = element.getAnnotation(StructCreator::class.java)
        if (annotation.companionable) {
            addFunction(
                FunSpec.builder("builder")
                    .receiver(declaringClass.nestedClass(COMPANION))
                    .addCode("return $builderClassName()")
                    .returns(builderClass)
                    .build()
            )
        }

    }

    private fun FileSpec.Builder.addBuilderClass() = apply {
        addType(
            TypeSpec.classBuilder(builderClassName)
                .addChildProperties()
                .build()
        )
    }

    private fun TypeSpec.Builder.addChildProperties() = apply {
        members.forEach {
            addProperty(
                PropertySpec
                    .builder(it.variableName, it.kind.typeName)
                    .apply {
                        if (!it.kind.lateinitable) {
                            initializer(it.kind.initializer())
                        }
                    }
                    .mutable()
                    .addModifiers(*it.kind.modifier)
                    .build()
            )

            addChildFunction(it.kind, it.variableName)
        }

        addBuilderBuildFunction()
    }

    private fun TypeSpec.Builder.addChildFunction(structKind: StructKind, variableName: String) = apply {
        addFunction(
            FunSpec.builder("set${variableName.firstUppercase()}")
                .addParameter(variableName, structKind.typeName)
                .returns(builderClass)
                .addStatement("return apply { this.$variableName = $variableName }")
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuilderBuildFunction() {

        addFunction(
            FunSpec.builder("build")
                .returns(declaringClass)
                .addCode(CodeBlock.Builder()
                    .add("return $className")
                    .add("(\n")
                    .apply {
                        members.forEachIndexed { index, member ->
                            val isNotLast = index != members.size - 1
                            val sb = StringBuilder()
                                .append("   ")
                                .append(member.variableName)
                                .append(" = this.")
                                .append(member.variableName)
                            if (isNotLast) {
                                sb.append(",")
                            }
                            addStatement(sb.toString())
                        }
                    }
                    .add(")")
                    .build())
                .build()
        )
    }

    companion object {
        private const val COMPANION = "Companion"

        fun create(
            element: Element,
            members: List<Member>,
            kaptKotlinGeneratedDir: String,
            annotations: MutableSet<out TypeElement>,
            roundEnv: RoundEnvironment,
            processingEnv: ProcessingEnvironment
        ) = StructCodeConvert(
            element,
            members,
            kaptKotlinGeneratedDir,
            annotations,
            roundEnv,
            processingEnv
        )

        private fun String.firstUppercase(): String {
            if (isEmpty()) return this
            val first = first().uppercaseChar()
            return first + substring(1, length)
        }
    }
}