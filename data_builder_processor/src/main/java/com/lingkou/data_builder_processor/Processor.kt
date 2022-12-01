@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.lingkou.data_builder_processor

import com.google.auto.service.AutoService
import com.lingkou.data_builder_annotations.FieldAlias
import com.lingkou.data_builder_annotations.GenericNullable
import com.lingkou.data_builder_annotations.Mutable
import com.lingkou.data_builder_annotations.StructCreator
import com.lingkou.data_builder_annotations.UField
import com.sun.tools.javac.code.Symbol
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.lingkou.data_builder_annotations.StructCreator"])
@AutoService(Processor::class)
class Processor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            StructCreator::class.java.canonicalName,
            FieldAlias::class.java.canonicalName,
            Mutable::class.java.canonicalName,
            UField::class.java.canonicalName,
            GenericNullable::class.java.canonicalName,
            NotNull::class.java.canonicalName,
            Nullable::class.java.canonicalName,
        )
    }

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: return false

        // 被注解注释的类，因为 StructCreator 只能对类进行注解，所以只需要获取第一个
        val annotatedElement = roundEnv.getElementsAnnotatedWith(StructCreator::class.java).firstOrNull() ?: return false
        // 没有 field member 跳过
        val annotatedClassSymbol = annotatedElement as? Symbol.ClassSymbol
        val membersField = annotatedClassSymbol?.members() ?: return false
        val membersStr = membersField.toString()

        val membersFieldList = MembersFieldFilter
            .create(membersStr, annotatedElement.simpleName.toString())
            .getMemberField()

        val mutableFieldList = roundEnv.getElementsAnnotatedWith(Mutable::class.java)
        val uFieldList = roundEnv.getElementsAnnotatedWith(UField::class.java)
        val genericNullable = roundEnv.getElementsAnnotatedWith(GenericNullable::class.java)
        val nullableFieldList = roundEnv.getElementsAnnotatedWith(Nullable::class.java)
        val fieldAliasList = roundEnv.getElementsAnnotatedWith(FieldAlias::class.java)
        val members = membersFieldList.asMember(
            mutableFieldList,
            uFieldList,
            genericNullable,
            nullableFieldList,
            fieldAliasList
        )

        StructCodeConvert
            .create(
                element = annotatedElement,
                members = members,
                kaptKotlinGeneratedDir = kaptKotlinGeneratedDir,
                annotations = annotations,
                roundEnv = roundEnv,
                processingEnv = processingEnv
            )
            .buildFile()
        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}