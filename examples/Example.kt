fun runGetter() {
    runBlocking {
        getFunction(
            GetFunctionArgs(functionName)
        ).arn!!
    }
}
fun run() {
    runBlocking {
        bucketV2Resource {
            name("a")

            val bucket = getBucket(
                GetBucketArgs("whatever")
            )

            args {
                acl("asd")
                arn("omg")
                bucketPrefix(bucket.region)
                tags(
                    "a" to "b",
                    "c" to "z"
                )
            }

            opts {
                importId("omg")
                retainOnDelete(false)
                aliases(emptyList())
            }
        }
    }
}