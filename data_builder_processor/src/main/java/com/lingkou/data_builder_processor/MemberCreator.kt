package com.lingkou.data_builder_processor

import com.lingkou.data_builder_annotations.FieldAlias
import com.lingkou.data_builder_annotations.GenericNullable
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror

fun List<MembersFieldFilter.MemberField>?.asMember(
    mutableElements: Set<Element>,
    uFieldElements: Set<Element>,
    genericNullableElements: Set<Element>,
    nullableElements: Set<Element>,
    fieldAliasElements: Set<Element>
): List<Member> {
    if (this.isNullOrEmpty()) return emptyList()
    val members = mutableListOf<Member>()

    forEach { memberField ->
        val nullable =
            nullableElements.any { element -> element.simpleName.toString() == memberField.name }
        val aliasElement =
            fieldAliasElements.firstOrNull { element -> element.simpleName.toString() == memberField.name }
        val isMutable =
            mutableElements.any { element -> element.simpleName.toString() == memberField.name }
        val isUField =
            uFieldElements.any { element -> element.simpleName.toString() == memberField.name }
        val genericNullableElement =
            genericNullableElements.firstOrNull { element -> element.simpleName.toString() == memberField.name }
        val genericNullableArray = genericNullableElement
            ?.getAnnotation(GenericNullable::class.java)
            ?.nullables

        if (aliasElement != null) {
            addAliasCustomMember(nullable, aliasElement, memberField, members)
        } else {
            addStructKindMember(nullable, isMutable, isUField, genericNullableArray, memberField, members)
        }
    }
    return members
}

private fun addStructKindMember(
    nullable: Boolean,
    isMutable: Boolean,
    isUField: Boolean,
    genericNullableArray: BooleanArray?,
    memberField: MembersFieldFilter.MemberField,
    members: MutableList<Member>
) {
    members.add(
        Member(
            variableName = memberField.name,
            kind = memberField.javaType.asStructKind(
                nullable,
                isMutable,
                isUField,
                genericNullableArray?.toMutableList()
            )
        )
    )
}

private fun addAliasCustomMember(
    nullable: Boolean,
    aliasElement: Element,
    memberField: MembersFieldFilter.MemberField,
    members: MutableList<Member>
) {
    val fieldAlias = aliasElement.getAnnotation(FieldAlias::class.java)
    val aliasTypeName = getTypeMirror(fieldAlias)?.asTypeName()

    aliasTypeName?.let {
        members.add(
            Member(
                variableName = memberField.name,
                kind = AliasKind(it, nullable, fieldAlias.defaultValue)
            )
        )
    }
}

private fun getTypeMirror(annotation: FieldAlias): TypeMirror? {
    try {
        annotation.type
    } catch (e: MirroredTypeException) {
        return e.typeMirror
    }
    return null
}