package xyz.mf7.kotlinpoet.`fun`

fun <T> T.letIf(what: (T) -> Boolean, mapper: (T) -> T): T {
    return if (what(this)) {
        mapper(this)
    } else {
        this
    }
}

fun <T> T.letIf(what: Boolean, mapper: (T) -> T): T {
    return if (what) {
        mapper(this)
    } else {
        this
    }
}