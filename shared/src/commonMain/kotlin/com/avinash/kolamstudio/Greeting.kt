package com.avinash.kolamstudio

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
