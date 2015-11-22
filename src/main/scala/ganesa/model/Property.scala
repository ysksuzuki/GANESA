package ganesa.model

trait StringPropertyBase {
  protected def getQuote(value: String): String = {
    if (escapePattern(value)) "\"\"\""
    else "\""
  }
  private def escapePattern(value: String) = value.matches("""^.*[\\'"${}].*$""")
}
case class StringPropertyModel(
  key: String,
  value: String,
  indent: String = "    ",
  separator: String = " = "
) extends StringPropertyBase {
  override def toString() = {
    val quote = getQuote(value)
    s"$indent$key$separator$quote$value$quote"
  }
}

case class StringListPropertyModel(
  key: String,
  value: Seq[String],
  indent: String = "    ",
  separator: String = " = "
) extends StringPropertyBase {
  override def toString() = {
    val valueList = value.map(w => {
      val quote = getQuote(w)
      s"$quote$w$quote"
    }).mkString(", ")
    s"$indent$key$separator[$valueList]"
  }
}

case class IntPropertyModel(
  key: String,
  value: Int,
  indent: String = "    ",
  separator: String = " = "
) {
  override def toString() = s"""${indent}${key}${separator}${value}"""
}
