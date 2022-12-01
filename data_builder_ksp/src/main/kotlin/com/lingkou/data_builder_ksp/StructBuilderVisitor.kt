package com.lingkou.data_builder_ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class StructBuilderVisitor(
    environment: SymbolProcessorEnvironment,
    private val resolver: Resolver,
) : KSVisitorVoid() {

    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger


    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.primaryConstructor!!.accept(this, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val parent = function.parentDeclaration as KSClassDeclaration
        val packageName = parent.containingFile!!.packageName.asString()
        val className = parent.toClassName()

        val fileSpec = FileSpec.builder(packageName = packageName, fileName = "${className.simpleName}Extension")
            .addExtensionFunction(parent, function, className)
            .addBuilderClass(className, packageName, function, resolver)
            .build()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, function.containingFile!!),
            packageName = packageName,
            fileName = "${className.simpleName}Extension",
        )

        OutputStreamWriter(file, StandardCharsets.UTF_8)
            .use(fileSpec::writeTo)
    }


    private fun FileSpec.Builder.addExtensionFunction(
        parent: KSClassDeclaration,
        function: KSFunctionDeclaration,
        className: ClassName
    ) = apply {
        if (parent.isCompanionObject) {
            addFunction(
                FunSpec.builder("builder")
                    .receiver(parent.toClassName().nestedClass("Companion"))
                    .addCode("return ${className.simpleName}Builder()")
                    .returns(ClassName(className.packageName, "${className.simpleName}Builder"))
                    .build()
            )
        }
    }


    private fun FileSpec.Builder.addBuilderClass(
        className: ClassName,
        packageName: String,
        function: KSFunctionDeclaration,
        resolver: Resolver
    ) = apply {
        val builderClassName = "${className.simpleName}Builder"
        val parent = function.parentDeclaration as KSClassDeclaration
        val typeVariableNames = parent.typeParameters.map { it.toTypeVariableName() }

        val builderClass = ClassName(packageName, builderClassName)

        addType(
            TypeSpec.classBuilder(builderClass)
                .addTypeVariables(typeVariableNames)
                .addParametersFunction(function, resolver)
                .addBuildFunction(className, function)
                .build()
        )
    }

    private fun TypeSpec.Builder.addParametersFunction(
        function: KSFunctionDeclaration,
        resolver: Resolver
    ) = apply {
        function.parameters.forEach {
            val parameterName = it.name!!.asString()
            val typeName = try {
                it.type.toTypeName()
            } catch (e: NoSuchElementException) {
                TypeVariableName(
                    name = it.type.resolve().declaration.simpleName.asString(),
                    bounds = it.type.resolve().declaration.typeParameters.map { t -> t.toTypeVariableName() }
                ).copy(nullable = it.nullable())
            }
            addProperty(
                PropertySpec.builder(parameterName, typeName)
                    .mutable()
                    .addModifiers(*it.asPropertyModifiers(resolver))
                    .asInitializer(it, resolver)
                    .build()
            )

            addFunction(
                FunSpec.builder("with${parameterName.firstUppercase()}")
                    .addParameter(parameterName, typeName)
                    .addCode("return apply { this.$parameterName = $parameterName }")
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addBuildFunction(
        className: ClassName,
        function: KSFunctionDeclaration
    ) = apply {
        addFunction(
            FunSpec.builder("builder")
                .addCode(
                    CodeBlock.Builder()
                        .addDepthDefaultConstructor(className, function, resolver)
                        .build()
                )
                .build()
        )
    }

    private fun PropertySpec.Builder.asInitializer(
        parameter: KSValueParameter,
        resolver: Resolver
    ) = apply {
        if (!parameter.isVariable(resolver)) { // lateinit var 不需要初始化
            return@apply
        }

        if (parameter.type.resolve().isPrimitiveForm(resolver)) {
            initializer(parameter.type.resolve().defaultPrimitiveStringValue(resolver))
        } else {
            initializer("null")
        }
    }

    private fun CodeBlock.Builder.addDepthDefaultConstructor(
        className: ClassName,
        function: KSFunctionDeclaration,
        resolver: Resolver,
    ) = apply {
        val parameters = function.parameters
        val hasDefaultParameter = parameters.filter { it.hasDefault }

        if (hasDefaultParameter.isEmpty()) {
            add("return ")
            addNewConstruct(className, parameters)
            return@apply
        }

        beginControlFlow("return when")
        addForEachDefaultValueFlow(className, parameters, hasDefaultParameter, resolver)

        addStructControlFlow(
            controlFlowName = "else ->",
            className = className,
            parameters = parameters.toMutableList().also { it.removeAll(hasDefaultParameter) }
        )
        endControlFlow()
    }

    /**
     * list(test1, test2, test3, test4, test5, test6), list(test2, test3, test4, test5) 为空
     * size = 3..1, slice = 0..2
     * 1. - size = 4, slice = 0 => test2, test3, test4, test5
     *
     * 2. - size = 3, slice = 0 => test2, test3, test4
     *    - size = 3, slice = 2 => test2, test3, test5
     *    - size = 3, slice = 2 => test2, test4, test5
     *    - size = 3, slice = 3 => test3, test4, test5
     *
     * 3. - size = 2, slice = 0 => test2, test3
     *    - size = 2, slice = 1 => test2, test4
     *    - size = 2, slice = 2 => test2, test5
     *    - size = 2, slice = 3 => test3, test4
     *    - size = 2, slice = 4 => test3, test5
     *    - size = 2, slice = 5 => test4, test5
     *
     * 4. - size = 1, slice = 0 => test2
     *    - size = 1, slice = 1 => test3
     *    - size = 1, slice = 2 => test4
     *    - size = 1, slice = 0 => test5
     *
     * C(4,4) + C(4, 3) + C(4, 2) + C(4, 1)
     */
    private fun CodeBlock.Builder.addForEachDefaultValueFlow(
        className: ClassName,
        parameters: List<KSValueParameter>,
        hasDefaultParameter: List<KSValueParameter>,
        resolver: Resolver,
    ) = apply {
        if (hasDefaultParameter.isEmpty()) return@apply

        for (shouldWatched in hasDefaultParameter.size downTo 1) {
            val permutations = DFSUtils.combination(hasDefaultParameter, shouldWatched)
            permutations.forEach { permutation ->
                val removedParameter = parameters.toMutableList()
                    .also { it.removeAll(hasDefaultParameter) }
                    .let { it + permutation }
                addStructControlFlow(
                    defaultParameter2Expression(permutation, resolver),
                    className,
                    removedParameter
                )
            }
        }
    }

    private fun defaultParameter2Expression(parameters: List<KSValueParameter>, resolver: Resolver): String {
        val sb = StringBuilder()
        parameters.forEachIndexed { index, parameter ->
            val name = parameter.name!!.asString()
            val parameterResolver = parameter.type.resolve()
            when {
                parameter.isVariable(resolver) && parameterResolver.isPrimitiveForm(resolver) -> { //是变量并且是原始类型
                    sb.append("$name != ${parameterResolver.defaultPrimitiveStringValue(resolver)}")
                }
                parameter.isVariable(resolver) -> { //只是变量不是原始类型
                    sb.append("$name != null")
                }
                else -> {
                    sb.append("this::$name.isInitialized")
                }
            }
            if (index != parameters.lastIndex) {
                sb.append(" && ")
            } else {
                sb.append(" ->")
            }
        }
        return sb.toString()
    }

    private fun CodeBlock.Builder.addStructControlFlow(controlFlowName: String, className: ClassName, parameters: List<KSValueParameter>) = apply {
        beginControlFlow(controlFlowName)
        addNewConstruct(className, parameters)
        endControlFlow()
    }

    private fun CodeBlock.Builder.addNewConstruct(className: ClassName, parameters: List<KSValueParameter>) = apply {
        add("${className.simpleName}(\n")
        addParametersLineText(parameters)
        add(")\n")
    }

    private fun CodeBlock.Builder.addParametersLineText(parameters: List<KSValueParameter>) = apply {
        parameters.forEachIndexed { index, ksValueParameter ->
            val isNotLast = index != parameters.size - 1
            val parameterName = ksValueParameter.name!!.asString()
            var sb = "  $parameterName = this.$parameterName"
            if (isNotLast) {
                sb = "$sb,"
            }
            addStatement(sb)
        }
    }
}