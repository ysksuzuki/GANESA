package ganesa.controller

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import ganesa.MainGanesa
import ganesa.application.LogArchiver

import scalafx.scene.control.TextField
import scalafx.stage.DirectoryChooser
import scalafxml.core.macros.sfxml
import scala.util.control.Exception._

@sfxml
class LogArchiveController(
  private val folderText: TextField,
  private val fileText: TextField,
  private val closeF: () => Unit
) {
  private val logger: Logger = Logger(LoggerFactory.getLogger(LogArchiveController.this.getClass))

  val defaultFileName = {
    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
    s"ganesa_${timeStamp}.zip"
  }

  initialize()
  private def initialize() = {
    fileText.text_=(defaultFileName)
  }

  def folderChoose() = {
    val dirChooser = new DirectoryChooser() {
      title = "出力先を選択してください"
    }
    val dir = dirChooser.showDialog(MainGanesa.stage)
    if (dir != null) {
      folderText.text_=(dir.getAbsolutePath)
    }
  }

  def archive() = {
    allCatch withApply(t => {
      logger.error(t.getMessage, t)
    }) andFinally  {
      closeF()
    } apply {
      LogArchiver.archive(new File(folderText.text.value, fileText.text.value).getAbsolutePath)
    }
  }

  def cancel() = closeF()
}
