package ganesa.controller

import java.io.File

import ganesa.MainGanesa
import ganesa.api.Const

import scalafx.scene.control.{ComboBox, TextField}
import scalafx.stage.{FileChooser, DirectoryChooser}
import scalafxml.core.macros.sfxml

@sfxml
class CsvFileChooserForReadController(
  private val fileText: TextField,
  private val encodeCombo: ComboBox[String],
  private val searchViewController: SearchViewControllerInterface,
  private val closeF: () => Unit
) {

  initialize()
  private def initialize() = {
    Const.encodes.foreach(encodeCombo += _)
    encodeCombo.getSelectionModel.select(0)
  }

  def fileChoose() = {
    val fileChooser = new FileChooser() {
      title = "CSVファイルを選択してください"
    }
    val file = fileChooser.showOpenDialog(MainGanesa.stage)
    if (file != null) {
      fileText.text_=(file.getAbsolutePath)
    }
  }

  def read() = {
    val file = new File(fileText.text.value)
    val encode = encodeCombo.getSelectionModel.getSelectedItem
    searchViewController.csvRead(file, encode)
    closeF()
  }

  def cancel() = closeF()

}
