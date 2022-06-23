package xyz.mf7.kotlinpoet.`fun`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.kotlinpoet.*

class Whatever {

}




data class Context(val valueMapKeys: List<String>)

fun generate(parentKey: String?, map: Any?, context: Context): List<TypeSpec> {
    val value = when(map) {
        is Map<*, *> -> {
            if(context.valueMapKeys.contains(parentKey)) {
                map.map { (key, value) ->
                    val newKey = key as String
                    key to generate(key, value, context)
                }
            } else {

            }
        }

        null -> {
            null
        }

        is Int -> {

        }

        is Long -> {

        }

        is List<*> -> {

        }

        is String -> {

        }

        is Boolean -> {

        }
        else -> {
            println("WARNING: unknown type $map")
        }
    }

    if(context.valueMapKeys.contains(parentKey)) {

    }
}

fun main() {

    val resource = Whatever::class.java.getResourceAsStream("/schema.json")!!

    val json = ObjectMapper()

    val valuesForTheseKeysShouldBeAMap = listOf("types", "resources")

    val schema = json.readValue<Map<String, Any>>(resource)

    val rootClass = ClassName("", "Root")

    val testClass = TypeSpec.classBuilder("Test")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("prop", String::class).build()
                )
                .build()
        )
        .addProperty(
            PropertySpec.builder("prop", String::class)
                .initializer("prop")
                .build()
        )
        .build()

    val otherType = TypeSpec.classBuilder("Test2")
        .addModifiers(KModifier.DATA)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("prop", ClassName("", testClass.name!!)).build()
                )
                .build()
        )
        .addProperty(
            PropertySpec.builder("prop", String::class)
                .initializer("prop")
                .build()
        )
        .build()

    val file = FileSpec.builder("", "file")
        .addType(testClass)
        .addType(otherType)
        .build()

    file.writeTo(System.out)
}

/*"aws-native:rds:getDBClusterParameterGroup": {
    "description": "The AWS::RDS::DBClusterParameterGroup resource creates a new Amazon RDS DB cluster parameter group. For more information, see Managing an Amazon Aurora DB Cluster in the Amazon Aurora User Guide.",
    "inputs": {
        "properties": {
        "dBClusterParameterGroupName": {
        "type": "string"
    }
    },
        "required": [
        "dBClusterParameterGroupName"
        ]
    },
    "outputs": {
        "properties": {
        "dBClusterParameterGroupName": {
        "type": "string"
    },
        "tags": {
        "type": "array",
        "items": {
        "$ref": "#/types/aws-native:rds:DBClusterParameterGroupTag"
    },
        "description": "The list of tags for the cluster parameter group."
    }
    }
    }
},*/