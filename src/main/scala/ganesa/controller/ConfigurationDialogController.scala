package ganesa.controller

import javafx.beans.value.{ObservableValue, ChangeListener}
import javafx.collections.FXCollections
import javafx.scene.layout.BorderPane
import javafx.{scene => jfxs}

import ganesa.model.{Configuration, Authentication, OperationSettings}
import ganesa.util.ConfigUtil
import ganesa.util.StringUtil._

import scala.util.Properties
import scalafx.scene.control.{TextArea, PasswordField, TextField, ListView}
import scalafxml.core.macros.sfxml
import scalafxml.core.{DependenciesByType, FXMLLoader}
import scala.reflect.runtime.{universe=>ru}
import scala.collection.JavaConversions._

import com.typesafe.config.{Config}

trait ConfigurationDialogInterface {
  def setConfigLayout(layout: BorderPane)
  def saveConfiguration(): Unit
  def cancel(): Unit
}

@sfxml
class ConfigurationDialogController(
  private val configurationListView: ListView[String],
  private val closeF: () => Unit
) extends ConfigurationDialogInterface {

  private val config = ConfigUtil.load()
  private var configLayout: BorderPane = null
  private val authentication: (jfxs.Parent, ConfigurationInterface) = loadFxml("view/AuthenticationView.fxml")
  private val settings: (jfxs.Parent, ConfigurationInterface) = loadFxml("view/OperationSettingsView.fxml")

  val AUTHENTICATION = "認証情報"
  val OPERATION_CONFIG = "動作設定"

  private val views: Map[String, (jfxs.Parent, ConfigurationInterface)] = Map(
    OPERATION_CONFIG -> settings,
    AUTHENTICATION -> authentication
  )

  initialize()
  private def initialize() = {
    configurationListView.getSelectionModel.selectedItemProperty().addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = changeView(newValue)
    })
    loadListItems()
  }

  private def loadListItems() = {
    configurationListView.setItems(FXCollections.observableArrayList(AUTHENTICATION, OPERATION_CONFIG))
  }

  private def loadFxml(fxmlPath: String) = {
    val loader = new FXMLLoader(getClass.getClassLoader().getResource(fxmlPath), new DependenciesByType(Map(ru.typeOf[Config]->config)))
    loader.load()
    (loader.getRoot[jfxs.Parent], loader.getController[ConfigurationInterface]())
  }

  private def changeView(value: String) = {
    val (parent: jfxs.Parent, controller: ConfigurationInterface) = views.getOrElse(value, settings)
    configLayout.setRight(parent)
  }

  override def setConfigLayout(layout: BorderPane) = {
    configLayout = layout
    configurationListView.getSelectionModel.select(0)
  }

  override def saveConfiguration(): Unit = {
    val (authParent: jfxs.Parent, authController: ConfigurationInterface) = authentication
    val (settingsParent: jfxs.Parent, settingsController: ConfigurationInterface) = settings
    ConfigUtil.save(
      authController.configuration.asInstanceOf[Authentication],
      settingsController.configuration.asInstanceOf[OperationSettings]
    )
    closeF()
  }

  override def cancel(): Unit = closeF()

}

trait ConfigurationInterface {
  val config: Config
  def configuration(): Configuration
}

@sfxml
class OperationSettingsController(
  private val buddhaFace: TextField,
  private val interval: TextField,
  private val connectTimeout: TextField,
  private val readTimeout: TextField,
  private val excludeWords: TextArea,
  override val config: Config
) extends ConfigurationInterface {

  initialize()

  private def initialize() = {
    buddhaFace.text_=(config.getString("settings.general.buddhaFace"))
    interval.text_=(config.getString("settings.general.interval"))
    connectTimeout.text_=(config.getString("settings.general.connectTimeout"))
    readTimeout.text_=(config.getString("settings.general.readTimeout"))
    excludeWords.text_=(config.getStringList("settings.general.excludeWords").mkString(Properties.lineSeparator))
  }

  override def configuration(): Configuration = {
    new OperationSettings(
      buddhaFace.text.value.toIntOpt.getOrElse(0),
      interval.text.value.toIntOpt.getOrElse(0),
      connectTimeout.text.value.toIntOpt.getOrElse(0),
      readTimeout.text.value.toIntOpt.getOrElse(0),
      excludeWords.text.value.split(Properties.lineSeparator).toSeq
    )
  }
}

@sfxml
class AuthenticationController(
  private val awsAccessKeyId: TextField,
  private val awsSecretKey: PasswordField,
  private val associateTag: TextField,
  override val config: Config
) extends ConfigurationInterface {

  initialize()

  private def initialize() = {
    awsAccessKeyId.text_=(config.getString("authentication.amazon.awsAccessKeyId"))
    awsSecretKey.text_=(config.getString("authentication.amazon.awsSecretKey"))
    associateTag.text_=(config.getString("authentication.amazon.associateTag"))
  }

  override def configuration(): Configuration = {
    new Authentication(awsAccessKeyId.text.value, awsSecretKey.text.value, associateTag.text.value)
  }
}
