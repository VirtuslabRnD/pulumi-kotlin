package org.virtuslab.pulumikotlin.codegen.step2intermediate

import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ArrayProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.MapProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ObjectProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.OneOfProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.PrimitiveProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Property
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferenceProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.ReferencingOtherTypesProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.RootTypeProperty
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.Schema
import org.virtuslab.pulumikotlin.codegen.step1schemaparse.SchemaModel.StringEnumProperty
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Depth.Nested
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Input
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Direction.Output
import org.virtuslab.pulumikotlin.codegen.step2intermediate.MapWithKeyTransformer.ConflictStrategy.Companion.mergeSetsOnConflicts
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Function
import org.virtuslab.pulumikotlin.codegen.step2intermediate.Subject.Resource
import org.virtuslab.pulumikotlin.codegen.utils.DEFAULT_PROVIDER_TOKEN
import org.virtuslab.pulumikotlin.codegen.utils.valuesToSet

class ReferenceFinder(schema: Schema) {

    private val rootTypesByName = schema.types.lowercaseKeys()
    private val usages = findAllUsages(schema)

    fun resolve(typeName: String): RootTypeProperty? {
        return rootTypesByName[typeName]
    }

    fun getUsages(typeName: String): Set<UsageKind> {
        return usages[typeName].orEmpty()
    }

    private fun findAllUsages(schema: Schema): Map<String, Set<UsageKind>> {
        val cases = concat(
            findNestedUsages(schema.resources, UsageKind(Nested, Resource, Output)) {
                it.properties.values
            },
            findNestedUsages(schema.resources, UsageKind(Nested, Resource, Input)) {
                it.inputProperties.values
            },
            findNestedUsages(mapOf(DEFAULT_PROVIDER_TOKEN to schema.provider), UsageKind(Nested, Resource, Input)) {
                it?.inputProperties?.values.orEmpty()
            },
            findNestedUsages(schema.functions, UsageKind(Nested, Function, Output)) {
                it.outputs.properties.values
            },
            findNestedUsages(schema.functions, UsageKind(Nested, Function, Input)) {
                it.inputs?.properties?.values.orEmpty()
            },
        )

        return cases
            .groupingBy { it.typeName }
            .valuesToSet { it.usageKind }
            .lowercaseKeys(conflictStrategy = mergeSetsOnConflicts())
    }

    private fun <V> findNestedUsages(
        resourcesOrFunctions: Map<String, V>,
        usageKind: UsageKind,
        mapper: (V) -> Iterable<Property>,
    ): List<TypeNameAndUsageKind> {
        return resourcesOrFunctions.values
            .flatMap { mapper(it) }
            .flatMap { property -> findReferencedTypeNamesUsedByProperty(property, emptySet()) }
            .map { TypeNameAndUsageKind(it, usageKind) }
    }

    private fun findReferencedTypeNamesUsedByProperty(property: Property?, visited: Set<String>): List<String> {
        return when (property) {
            is ReferenceProperty -> {
                if (property.isArchive() || property.isAssetOrArchive() || property.isJson()) {
                    emptyList()
                } else {
                    val typeName = property.referencedTypeName
                    val referencedProperty = resolve(typeName)

                    val nestedUsages = if (visited.contains(typeName)) {
                        emptyList()
                    } else {
                        findReferencedTypeNamesUsedByProperty(referencedProperty, visited + typeName)
                    }

                    nestedUsages + typeName
                }
            }

            is ReferencingOtherTypesProperty -> {
                getInnerTypesOf(property).flatMap { findReferencedTypeNamesUsedByProperty(it, visited) }
            }

            null, is PrimitiveProperty, is StringEnumProperty -> emptyList()
        }
    }

    private fun getInnerTypesOf(property: ReferencingOtherTypesProperty): Iterable<Property> {
        return when (property) {
            is ArrayProperty -> listOf(property.items)
            is MapProperty -> listOf(property.additionalProperties)
            is ObjectProperty -> property.properties.values
            is OneOfProperty -> property.oneOf
        }
    }

    private fun <T> concat(iterableOfIterables: Iterable<Iterable<T>>): List<T> =
        iterableOfIterables.flatten()

    private fun <T> concat(vararg iterables: Iterable<T>) =
        concat(iterables.asIterable())

    private data class TypeNameAndUsageKind(val typeName: String, val usageKind: UsageKind)
}
