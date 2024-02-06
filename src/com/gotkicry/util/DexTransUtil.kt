package com.gotkicry.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*


object DexTransUtil{
    fun encodeDexFile(dexFile: File, outDexFile: File) {
        val basePath = "D:\\_Work\\TestDemo\\DexTrans\\test\\output\\"
        for (targetFile in File(basePath).listFiles()!!) {
            if (targetFile.isFile) {
                if (targetFile.exists()) {
                    val transformFile = File(targetFile.absolutePath.replace(".dex", ".plugin.dex"))
                    try {
                        FileInputStream(targetFile).use { inputStream ->
                            FileOutputStream(transformFile).use { outputStream ->
                                val bytesBuff = ByteArray(1024 * 2)
                                val dexStart =
                                    byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00)
                                val badDataByte = ByteArray(0x10 * 0x2C)
                                Random().nextBytes(badDataByte)
                                inputStream.read(dexStart)
                                outputStream.write(badDataByte)
                                var bytesRead: Int
                                while ((inputStream.read(bytesBuff).also { bytesRead = it }) != -1) {
                                    outputStream.write(bytesBuff, 0, bytesRead)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println(e)
                    }
                    println("DONE")
                }
            }
        }
    }
}
