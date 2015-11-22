package ganesa.controller

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import ganesa.MainGanesa
import ganesa.api.Const

import scalafx.scene.control.{ComboBox, TextField}
import scalafx.stage.DirectoryChooser
import scalafxml.core.macros.sfxml

@sfxml
class CsvFileChooserForWriterController(
  private val folderText: TextField,
  private val fileText: TextField,
  private val encodeCombo: ComboBox[String],
  private val searchViewController: SearchViewControllerInterface,
  private val closeF: () => Unit
) {

  val defaultFileName = {
    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
    s"${timeStamp}.csv"
  }

  initialize()
  private def initialize() = {
    fileText.text_=(defaultFileName)
    Const.encodes.foreach(encodeCombo += _)
    encodeCombo.getSelectionModel.select(0)
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

  def write() = {
    val file = new File(folderText.text.value, fileText.text.value)
    val encode = encodeCombo.getSelectionModel.getSelectedItem
    searchViewController.csvWrite(file, encode)
    closeF()
  }

  def cancel() = closeF()

}
