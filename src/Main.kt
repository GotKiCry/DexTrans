import com.gotkicry.transforer.ObsTransManager
import com.gotkicry.transforer.SmaliTransUtil
import java.io.File

fun main() {
    ObsTransManager.initMapping("./test/mapping.txt")
//    parseMapping.forEach {
//        it.printClassInfo()
//    }
//    File(".").listFiles()?.forEach {
//        println("fileName : ${it.name}")
//    }
//
    val path = File("./test/smali")
    getDeepFileList(path).forEach {
        transSmaliFile(it)
    }


}

//查询目录下的所有深度文件
fun getDeepFileList(path: File): List<File> {
    val fileList = path.listFiles()?.toList() ?: emptyList()
    val deepFileList = mutableListOf<File>()
    deepFileList.addAll(fileList.filter { it.isDirectory() }.flatMap { getDeepFileList(it) })
    deepFileList.addAll(fileList.filter { it.isFile() })
    return deepFileList
}

fun transSmaliFile(file: File){
    val outputDir = File("./test/output/smali/")
    var content = file.reader().readText()
    run {
        val classInfo = content.lines()[0]
        val smaliFilePath = "L.*?;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
        val obsClass = ObsTransManager.getObsTransClass(smaliFilePath.replace("/",".")) ?: return
        obsClass.fieldsList.forEach{
            content = content.replace(" ${it.fieldName}:", " ${it.transName}:")
        }
    }

    var writeFile :File? = null
    content.lines().forEach { line->
        var transLine = SmaliTransUtil.transSmali(line)
        if (transLine.startsWith(".class")){
            val classInfo = transLine
            val smaliFilePath = "L.*?;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
            val smaliFile = File(outputDir, "$smaliFilePath.smail")
            smaliFile.parentFile.mkdirs()
            smaliFile.createNewFile()
            writeFile = smaliFile
        }else if (transLine.startsWith(".source")){
            transLine = ".source \"${writeFile!!.name.replace(".smail",".java")}\""
        }
        writeFile!!.appendText(transLine)
        writeFile!!.appendText("\n")
    }
}