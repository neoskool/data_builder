package com.lingkou.data_builder_ksp

import java.util.LinkedList

object DFSUtils {
    fun <T> combination(values: List<T>, size: Int): List<List<T>> {
        if (0 == size) {
            return listOf(emptyList())
        }
        if (values.isEmpty()) {
            return emptyList()
        }
        val combination: MutableList<List<T>> = LinkedList()
        val actual = values.iterator().next()
        val subSet: MutableList<T> = LinkedList(values)
        subSet.remove(actual)
        val subSetCombination = combination(subSet, size - 1)
        for (set in subSetCombination) {
            val newSet = LinkedList(set)
            newSet.add(0, actual)
            combination.add(newSet)
        }
        combination.addAll(combination(subSet, size))
        return combination
    }
}