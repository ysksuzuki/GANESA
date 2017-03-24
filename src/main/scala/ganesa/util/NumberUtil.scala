package ganesa.util

import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode

object NumberUtil {
  def inchToCm(inch: Int, scale: Int = 100) = {
    require(inch >= 0)
    val base = "2.54"
    (BigDecimal(inch) / BigDecimal(scale) * BigDecimal(base)).setScale(1, RoundingMode.HALF_UP)
  }

  def poundToGram(pound: Int, scale: Int = 100) = {
    val base = "453.59237"
    (BigDecimal(pound) / BigDecimal(scale) * BigDecimal(base)).setScale(1, RoundingMode.HALF_UP)
  }
}
