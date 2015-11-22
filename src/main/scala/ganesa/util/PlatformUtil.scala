package ganesa.util

import java.io.File

import scala.util.Properties

object PlatformUtil {
  val os = Properties.osName.toLowerCase()
  def isLinux = os.startsWith("linux")
  def isWindows = os.startsWith("windows")
  val appHome = new File(Properties.userHome, ".ganesa").getAbsolutePath
  val logDir = new File(appHome, "log").getAbsolutePath
  val confDir = new File(appHome, "conf").getAbsolutePath
}
