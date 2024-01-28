package com.gotkicry.transforer.bean

data class ObsTransMethod(
    val methodName: String,
    val transName: String,
    val methodType: String,
    val methodAttr: List<String>
) {
    fun printMethodInfo(){
        println("   $methodType $methodName(${methodAttr.joinToString(",")}) -> $transName")
    }
}