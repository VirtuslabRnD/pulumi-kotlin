package myproject;

import com.pulumi.kotlin.Pulumi
import com.pulumi.core.Output

fun main() {
    Pulumi.run { ctx ->
        ctx.export("exampleOutput", Output.of("example"))
    }
}
