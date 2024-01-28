package com.gotkicry.transforer

import com.gotkicry.util.LogUtil

object SmaliTransUtil {
    const val TAG = "SmaliTransUtil"


    fun transSmali(smaliFileLine :String):String{
        var transLine = smaliFileLine.trim().let {
            if (it.startsWith("invoke")){
                transMethod(smaliFileLine)
            }else if (it.startsWith("sget") || it.startsWith("iget") || it.startsWith("iput") || it.startsWith("sput")){
                transGetSetField(smaliFileLine)
            }else if (it.startsWith(".field")) {
                transField(smaliFileLine)
            }else{
                smaliFileLine
            }
        }
        "L.*?;".toRegex().findAll(smaliFileLine).forEach {
            val replace = it.value.replaceFirst("L", "").replace(";", "").replace("/", ".")
            val obsTransClass = ObsTransManager.getObsTransClass(replace)
            if (obsTransClass != null){
                transLine = transLine.replace(it.value, "L${obsTransClass.transName.replace(".","/")};")
            }

        }
        return transLine
    }


    fun transField(fieldString: String):String{
        //暫時不操作
        return  fieldString
    }

    fun transGetSetField(fieldString: String):String{
        val startStr = fieldString.dropLast(fieldString.count() - fieldString.indexOfFirst { it == 'L' })
        val substring = fieldString.substring(fieldString.indexOfFirst { it == 'L' })
        val methodInfo = substring.split("->")
        val className = methodInfo[0].replaceFirst("L","").dropLast(1).replace("/",".")
        val obsClass = ObsTransManager.getObsTransClass(className) ?: run {
            return fieldString
        }

        val fieldName = methodInfo[1].split(":")[0]
        val returnType = transReturnValue(methodInfo[1].split(":")[1])
        val obsField = obsClass.fieldsList.filter {
            it.fieldName == fieldName && it.fieldType == returnType
        }.ifEmpty {
            return fieldString
        }.first()

        return "${startStr}L${className.replace(".","/")};->${obsField.transName}:${transJavaReturnType2Smali(obsField.fieldType)}"
    }

    /**
     * example: Ljava/lang/Object;->toString()Ljava/lang/String;
     */
    fun transMethod(methodString: String):String{
//        println("get method -> $methodString")
        val startStr = methodString.dropLast(methodString.count() - methodString.indexOfFirst { it == 'L' })
        val substring = methodString.substring(methodString.indexOfFirst { it == 'L' })
        LogUtil.LOGD(TAG,substring)

        val methodInfo = substring.split("->")
        val className = methodInfo[0].replaceFirst("L","").dropLast(1).replace("/",".")
        LogUtil.LOGD(TAG,"className : $className")
        val obsClass = ObsTransManager.getObsTransClass(className)

        val methodName = methodInfo[1].split("(")[0]
        LogUtil.LOGD(TAG,"methodName : $methodName")
        //如果只有一个方法，可以直接替换
        val methodList = obsClass?.let{ obsClazz ->
            obsClazz.methodList.filter { it.methodName == methodName }
        }

        if (methodList.isNullOrEmpty()){
            println("DEBUG -> 空数据")
            //不正常 待确认
            return methodString
        }else if (methodList.count() == 1){
            println("DEBUG -> 只有一条数据")
            return "${startStr}L${className.replace(".","/")};->${methodList[0].transName}(${transJavaAttr2Smali(methodList[0].methodAttr)})${transJavaReturnType2Smali(methodList[0].methodType)}"

        }

        val returnValue : String

        val methodAttr = methodInfo[1].split("(")[1].let { content ->
            val indexCount = content.indexOfLast { it == ')' }
            returnValue = transReturnValue(content.substring(indexCount + 1))
            transMethodAttr(content.dropLast(content.count() - indexCount))
        }
        LogUtil.LOGD(TAG,"methodAttr : $methodAttr")
        LogUtil.LOGD(TAG,"returnValue : $returnValue")

        val matchList = methodList.filter { it.methodAttr.joinToString(",") == methodAttr&& it.methodType == returnValue }
        return if (matchList.isNotEmpty()){
            "${startStr}L${className.replace(".","/")};->${matchList[0].transName}(${transJavaAttr2Smali(matchList[0].methodAttr)})${transJavaReturnType2Smali(returnValue)}"
        }else{
            methodString
        }
    }


    private fun transMethodAttr(attrs: String): String {
        val newSplitList = mutableListOf<String>()
        attrs.split("L").let { startSplitList ->
            for (startSplitAttr in startSplitList){
                val targetIndex = startSplitList.indexOf(startSplitAttr)
                val value = if (targetIndex != 0){
                    "L${startSplitAttr}"
                }else{
                    startSplitAttr
                }
                value.split(";").let {  endSplitList ->

                    for (endSplitAttr in endSplitList){
                        if (endSplitAttr.isEmpty())continue

                        val tagIndex = endSplitList.indexOf(endSplitAttr)
                        if (tagIndex != endSplitList.count() -1){
                            newSplitList.add(endSplitAttr.substring(1).replace("/","."))
                        }else{
                            endSplitAttr.forEach { str -> newSplitList.add(transSmaliType(str.toString()))}
                        }
                    }

//                    if (it.isNotBlank()){
//                        if (it.startsWith("L")){
//                            newSplitList.add(it.substring(1).replace("/","."))
//                        }else{
//                            it.forEach { str -> newSplitList.add(transSmaliType(str.toString()))}
//                        }
//                    }

                }
            }
        }
//        for (splitStr in attrs.split("L")) {
//            val str = if (splitStr.endsWith(";")){
//                 "L$splitStr"
//            }else{
//                splitStr
//            }
//
//
//        }
        return newSplitList.joinToString(",")
    }

    private fun transReturnValue(returnValue :String):String{
        return  if (returnValue.startsWith("L")){
            returnValue.substring(1).dropLast(1).replace("/",".")
        }else{
            transSmaliType(returnValue)
        }
    }

    private fun transJavaReturnType2Smali(javaType: String):String{
        return transJavaAttr2Smali(listOf(javaType))
    }


    private fun transJavaAttr2Smali(javaAttr: List<String>):String{
        val smaliAttrBuilder = StringBuilder()
        javaAttr.forEach {
            val value = transJavaType(it)
            if (value != "nil"){
                smaliAttrBuilder.append(value)
            }else if (it.isNotBlank()){
                smaliAttrBuilder.append("L${it.replace(".","/")};")
            }
        }
        return smaliAttrBuilder.toString()
    }

    /**
     * 将Smali所有类型转换为Java基本类型
     */

    fun transSmaliType(smaliType: String): String {
        return when (smaliType) {
            "Z" -> "boolean"
            "B" -> "byte"
            "S" -> "short"
            "C" -> "char"
            "I" -> "int"
            "J" -> "long"
            "F" -> "float"
            "D" -> "double"
            "V" -> "void"
            else -> "nil"
        }
    }

    /**
     * 将Java基本类型转换为Smali类型
     */
    fun transJavaType(javaType: String): String {
        return when (javaType) {
            "boolean" -> "Z"
            "byte" -> "B"
            "short" -> "S"
            "char" -> "C"
            "int" -> "I"
            "long" -> "J"
            "float" -> "F"
            "double" -> "D"
            "void" -> "V"
            else -> "nil"
        }
    }

}