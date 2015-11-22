package ganesa.api

import java.net.{URI, URLEncoder}

class QueryBuilder(val url: String, val encode: String = "UTF-8") {
  var params: List[String] = Nil
  def addParam(key: String, value: String): QueryBuilder = {
    params = "%s=%s".format(key, URLEncoder.encode(value, encode)) :: params
    this
  }
  override def toString() = "%s?%s".format(encodeUrl(url), params.mkString("&"))

  private def encodeUrl(url: String) = {
    val separator = "/"
    val uri = URI.create(url)
    val path = uri.getPath.split(separator).map(URLEncoder.encode(_, encode)).mkString(separator)
    s"${new URI(uri.getScheme, uri.getHost, "", null).toString}$path"
  }
}
