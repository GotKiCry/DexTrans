package com.gotkicry.util

object LogUtil {

    fun debug(tag: String, msg: String){
        println("$tag -> $msg")
    }

    fun debug( msg: String){
        printAndLine(msg)
    }

    fun printAndLine(msg: String){
        println("${getMethodName()} : $msg")
    }
    fun print(msg: String){
        println(msg)
    }

    private fun getMethodName(): String {
        val stackTrace = Thread.currentThread().stackTrace
        for (elementIndex in 1..stackTrace.count()) {
            val element = stackTrace[elementIndex]
            if (element.className != "com.gotkicry.util.LogUtil") {
                return "[${element.className}(${element.methodName + ":" +element.lineNumber})]"
            }
        }
        return "unknown"
    }
}