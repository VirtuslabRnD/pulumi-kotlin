package com.pulumi.kotlin

class PulumiNullFieldException(name: String) :
    RuntimeException(
        "Field $name is required but was not set (or was set to null)",
    )
