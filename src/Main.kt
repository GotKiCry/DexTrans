import com.gotkicry.transforer.ObsTransManager
import com.gotkicry.transforer.SmaliTransUtil
import java.io.File
import java.util.concurrent.ThreadPoolExecutor

fun main(array: Array<String>) {
    ObsTransManager.initMapping("./test/mapping.txt")
//    parseMapping.forEach {
//        it.printClassInfo()
//    }
//    File(".").listFiles()?.forEach {
//        println("fileName : ${it.name}")
//    }
//

    ObsTransManager.packageName = "com.jh.tyly.mi"
    val dirs = listOf(
//        File("./test/xiaomi"),
//        File("./test/xiaomi-4142"),
//        File("./test/xiaomi-4142-api-def"),
//        File("./test/x3-4085"),
//        File("./test/x3-4142-1"),
//        File("./test/x4-4085"),
        File("./test/x3-4030"),
//        File("./test/x3"),
        File("./test/x4"),
//        File("./test/oppotest"),

        )

    //kotlin创建一个多线程线程池
    val threadPool = ThreadPoolExecutor(
        10,
        50,
        0L,
        java.util.concurrent.TimeUnit.MILLISECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    )
    for (dir in dirs) {
        threadPool.execute {
            getDeepFileList(dir).forEach {
                threadPool.execute {
                    transSmaliFile(it, dir.name)
                }
            }
        }
    }
    while (threadPool.activeCount > 0){
        println("activeCount : ${threadPool.activeCount}")
        Thread.sleep(500)
    }
    threadPool.shutdown()
}

//查询目录下的所有深度文件
fun getDeepFileList(path: File): List<File> {
    val fileList = path.listFiles()?.toList() ?: emptyList()
    val deepFileList = mutableListOf<File>()
    deepFileList.addAll(fileList.filter { it.isDirectory() }.flatMap { getDeepFileList(it) })
    deepFileList.addAll(fileList.filter { it.isFile() })
    return deepFileList
}

fun transSmaliFile(file: File, moduleName: String) {
    val outputDir = File("./test/output/${moduleName}/smali/")
    var content = file.reader().readText()

    val classInfo = content.lines()[0]
    val smaliFilePath = "L[^;:]*;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
    val obsClass = ObsTransManager.getObsTransClass(smaliFilePath.replace("/", ".")) ?: run {
        var packageName = smaliFilePath.replace("/", ".")
        packageName = packageName.dropLast(packageName.count() - packageName.indexOfFirst { it == '$' })
        ObsTransManager.getObsTransClass(packageName)
    }
    var writeFile: File? = null
    content.lines().forEach { line ->
        var transLine = SmaliTransUtil.transSmali(line, obsClass)
        if (transLine.startsWith(".class")) {
            val classInfo = transLine
            val smaliFilePath = "L[^;:]*;".toRegex().find(classInfo)!!.value.substring(1).dropLast(1)
            val smaliFile = File(outputDir, "$smaliFilePath.smali")
            smaliFile.parentFile.mkdirs()
            smaliFile.createNewFile()
            println("${Thread.currentThread().name}->${file.absolutePath} : ${smaliFile.absolutePath}")
            writeFile = smaliFile
        } else if (transLine.startsWith(".source")) {
            transLine = ".source \"${writeFile!!.name.replace(".smali", ".java")}\""
        }
        writeFile!!.appendText(transLine)
        writeFile!!.appendText("\n")
    }
}