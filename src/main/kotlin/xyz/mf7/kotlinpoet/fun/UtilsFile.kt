package xyz.mf7.kotlinpoet.`fun`

import com.squareup.kotlinpoet.FileSpec

fun generateUtilsFile(packagePath: String, packageName: String): String {
    return """
        package ${packageName};
        
        import java.io.BufferedReader;
        import java.io.InputStreamReader;
        import java.util.Optional;
        import java.util.stream.Collectors;
        import javax.annotation.Nullable;
        import com.pulumi.core.internal.Environment;
        import com.pulumi.deployment.InvokeOptions;

        public class Utilities {

        	public static Optional<String> getEnv(String... names) {
                for (var n : names) {
                    var value = Environment.getEnvironmentVariable(n);
                    if (value.isValue()) {
                        return Optional.of(value.value());
                    }
                }
                return Optional.empty();
            }

        	public static Optional<Boolean> getEnvBoolean(String... names) {
                for (var n : names) {
                    var value = Environment.getBooleanEnvironmentVariable(n);
                    if (value.isValue()) {
                        return Optional.of(value.value());
                    }
                }
                return Optional.empty();
        	}

        	public static Optional<Integer> getEnvInteger(String... names) {
                for (var n : names) {
                    var value = Environment.getIntegerEnvironmentVariable(n);
                    if (value.isValue()) {
                        return Optional.of(value.value());
                    }
                }
                return Optional.empty();
        	}

        	public static Optional<Double> getEnvDouble(String... names) {
                for (var n : names) {
                    var value = Environment.getDoubleEnvironmentVariable(n);
                    if (value.isValue()) {
                        return Optional.of(value.value());
                    }
                }
                return Optional.empty();
        	}

        	// TODO: this probably should be done via a mutator on the InvokeOptions
        	public static InvokeOptions withVersion(@Nullable InvokeOptions options) {
                    if (options != null && options.getVersion().isPresent()) {
                        return options;
                    }
                    return new InvokeOptions(
                        options == null ? null : options.getParent().orElse(null),
                        options == null ? null : options.getProvider().orElse(null),
                        getVersion()
                    );
                }

            private static final String version;
            public static String getVersion() {
                return version;
            }

            static {
                var resourceName = "${packagePath}/version.txt";
                var versionFile = Utilities.class.getClassLoader().getResourceAsStream(resourceName);
                if (versionFile == null) {
                    throw new IllegalStateException(
                            String.format("expected resource '%s' on Classpath, not found", resourceName)
                    );
                }
                version = new BufferedReader(new InputStreamReader(versionFile))
                        .lines()
                        .collect(Collectors.joining("\n"))
                        .trim();
            }
        }

    """.trimIndent()
}