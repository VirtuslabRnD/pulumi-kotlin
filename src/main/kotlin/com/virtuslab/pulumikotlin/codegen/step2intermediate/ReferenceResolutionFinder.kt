package com.virtuslab.pulumikotlin.codegen.step2intermediate

import com.virtuslab.pulumikotlin.codegen.step1schemaparse.ParsedSchema
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ArrayProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.MapProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.OneOfProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PrimitiveProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Property
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferencingOtherTypesProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RootTypeProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringEnumProperty
import com.virtuslab.pulumikotlin.codegen.step1schemaparse.referencedTypeName
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import com.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource

class ReferenceResolutionFinder(schema: ParsedSchema) {

    private val rootTypesByName = schema.types.lowercaseKeys()
    private val usages = findAllUsages(schema)

    fun resolve(typeName: String): RootTypeProperty? {
        return rootTypesByName[typeName]
    }

    fun getUsages(typeName: String): List<Usage> {
        return usages[typeName] ?: emptyList()
    }

    private fun findAllUsages(schema: ParsedSchema): Map<String, List<Usage>> {
        val cases = concat(
            findNestedUsages(schema.resources, Usage(Nested, Resource, Output)) {
                it.properties.values
            },
            findNestedUsages(schema.resources, Usage(Nested, Resource, Input)) {
                it.inputProperties.values
            },
            findNestedUsages(schema.functions, Usage(Nested, Function, Output)) {
                it.outputs.properties.values
            },
            findNestedUsages(schema.functions, Usage(Nested, Function, Input)) {
                it.inputs?.properties?.values.orEmpty()
            },
        )

        return cases
            .groupBy(
                keySelector = { it.typeName },
                valueTransform = { it.usage },
            )
            .lowercaseKeys()
    }

    private fun <V> findNestedUsages(
        resourcesOrFunctions: Map<String, V>,
        usage: Usage,
        mapper: (V) -> Iterable<Property>,
    ): List<UsageForName> {
        return resourcesOrFunctions.values
            .flatMap { mapper(it) }
            .flatMap { property -> findTypesUsedByProperty(property) }
            .map { UsageForName(it, usage) }
    }

    private fun findTypesUsedByProperty(
        property: Property?,
    ): List<String> {
        return when (property) {
            is ReferenceProperty -> {
                val typeName = property.referencedTypeName
                val referencedProperty = resolve(typeName)
                val nestedUsages = findTypesUsedByProperty(referencedProperty)
                if (nestedUsages.isEmpty()) {
                    println("Could not compute nested usages for $typeName")
                }
                nestedUsages + typeName
            }

            is ReferencingOtherTypesProperty -> {
                getInnerTypesOf(property).flatMap { findTypesUsedByProperty(it) }
            }

            is PrimitiveProperty -> emptyList()
            null -> emptyList()
        }
    }

    private fun getInnerTypesOf(property: ReferencingOtherTypesProperty): Iterable<Property> {
        return when (property) {
            is ArrayProperty -> listOf(property.items)
            is MapProperty -> listOf(property.additionalProperties)
            is ObjectProperty -> property.properties.values
            is OneOfProperty -> property.oneOf
            is StringEnumProperty -> emptyList()
        }
    }

    private fun <T> concat(iterableOfIterables: Iterable<Iterable<T>>): List<T> =
        iterableOfIterables.flatten()

    private fun <T> concat(vararg iterables: Iterable<T>) =
        concat(iterables.asIterable())

    private data class UsageForName(val typeName: String, val usage: Usage)
}
