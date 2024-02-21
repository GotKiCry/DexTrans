package com.gotkicry.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object DexTransUtil{
    fun encodeDexFile(targetFile: File, outDexFile: File) {
        if (targetFile.isFile) {
            if (targetFile.exists()) {
                try {
                    FileInputStream(targetFile).use { inputStream ->
                        FileOutputStream(outDexFile).use { outputStream ->
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
            }
        }
    }

    //添加文件到APK(Zip)
    fun addFilesToZip(zipFile: File, files: Map<File, String>) {
        val tmpFile = File(zipFile.absolutePath + ".tmp")
        zipFile.renameTo(tmpFile)

        ZipInputStream(FileInputStream(tmpFile)).use { zis ->
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 把已存在的条目复制到新的ZIP流
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!files.values.any { it == (entry?.name ?: "") }) {
                        val newEntry = ZipEntry(entry.name)
//                        LogUtil.printAndLine("entry name : ${newEntry.name}")
                        newEntry.method = entry.method
                        if(newEntry.method == 0) {
                            newEntry.time = entry.time
                            newEntry.size = entry.size
                            newEntry.crc = entry.crc
                        }
                        zos.putNextEntry(newEntry)
                        zis.copyTo(zos, 1024)
                        zos.closeEntry()
                    }
                    entry = zis.nextEntry
                }

                // 添加新文件到ZIP
                for ((file, path) in files) {
                    FileInputStream(file).use { fis ->
                        // 在这里指定目录和文件名，将会将文件保存至指定的目录结构下
                        val zipEntry = ZipEntry(path + file.name)
                        zos.putNextEntry(zipEntry)
                        fis.copyTo(zos, 1024)
                        zos.closeEntry()
                    }
                }
            }
        }

        // 完成后删除临时文件
        tmpFile.delete()
    }
}
