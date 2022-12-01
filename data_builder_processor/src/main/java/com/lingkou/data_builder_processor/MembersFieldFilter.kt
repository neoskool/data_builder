package com.lingkou.data_builder_processor

class MembersFieldFilter(
    fieldStr: String,
    private val annotatedClassName: String,
) {
    private val membersFiled = fieldStr.substringAfter(SUB_SCOPE_START, missingDelimiterValue = "")
        .substringBeforeLast(SUB_SCOPE_END, missingDelimiterValue = "")

    fun getMemberField(): List<MemberField>? {
        if (membersFiled.isEmpty()) return null
        val splittedMembers = membersFiled.split(", ")
        val removedMembers = removeSubContent(splittedMembers)
        val constructMembers = splitConstructMember(filterConstructMember(splittedMembers))
        val sortedIndexList = sortedIndex(splittedMembers)
        if (constructMembers.size != removedMembers.size || constructMembers.size != sortedIndexList.size) return null

        return constructMembers.mapIndexed { index, javaType ->
            MemberField(
                javaType = javaType,
                name = removedMembers[sortedIndexList[index] - 1]
            )
        }
    }

    private fun removeSubContent(fields: List<String>): List<String> {
        val contents = mutableListOf<String>()

        fields.forEach {
            if (isValueMember(it, annotatedClassName)) {
                contents.add(it)
            }
        }

        return contents
    }

    private fun splitConstructMember(construct: String?): List<String> {
        if (construct.isNullOrEmpty()) return emptyList()
        val tempSplit = construct.substringAfter(SUB_FUNCTION_START)
            .substringBefore(SUB_FUNCTION_END)
            .split(',')
        val fieldNames = mutableListOf<String>()
        var skippedGenericIndex = -1

        for (index in tempSplit.indices) {
            val name = tempSplit[index]

            if (!name.contains('<') && !name.contains('>')) {
                fieldNames.add(name)
                continue
            }

            if (name.contains('<') && name.last() == '>') {
                fieldNames.add(name)
                continue
            }
            skippedGenericIndex = if (skippedGenericIndex != -1) {
                fieldNames.add("${tempSplit[skippedGenericIndex]},$name")
                -1
            } else {
                index
            }
        }

        return fieldNames
    }

    private fun filterConstructMember(fields: List<String>): String? {
        fields.forEach {
            if (containsConstructMember(it, annotatedClassName)) {
                return it
            }
        }
        return null
    }

    private fun sortedIndex(fields: List<String>): List<Int> {
        val fieldFunctionIndexList = mutableListOf<Int>()

        fields.forEachIndexed { index, field ->
            if (field.isFieldFunction()) {
                fieldFunctionIndexList.add(index)
            }
        }

        if (fieldFunctionIndexList.size == 0) return emptyList()

        return fieldFunctionIndexList.map { index ->
            val originalComponentStr = fields[index]
            originalComponentStr.substringAfter(SUB_COMPONENT).substringBefore(SUB_FUNCTION_START).toInt()
        }
    }

    data class MemberField(
        val name: String,
        val javaType: String
    )

    companion object {
        private const val SUB_SCOPE_START = '['
        private const val SUB_SCOPE_END = ']'
        private const val SUB_GET = "get"
        private const val SUB_COMPONENT = "component"
        private const val SUB_TO_STRING = "toString"
        private const val SUB_HASH_CODE = "hashCode"
        private const val SUB_EQUALS = "equals"
        private const val SUB_COPY = "copy"
        private const val SUB_COMPANION = "companion"
        private const val SUB_FUNCTION_START = '('
        private const val SUB_FUNCTION_END = ')'
        private const val SUB_GENERIC_START = '<'
        private const val SUB_GENERIC_END = '>'


        fun create(fieldStr: String, annotatedClassName: String) = MembersFieldFilter(fieldStr, annotatedClassName)

        fun isJavaArray(content: String): Boolean {
            val lastIndex = content.lastIndex
            return content[lastIndex] == SUB_SCOPE_END
                    && content[maxOf(0, lastIndex - 1)] == SUB_SCOPE_START
        }

        fun isGeneric(content: String): Boolean {
            return content.contains(SUB_GENERIC_START) && content.contains(SUB_GENERIC_END)
        }

        private fun containsConstructMember(original: String, constructName: String): Boolean {
            return original.contains(constructName) && original.isFunction()
        }

        private fun isValueMember(original: String, construct: String): Boolean {
            if (original.contains(SUB_GET) && original.isFunction()) return false

            if (original.contains(SUB_COMPONENT) && original.isFunction()) return false

            if (original.contains(construct) && original.isFunction()) return false

            if (original.contains(SUB_TO_STRING) && original.isFunction()) return false

            if (original.contains(SUB_HASH_CODE) && original.isFunction()) return false

            if (original.contains(SUB_EQUALS) && original.isFunction()) return false

            if (original.contains(SUB_COPY) && original.isFunction()) return false

            if (original.contains(SUB_COMPANION, ignoreCase = true)) return false

            if (original.isFunction()) return false

            return true
        }

        private fun String.isFunction(): Boolean {
            return this.contains(SUB_FUNCTION_START) && this.contains(SUB_FUNCTION_END)
        }

        private fun String.isFieldFunction(): Boolean {
            return this.contains(SUB_COMPONENT) && this.isFunction()
        }
    }
}