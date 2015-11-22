package ganesa.controller

import javafx.beans.value.{ObservableValue, ChangeListener}
import javafx.{concurrent => jfxc}
import scalafx.application.Platform
import scalafx.concurrent.Service
import scalafx.scene.control.{Label, ProgressBar}
import scalafxml.core.macros.sfxml

@sfxml
class ProgressViewController(
  private val progressLabel: Label,
  private val progressBar: ProgressBar,
  private val cancelLabel: Label,
  private val task: jfxc.Task[Boolean],
  private val closeF: () => Unit,
  private val cancelF: () => Boolean
) {
  progressLabel.text.bind(Worker.message)
  progressLabel.text.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      if (newValue.startsWith("Complete")) {
        close()
      }
    }
  })
  progressBar.progressProperty().bind(task.progressProperty())
  Worker.restart()
  object Worker extends Service(new jfxc.Service[Boolean]() {
    protected def createTask(): jfxc.Task[Boolean] = task
  })

  def cancel(): Boolean = {
    cancelLabel.text_=("Canceling...")
    cancelF()
  }

  def close(): Unit = closeF()
}
