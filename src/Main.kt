import com.gotkicry.transforer.ObsTransManager
import com.gotkicry.transforer.SmaliTransUtil
import com.gotkicry.util.CommandUtil
import com.gotkicry.util.DexTransUtil
import com.gotkicry.util.LogUtil
import java.io.File
import java.util.concurrent.ThreadPoolExecutor


var targetApk = File("")
const val outputDir = "./workTmp/output"
//模块名称
var channelName = ""
fun main(array: Array<String>) {
    try {
        if(array.isEmpty()){
            LogUtil.printAndLine("请输入指令")
            return
        }
        for(i in 0..< array.count()){
            when(array[i]){
                "-mapping" ->{
                    if(array[i + 1].isEmpty()){
                        throw Exception("mapping file is empty")
                    }
                    LogUtil.printAndLine("mapping file is ${array[i + 1]}")
                    ObsTransManager.initMapping(array[i + 1])
                }
                "-apk" ->{
                    if(array[i + 1].isEmpty()){
                        throw Exception("apk file is empty")
                    }
                    LogUtil.printAndLine("apk file is ${array[i + 1]}")
                    targetApk = File(array[i + 1])
                }
                "-channel" ->{
                    LogUtil.printAndLine("channelName is ${array[i + 1]}")
                    channelName = array[i + 1]
                }
            }
        }
    }catch (e : Exception){
        LogUtil.printAndLine("指令错误，请检查")
        e.printStackTrace()
    }


    if (File(outputDir).deleteRecursively()){
        LogUtil.debug("delete outputDir success")
    }else{
        throw Exception("delete outputDir fail")
    }

    ObsTransManager.packageName = "com.test.only"
    File("./workTmp/module/$channelName").listFiles()?.let {
        startTrans(
            it.toList()
        )
    } ?: run {
        println("找不到指定模块文件 $channelName")
    }
}

fun startTrans(moduleList:List<File>){
    //kotlin创建一个多线程线程池
    val threadPool = ThreadPoolExecutor(
        10,
        50,
        0L,
        java.util.concurrent.TimeUnit.MILLISECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    )
    for (dir in moduleList) {
        threadPool.execute {
            getDeepFileList(dir).forEach {
                threadPool.execute {
                    transSmaliFile(it, dir.name)
                }
            }
        }
    }
    while (threadPool.activeCount > 0){
//        println("activeCount : ${threadPool.activeCount}")
        Thread.sleep(500)
    }
    threadPool.shutdown()
    makeModule2Dex()
}



fun makeModule2Dex(){
    LogUtil.printAndLine("生成DEX")
    File(outputDir).listFiles()?.let { modulesDir ->
        for (moduleDir in modulesDir){
            val dexFile = File("${moduleDir.absolutePath}.dex")
            CommandUtil.exec(arrayOf("java","-jar","./env/lib/smali-2.5.2.jar","as",moduleDir.absolutePath,"--output",dexFile.absolutePath), isShowLog = false)
            DexTransUtil.encodeDexFile(dexFile, File(dexFile.absolutePath.replace(".dex", ".plugin.dex")))
            dexFile.deleteRecursively()
        }


    }
    File(outputDir).listFiles()?.let { modulesDir ->
        val dexFiles = modulesDir.filter { it.name.endsWith(".dex") && it.isFile }.associateWith { "/assets/bin/" }
        DexTransUtil.addFilesToZip(targetApk,dexFiles)
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

fun transSmaliFile(file: File, moduleName: String) {
    val outputDir = File("$outputDir/${moduleName}/smali/")
    val content = file.reader().readText()

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
            val clazzInfo = transLine
            val smaliPath = "L[^;:]*;".toRegex().find(clazzInfo)!!.value.substring(1).dropLast(1)
            val smaliFile = File(outputDir, "$smaliPath.smali")
            smaliFile.parentFile.mkdirs()
            smaliFile.createNewFile()
//            println("${Thread.currentThread().name}->${file.absolutePath} : ${smaliFile.absolutePath}")
            writeFile = smaliFile
        } else if (transLine.startsWith(".source")) {
            transLine = ".source \"${writeFile!!.name.replace(".smali", ".java")}\""
        }
        writeFile!!.appendText(transLine)
        writeFile!!.appendText("\n")
    }
}