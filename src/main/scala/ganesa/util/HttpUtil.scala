package ganesa.util

import java.io.{IOException, StringReader}
import java.net.{ConnectException}

import ganesa.api.QueryBuilder
import org.apache.http.client.config.RequestConfig
import org.apache.http.{HttpResponse, HttpEntity}
import org.apache.http.client.{ClientProtocolException, ResponseHandler}
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.{HttpClientBuilder, CloseableHttpClient}
import org.apache.http.util.EntityUtils

import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

import nu.validator.htmlparser.sax.HtmlParser
import nu.validator.htmlparser.common.XmlViolationPolicy
import org.xml.sax.InputSource

object HttpUtil {
  val BUDDHA_FACE = 5
  val INTERVAL = 3000
  val DEFAULT_CHARSET = "UTF-8"
  val CONNECT_TIMEOUT = 5000
  val READ_TIMEOUT = 5000
  private val logger: Logger = Logger(LoggerFactory.getLogger(HttpUtil.getClass))
  def callApi(url: String, buddhaFace: Int = BUDDHA_FACE, interval: Int = INTERVAL,
              connectTimeout: Int = CONNECT_TIMEOUT, readTimeout: Int = READ_TIMEOUT,
              encode: String = DEFAULT_CHARSET): Option[String] = {
    try {
      RetryUtil.retry(buddhaFace, interval, classOf[ConnectException], classOf[IOException]) {
        httpGet(url, connectTimeout, readTimeout, encode)
      }
    } catch {
      case e: RetryException =>
        e.throwables.foreach(t => logger.error(t.getMessage, t))
        None
    }
  }

  def callApiIterator(url: String, buddhaFace: Int = BUDDHA_FACE, interval: Int = INTERVAL,
                      connectTimeout: Int = CONNECT_TIMEOUT, readTimeout: Int = READ_TIMEOUT,
                      encode: String = DEFAULT_CHARSET): Iterator[String] = {
    try {
      RetryUtil.retry(buddhaFace, interval, classOf[ConnectException], classOf[IOException]) {
        httpGet(url, connectTimeout, readTimeout, encode).map(response => {
          val LINE_SEPARATOR_PATTERN = "\r\n|[\n\r\u2028\u2029\u0085]"
          response.split(LINE_SEPARATOR_PATTERN).iterator
        }).getOrElse(Iterator())
      }
    } catch {
      case e: RetryException =>
        e.throwables.foreach(t => logger.error(t.getMessage, t))
        Iterator()
    }
  }

  def callApiNode(url: String, buddhaFace: Int = BUDDHA_FACE, interval: Int = INTERVAL,
                  connectTimeout: Int = CONNECT_TIMEOUT, readTimeout: Int = READ_TIMEOUT,
                  encode: String = DEFAULT_CHARSET): Option[Node] = {
    callApi(url, buddhaFace, interval, connectTimeout, readTimeout, encode).map(content => {
      Option(toNode(content))
    }).getOrElse(None)
  }

  def toNode(str: String): Node = {
    val hp = new HtmlParser
    hp.setNamePolicy(XmlViolationPolicy.ALLOW)

    val saxer = new NoBindingFactoryAdapter
    hp.setContentHandler(saxer)
    hp.parse(new InputSource(new StringReader(str)))

    saxer.rootElem
  }

  def makeQuery(url: String, params: Map[String, String], encode: String = DEFAULT_CHARSET): String = {
    val query = new QueryBuilder(url, encode)
    params.foreach(param => {
      val (key, value) = param
      query.addParam(key, value)
    })
    query.toString()
  }

  private def httpGet(url: String, connectTimeout: Int, readTimeout: Int, encode: String): Option[String] = {
    val config: RequestConfig = RequestConfig.custom()
      .setSocketTimeout(readTimeout)
      .setConnectTimeout(connectTimeout)
      .build()
    val httpClient: CloseableHttpClient =
      HttpClientBuilder.create().setDefaultRequestConfig(config).build()
    val httpGet: HttpGet = new HttpGet(url)
    val response: CloseableHttpResponse = httpClient.execute(httpGet)
    try {
      val responseHandler: ResponseHandler[String] = new ResponseHandler[String] {
        override def handleResponse(httpResponse: HttpResponse): String = {
          val status = response.getStatusLine().getStatusCode()
          if (status >= 200 && status < 300) {
            val entity: HttpEntity = response.getEntity()
            if (entity != null) EntityUtils.toString(entity, encode)
            else null
          } else {
            throw new ClientProtocolException("Unexpected response status: " + status)
          }
        }
      }
      Option(httpClient.execute(httpGet, responseHandler))
    } finally {
      response.close()
    }
  }


}
