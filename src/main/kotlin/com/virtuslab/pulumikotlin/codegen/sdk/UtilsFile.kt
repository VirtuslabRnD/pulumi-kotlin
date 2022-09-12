package com.pulumi.kotlin

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import com.pulumi.core.internal.Environment;
import com.pulumi.deployment.InvokeOptions;

object Utilities {

    var version: String

    init {
        val resourceName = "{{{PACKAGE_PATH}}}/version.txt";
        val versionFile = Utilities::class.java.classLoader.getResourceAsStream(resourceName);
        checkNotNull(versionFile) {
            "expected resource '${resourceName}' on Classpath, not found"
        }
        version = BufferedReader(InputStreamReader(versionFile))
            .lines()
            .collect(Collectors.joining("\n"))
            .trim();
    }

    fun getEnv(vararg names: String): String? {
        names.forEach {
            val value = Environment.getEnvironmentVariable(it);
            if (value.isValue) {
                return value.value()
            }
        }
        return null
    }

    fun getEnvBoolean(vararg names: String): Boolean? {
        names.forEach {
            val value = Environment.getBooleanEnvironmentVariable(it);
            if (value.isValue) {
                return value.value()
            }
        }
        return null
    }

    fun getEnvInteger(vararg names: String): Int? {
        names.forEach {
            val value = Environment.getIntegerEnvironmentVariable(it);
            if (value.isValue) {
                return value.value()
            }
        }
        return null
    }

    fun getEnvDouble(vararg names: String): Double? {
        names.forEach {
            val value = Environment.getDoubleEnvironmentVariable(it);
            if (value.isValue) {
                return value.value()
            }
        }
        return null
    }

    // TODO: this probably should be done via a mutator on the InvokeOptions
    fun withVersion(options: InvokeOptions?): InvokeOptions {
        if (options != null && options.version.isPresent) {
            return options;
        }
        return InvokeOptions(
            options?.parent?.orElse(null),
            options?.provider?.orElse(null),
            version
        )
    }

}
