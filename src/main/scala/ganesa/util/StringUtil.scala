package ganesa.util

object StringUtil {
  implicit class StringImprovements(val s: String) {
    import scala.util.control.Exception._
    def toIntOpt = catching(classOf[NumberFormatException]) opt s.toInt
    def isNumeric = !s.isEmpty && s.forall(_.isDigit)
  }

  def normalizeKeyword(keyword: String) = {
    val eraseChar = Seq('【', '】', '!', '！', '★', '☆', '[', ']')
    keyword.split(" ").filter(s => eraseChar.forall(c => !s.contains(c)))
      .filterNot(_.trim.isEmpty).take(5).mkString(" ")
  }

}
