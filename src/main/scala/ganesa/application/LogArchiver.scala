package ganesa.application

import ganesa.util.{ZipUtil, PlatformUtil}

object LogArchiver {

  val defaultPassword = "ganesa"

  def archive(output: String) = {
    new ZipUtil().zip(PlatformUtil.logDir, output, defaultPassword)
  }
}
