package ganesa.controller

import java.io.{IOException}
import java.net.URL
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.layout.BorderPane
import javafx.{scene => jfxs}
import ganesa.MainGanesa

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.stage.{Modality, Stage}
import scalafx.scene.{Scene}
import scalafxml.core.{DependenciesByType, FXMLLoader}
import scalafxml.core.macros.sfxml
import scala.reflect.runtime.{universe=>ru}

import com.typesafe.config.{Config}

@sfxml
class RootLayoutController(
  private val searchViewController: SearchViewControllerInterface
) {

  def handleOpen(): Unit = {
    val dialogStage = new Stage {
      outer =>
      title = "CSV読込"
      val resource: URL = getClass.getClassLoader().getResource("view/CsvFileChooserForRead.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: CsvFileChooserForRead.fxml")
      }
      val loader = new FXMLLoader(resource,
        new DependenciesByType(Map(
          ru.typeOf[SearchViewControllerInterface] -> searchViewController,
          ru.typeOf[() => Unit] -> {() => outer.close()}
        )))
      loader.load()
      val root = loader.getRoot[jfxs.Parent]

      scene = new Scene(root)
      initModality(Modality.WINDOW_MODAL)
      initOwner(MainGanesa.stage)
    }
    dialogStage.showAndWait()
  }

  def handleSave(): Unit = {
    val dialogStage = new Stage {
      outer =>
      title = "CSV出力"
      val resource: URL = getClass.getClassLoader().getResource("view/CsvFileChooserForWrite.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: CsvFileChooserForWrite.fxml")
      }
      val loader = new FXMLLoader(resource,
        new DependenciesByType(Map(
          ru.typeOf[SearchViewControllerInterface] -> searchViewController,
          ru.typeOf[() => Unit] -> {() => outer.close()}
        )))
      loader.load()
      val root = loader.getRoot[jfxs.Parent]

      scene = new Scene(root)
      initModality(Modality.WINDOW_MODAL)
      initOwner(MainGanesa.stage)
    }
    dialogStage.showAndWait()
  }

  def handleConfig(): Unit = {
    val dialogStage = new Stage {
      outer =>
      title = "設定"
      val resource: URL = getClass.getClassLoader().getResource("view/ConfigurationDialogView.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: ConfigurationDialogView.fxml")
      }
      val loader = new FXMLLoader(resource,
        new DependenciesByType(Map(ru.typeOf[() => Unit] -> {() => outer.close()})))
      loader.load()
      val root = loader.getRoot[jfxs.Parent]
      val controller = loader.getController[ConfigurationDialogInterface]
      controller.setConfigLayout(root.delegate.asInstanceOf[BorderPane])

      scene = new Scene(root)
      initModality(Modality.WINDOW_MODAL)
      initOwner(MainGanesa.stage)
    }
    dialogStage.showAndWait()
  }

  def handleLogArchive(): Unit = {
    val dialogStage = new Stage {
      outer =>
      title = "Log Archive"
      val resource: URL = getClass.getClassLoader().getResource("view/LogArchiveView.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: LogArchiveView.fxml")
      }
      val loader = new FXMLLoader(resource,
        new DependenciesByType(Map(
          ru.typeOf[() => Unit] -> {() => outer.close()}
        )))
      loader.load()
      val root = loader.getRoot[jfxs.Parent]

      scene = new Scene(root)
      initModality(Modality.WINDOW_MODAL)
      initOwner(MainGanesa.stage)
    }
    dialogStage.showAndWait()

  }

  def handleAbout(): Unit = {
    val alert = new Alert(AlertType.INFORMATION)
    alert.setTitle("GANESA")
    alert.setHeaderText("About")
    alert.setContentText("Author: Yusuke Suzuki")

    alert.showAndWait()
  }

  def handleExit(): Unit = {
    Platform.exit()
  }

}
