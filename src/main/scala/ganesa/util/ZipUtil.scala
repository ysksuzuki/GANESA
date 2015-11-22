package ganesa.util

import java.io.File

import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants

class ZipUtil(
  val compressionMethod: Int = Zip4jConstants.COMP_DEFLATE,
  val compressionLevel: Int = Zip4jConstants.DEFLATE_LEVEL_NORMAL,
  val encryptionMethod: Int = Zip4jConstants.ENC_METHOD_STANDARD,
  val aesKeyStrength: Int = Zip4jConstants.AES_STRENGTH_256
) {

  def zip(input: String, output: String, password: String, fileNameCharset: String = "") = {
    val zipFile = new ZipFile(output)
    // If the fileNameCharset is empty then charset is detected automatically
    // Try with Cp850 and UTF8 or OS default
    if (!fileNameCharset.isEmpty()) {
      zipFile.setFileNameCharset(fileNameCharset)
    }

    val parameters = new ZipParameters()
    parameters.setCompressionMethod(compressionMethod)
    parameters.setCompressionLevel(compressionLevel)
    parameters.setEncryptFiles(true)
    parameters.setEncryptionMethod(encryptionMethod)
    parameters.setAesKeyStrength(aesKeyStrength)
    parameters.setPassword(password)

    val inputFile = new File(input)
    if (inputFile.isDirectory()) {
      zipFile.createZipFileFromFolder(inputFile, parameters, false, 0)
    } else {
      zipFile.addFile(inputFile, parameters)
    }
  }

  def unzip(input: String, output: String, password: String) = {
    val zipFile = new ZipFile(input)
    if (zipFile.isEncrypted()) {
      zipFile.setPassword(password)
    }
    zipFile.extractAll(output)
  }
}
