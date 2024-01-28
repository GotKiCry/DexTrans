package com.gotkicry.transforer.bean

data class ObsTransClass(
    val className: String,
    val transName: String,
    val fieldsList: MutableList<ObsTransFields>,
    val methodList: MutableList<ObsTransMethod>
){
    fun printClassInfo(){
        println("$className -> $transName")
        fieldsList.forEach{
            it.printMethodInfo()
        }
        methodList.forEach {
            it.printMethodInfo()
        }
    }
}
