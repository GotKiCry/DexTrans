package com.gotkicry.transforer

import com.gotkicry.transforer.bean.ObsTransClass
import com.gotkicry.util.LogUtil

object SmaliTransUtil {
    const val TAG = "SmaliTransUtil"


    fun transSmali(smaliFileLine: String,obsTransClass: ObsTransClass?): String {
        var transLine = smaliFileLine.trim().let {
            if (it.startsWith("invoke")) {
                transMethod(smaliFileLine)
            } else if (it.startsWith("sget") || it.startsWith("iget") || it.startsWith("iput") || it.startsWith("sput")) {
                transGetSetField(smaliFileLine)
            } else if (it.startsWith(".field")) {
                transField(smaliFileLine,obsTransClass)
            } else if (it.startsWith(".method")) {
                transMethodName(smaliFileLine,obsTransClass)
            } else {
                smaliFileLine
            }
        }
        "L[^;:]*;".toRegex().findAll(smaliFileLine).forEach {
            val transR :String = transRes(smaliFileLine)
            if (transR.isNotEmpty()){
                return transR
            }

            var replace = it.value
            if (replace.indexOf("(") != -1){
                replace = replace.split("(")[1]
            }

            if (replace.indexOf(")") != -1){
                replace = replace.split(")")[1]
            }
            val className = replace.replaceFirst("L", "").replace(";", "").replace("/", ".")

            var childClassName : String? = null
            val obsTransClass = ObsTransManager.getObsTransClass(className) ?: run {
                childClassName = if (className.indexOfFirst { it == '$' } != -1) {
                    className.substring(className.indexOfFirst { it == '$' })
                } else {
                    ""
                }
                ObsTransManager.getObsTransClass(className.dropLast(className.count() - className.indexOfFirst { it == '$' }))
            }
            if (obsTransClass != null) {
                transLine = transLine.replaceFirst(replace, "L${obsTransClass.transName.replace(".", "/")}${childClassName ?: ""};")
            }

        }
        return transLine
    }

    fun transRes(smaliFileLine: String):String{
        if (smaliFileLine.indexOfFirst { it == 'L' } == -1){
            return ""
        }
        val substring = smaliFileLine.substring(smaliFileLine.indexOfFirst { it == 'L' })
        val rClasses = listOf("R\$id", "R\$string", "R\$layout", "R\$anim", "R\$color", "R\$dimen", "R\$drawable", "R\$id", "R\$integer", "R\$style", "R\$string", "R\$string", "R\$style")
        val startStr = smaliFileLine.dropLast(smaliFileLine.count() - smaliFileLine.indexOfFirst { it == 'L' })
        rClasses.forEach {
            if (it in substring){
                return startStr + transJavaReturnType2Smali(ObsTransManager.packageName+".${it}") + "->${substring.split("->").last()}"
            }
        }
        return ""
    }

    fun transMethodName(methodString: String,obsTransClass: ObsTransClass?):String{
        if (obsTransClass == null){
            return methodString
        }

        val methodSplit = methodString.split(" ")
        val methodInfo = methodSplit.last()
        val methodName = methodInfo.split("(")[0]
        val methodAttrs = methodInfo.split("(")[1].split(")")[0]
        val returnType = methodInfo.split(")")[1]

        return obsTransClass.methodList.filter {
            it.methodName == methodName && it.methodAttr.joinToString(",") == transMethodAttr(methodAttrs) && it.methodType == transReturnValue(returnType)
        }.let {
            if (it.isNotEmpty()){
                val obsTransMethod = it[0]
                methodSplit.dropLast(1).joinToString(" ") + " ${obsTransMethod.transName}(${transJavaAttr2Smali(obsTransMethod.methodAttr)})${transJavaReturnType2Smali(obsTransMethod.methodType)}"
            }else{
                methodString
            }
        }
    }


    fun transField(fieldString: String,obsTransClass: ObsTransClass?):String{
        if (obsTransClass == null){
            return fieldString
        }
        try {
            val fieldSplit = fieldString.split(" ")
            val fieldInfo = fieldSplit.last()
            val fieldName = fieldInfo.split(":")[0]
            val fieldType = fieldInfo.split(":")[1]
            return obsTransClass.fieldsList.filter {
                it.fieldName == fieldName && it.fieldType == transReturnValue(fieldType)
            }.ifEmpty {
                return fieldString
            }.first().let {
                fieldSplit.dropLast(1).joinToString(" ") + " ${it.transName}:${transJavaReturnType2Smali(it.fieldType)}"
            }
        }catch (e : Exception){
            LogUtil.LOGD(TAG, "transField error : $fieldString")
        }

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
//            println("DEBUG -> 空数据")
            //不正常 待确认
            return methodString
        }else if (methodList.count() == 1){
//            println("DEBUG -> 只有一条数据")
            //即使只有一条数据也会因为上个版本参数不同导致参数错误
//            return "${startStr}L${className.replace(".","/")};->${methodList[0].transName}(${transJavaAttr2Smali(methodList[0].methodAttr)})${transJavaReturnType2Smali(methodList[0].methodType)}"

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
        "L[^;]+;|\\[[^)]*|[\\[BIJFDCSZ]".toRegex().findAll(attrs).forEach {
            val resultBuilder = StringBuilder()
            var attrStr = it.value
            val isArray = it.value.startsWith("[")
            if (isArray){
                attrStr = attrStr.substring(1)
            }
            if (attrStr.startsWith("L")){
                resultBuilder.append(attrStr.substring(1).replace("/",".").dropLast(1))
            }else{
                resultBuilder.append(transSmaliType(attrStr))
            }
            if (isArray){
                resultBuilder.append("[]")
            }
            newSplitList.add(resultBuilder.toString())
        }

//        attrs.split("L").let { startSplitList ->
//            for (startSplitAttr in startSplitList){
//                val targetIndex = startSplitList.indexOf(startSplitAttr)
//                val value = if (targetIndex != 0){
//                    "L${startSplitAttr}"
//                }else{
//                    startSplitAttr
//                }
//                value.split(";").let {  endSplitList ->
//
//                    for (endSplitAttr in endSplitList){
//                        if (endSplitAttr.isEmpty())continue
//
//                        val tagIndex = endSplitList.indexOf(endSplitAttr)
//                        if (tagIndex != endSplitList.count() -1){
//                            newSplitList.add(endSplitAttr.substring(1).replace("/","."))
//                        }else{
//                            endSplitAttr.forEach { str -> newSplitList.add(transSmaliType(str.toString()))}
//                        }
//                    }
//
////                    if (it.isNotBlank()){
////                        if (it.startsWith("L")){
////                            newSplitList.add(it.substring(1).replace("/","."))
////                        }else{
////                            it.forEach { str -> newSplitList.add(transSmaliType(str.toString()))}
////                        }
////                    }
//
//                }
//            }
//        }
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
            if (value != "nil") {
                smaliAttrBuilder.append(value)
            }else if(it.endsWith("[]")){
                val arrayAttr = "[${transJavaAttr2Smali(listOf(it.dropLast(2)))}"
                smaliAttrBuilder.append(arrayAttr)
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