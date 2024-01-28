package com.gotkicry.transforer.bean

data class ObsTransFields(
    val fieldName: String,
    val transName: String,
    val fieldType: String,
) {
    fun printMethodInfo(){
        println("   $fieldType $fieldName -> $transName")
    }
}