package ganesa.util

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import ganesa.model._

import scala.io.Source
import scala.util.Properties

object ConfigUtil {
  val configFile = "ganesa.conf"
  def load(): Config = {
    getConfigFile().map(file =>
      ConfigFactory.parseFile(file).resolve()).getOrElse(ConfigFactory.load().resolve())
  }

  def save(authentication: Authentication, operationSettings: OperationSettings) = {
    val header =
      """include classpath("application.conf")
        |
        |""".stripMargin
    val comment =
      """# If you want change configurations, remove comment out and update a value.
        |# Please leave them commented out when you don't need change.
        |# Default value will be applied.
        |
        |""".stripMargin
    val contents = Source.fromURL(getClass.getClassLoader.getResource("application.conf"))
      .getLines().map {
      case line@Property(key) => {
        key match {
          case "awsAccessKeyId" => stringProperty("awsAccessKeyId", authentication.awsAccessKeyId, line)
          case "awsSecretKey" => stringProperty("awsSecretKey", authentication.awsSecretKey, line)
          case "associateTag" => stringProperty("associateTag", authentication.associateTag, line)
          case "buddhaFace" => intProperty("buddhaFace", operationSettings.buddhaFace, line)
          case "interval" => intProperty("interval", operationSettings.interval, line)
          case "connectTimeout" => intProperty("connectTimeout", operationSettings.connectTimeout, line)
          case "readTimeout" => intProperty("readTimeout", operationSettings.readTimeout, line)
          case "excludeWords" => stringListProperty("excludeWords", operationSettings.excludeWords, line)
          case _ => "#" + line
        }
      }
      case line => line
    } mkString Properties.lineSeparator
    IOUtil.writeText(new File(PlatformUtil.confDir, configFile).getAbsolutePath, header + comment + contents)
  }

  private def stringProperty(key: String, value: String, line: String): String = {
    if (!value.isEmpty) StringPropertyModel(key, value).toString()
    else "#" + line
  }

  private def stringListProperty(key: String, value: Seq[String], line: String): String = {
    StringListPropertyModel(key, value).toString()
  }

  private def intProperty(key: String, value: Int, line: String): String = {
    if (value > 0) IntPropertyModel(key, value).toString()
    else "#" + line
  }

  private def getConfigFile(): Option[File] = {
    val homeConfig = new File(PlatformUtil.confDir, configFile)
    if (homeConfig.exists()) Some(homeConfig)
    else {
      val currentConfig = new File(configFile)
      if (currentConfig.exists()) Some(currentConfig)
      else None
    }
  }
}

object Property {
  def unapply(line: String) = {
    val list = line.replaceAll("\\s", "").split("=").toList
    if (list.length >= 2) list.headOption
    else None
  }
}
