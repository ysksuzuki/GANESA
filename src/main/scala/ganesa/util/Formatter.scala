package ganesa.util

import java.text.ParseException

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

object Formatter {
  private val logger: Logger = Logger(LoggerFactory.getLogger(Formatter.getClass))
  def formatNumber(price: Int): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    formatter.format(price)
  }

  def parsePrice(price: String): Int = {
    if (price.isEmpty) 0
    else {
      import scala.util.control.Exception._
      val formatter = java.text.NumberFormat.getIntegerInstance
      val catchingEx = catching(classOf[ParseException]) withApply {t => {
        logger.error(t.getMessage, t)
        0
      }}
      catchingEx {formatter.parse(price).intValue()}
    }
  }

  def formatText(text: String): String = {
    if (text.isEmpty) "-"
    else text
  }
}
