package ganesa

import java.io.{File, IOException}
import javafx.scene.layout.BorderPane
import javafx.{scene => jfxs}

import ganesa.controller.{SearchViewControllerInterface}
import ganesa.util.PlatformUtil
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.{Parent, Scene}
import scalafx.scene.image.Image
import scalafxml.core.{NoDependencyResolver, FXMLLoader, DependenciesByType, FXMLView}
import scala.reflect.runtime.{universe=>ru}

object MainGanesa extends JFXApp {

  private val logger: Logger = Logger(LoggerFactory.getLogger(MainGanesa.getClass))

  launch()
  private def launch() = {
    import scala.util.control.Exception._
    allCatch withApply {t => {
      logger.error(t.getMessage, t)
      throw t
    }} apply {
      initializeHome()
      showSearchView()
    }
  }

  private def initializeHome() = {
    new File(PlatformUtil.logDir).mkdirs()
    new File(PlatformUtil.confDir).mkdirs()
  }

  private def initRootLayout(searchViewController: SearchViewControllerInterface): Parent = {
    val resource = getClass.getClassLoader().getResource("view/RootLayout.fxml")
    if (resource == null) {
      throw new IOException("Cannot load resource: RootLayout.fxml")
    }
    val root = FXMLView(resource, new DependenciesByType(
      Map(ru.typeOf[SearchViewControllerInterface] -> searchViewController)))
    stage = new JFXApp.PrimaryStage() {
      title = "GANESA"
      scene = new Scene(root)
      icons.add(new Image("file:resources/images/icon.png"))
    }
    root
  }

  private def showSearchView(): Unit = {
    val resource = getClass.getClassLoader().getResource("view/SearchView.fxml")
    if (resource == null) {
      throw new IOException("Cannot load resource: SearchView.fxml")
    }

    val loader = new FXMLLoader(resource, NoDependencyResolver)
    loader.load()
    val searchView = loader.getRoot[jfxs.Parent]
    val controller = loader.getController[SearchViewControllerInterface]
    val root = initRootLayout(controller)
    root.delegate.asInstanceOf[BorderPane].center = searchView
  }
}
