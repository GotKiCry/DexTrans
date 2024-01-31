import com.gotkicry.transforer.ObsTransManager
import com.gotkicry.transforer.SmaliTransUtil
import com.gotkicry.transforer.bean.ObsTransClass
import java.io.File

fun main(array: Array<String>) {
    ObsTransManager.initMapping("./test/mapping.txt")
//    parseMapping.forEach {
//        it.printClassInfo()
//    }
//    File(".").listFiles()?.forEach {
//        println("fileName : ${it.name}")
//    }
//
    ObsTransManager.packageName = "com.test.demo"
    val dirs = listOf(
        File("./test/plugin"),
    )
    for (dir in dirs){
        getDeepFileList(dir).forEach {
            transSmaliFile(it)
        }
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

    val classInfo = content.lines()[0]
    val smaliFilePath = "L.*?;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
    val obsClass = ObsTransManager.getObsTransClass(smaliFilePath.replace("/",".")) ?: run{
        var packageName = smaliFilePath.replace("/", ".")
        packageName = packageName.dropLast(packageName.count() - packageName.indexOfFirst { it == '$' })
        ObsTransManager.getObsTransClass(packageName)
    }
    var writeFile :File? = null
    content.lines().forEach { line->
        var transLine = SmaliTransUtil.transSmali(line,obsClass)
        if (transLine.startsWith(".class")){
            val classInfo = transLine
            val smaliFilePath = "L.*?;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
            val smaliFile = File(outputDir, "$smaliFilePath.smali")
            smaliFile.parentFile.mkdirs()
            smaliFile.createNewFile()
            println("${file.absolutePath} : ${smaliFile.absolutePath}")
            writeFile = smaliFile
        }else if (transLine.startsWith(".source")){
            transLine = ".source \"${writeFile!!.name.replace(".smali",".java")}\""
        }
        writeFile!!.appendText(transLine)
        writeFile!!.appendText("\n")
    }
}