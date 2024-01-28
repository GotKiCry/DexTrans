package com.gotkicry.transforer.obs

import com.gotkicry.transforer.bean.ObsTransClass
import com.gotkicry.transforer.bean.ObsTransFields
import com.gotkicry.transforer.bean.ObsTransMethod
import java.io.File

class ObsFactor {

    fun parseMapping(mappingFilePath: String): Map<String,ObsTransClass> {
        val obsTransClassMap = mutableMapOf<String,ObsTransClass>()
        val mappingFile = File(mappingFilePath)
        var lastClassName = ""
        mappingFile.inputStream().reader().forEachLine { line ->
            /**
             * 如果是#开头的，则忽略
             * 如果是空格开头的，则是方法或变量
             * 其余为类
             * 使用When
             */
            when {
                line.startsWith("#") -> {
//                    println("ignore $line")
                }

                line.startsWith(" ") -> {
                    val obsTransClass = obsTransClassMap[lastClassName]!!.copy()
                    val lineList = line.trim().split(":")
                    val methodInfo = if (lineList.size == 1) {
                        lineList[0].split(" ")
                    } else {
                        lineList[2].split(" ")
                    }
                    val methodType = methodInfo[0]
                    val methodNameAndAttrs = methodInfo[1].split("(")
                    val methodName = methodNameAndAttrs[0]
                    val transMethodName = line.split(" -> ")[1]
                    try {
                        val methodAttr = methodNameAndAttrs[1].dropLast(1)
                        val methodAttrList = methodAttr.split(",")
                        obsTransClass.methodList.add(
                            ObsTransMethod(
                                methodName,
                                transMethodName,
                                methodType,
                                methodAttrList
                            )
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        obsTransClass.fieldsList.add(
                            ObsTransFields(
                                methodName,
                                transMethodName,
                                methodType
                            )
                        )
                    }
                    obsTransClassMap[lastClassName] = obsTransClass
                }

                else -> {
                    val className = line.split(" -> ")
                    lastClassName = className[0]
                    obsTransClassMap[lastClassName] = ObsTransClass(
                        className[0],
                        className[1].dropLast(1),
                        mutableListOf(),
                        mutableListOf()

                    )
                }
            }

        }
        return obsTransClassMap
    }
}