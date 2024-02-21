package com.gotkicry.util


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Arrays

object CommandUtil {
    fun exec(cmd : Array<String>,isShowLog :Boolean = true){
        exec(cmd,envp = null,isShowLog)
    }

    fun exec(cmd : Array<String>,envp: Array<String>?,isShowLog :Boolean = true){
        exec(cmd,envp, dir = null,isShowLog)
    }

    fun exec(cmd: Array<String>, dir: File?,isShowLog :Boolean = true){
        exec(cmd,envp = null,dir,isShowLog)
    }

    /*
     * 请勿修改ProcessBuilder ，设置了本地的jdk环境，缺少JDK环境会导致各种打包问题
     */
    private fun exec(cmd : Array<String>, envp: Array<String>?, dir:File?,isShowLog :Boolean = true) = runBlocking{
        val newCmd = byteTypeToGBK(cmd)
        val cmdBuilder = StringBuilder()
        newCmd?.forEach {
            cmdBuilder.append(it)
            cmdBuilder.append(" ")
        }
        LogUtil.debug("cmd = ${Arrays.toString(newCmd)}")
        val processRunTime = ProcessBuilder(newCmd?.toList())
        val environment = processRunTime.environment()

//        environment["JAVA_HOME"] = JAVA_HOME
//        environment["Path"] = environment["Path"] + ";" + "$JAVA_HOME\\bin\\;"
        if (envp != null) {
            for (i in envp){
                val processEnvp = i.split("=")
                environment[processEnvp[0]] = processEnvp[1]
            }
        }

        val runExec = withContext(Dispatchers.IO) {
//            val newEnv = envp ?: arrayOf("JAVA_HOME=${File("").absolutePath}\\env\\jdk")
            processRunTime.directory(dir)
            processRunTime.start()
        }
        val errorReader = BufferedReader(InputStreamReader(runExec.errorStream,Charset.forName("GBK")),8192 * 1024)
        val inputReader = BufferedReader(InputStreamReader(runExec.inputStream,Charset.forName("GBK")),8192 * 1024)
        withContext(Dispatchers.IO){
            launch { getReaderLine(inputReader,isShowLog) }
            launch { getReaderLine(errorReader,isShowLog) }
        }.join()
    }

    private fun getReaderLine(reader: BufferedReader,isShowLog:Boolean = true){
        do {
            val line = reader.readLine()
            if (line != null){
                if (isShowLog) {
                    LogUtil.debug(line)
                }
            }else{
                reader.close()
                return
            }
        }while (true)
    }

    private fun byteTypeToGBK(strArray :Array<String>): Array<String>? {
        if (strArray.isEmpty())return null
        val newArray = arrayListOf<String>()
        strArray.forEach {
            val string = String(it.toByteArray(Charset.forName("UTF-8")),Charset.forName("UTF-8"))
            newArray.add(string)
        }
        return Array(newArray.size){
            return@Array newArray[it]
        }
    }
}