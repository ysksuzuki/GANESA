package ganesa.api.amazon

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import scala.collection.immutable.TreeMap
import scala.collection.immutable.SortedMap

class SignedRequestsHelper(
    val awsAccessKeyId: String,
    val awsSecretKey: String,
    val endpoint: String = "webservices.amazon.com") {

  var toSign = ""

  private val UTF8_CHARSET = "UTF-8"
  private val HMAC_SHA256_ALGORITHM = "HmacSHA256"
  private val REQUEST_URI = "/onca/xml"
  private val REQUEST_METHOD = "GET"

  private var secretKeySpec: SecretKeySpec = null
  private var mac: Mac = null

  val secretyKeyBytes: Array[Byte] = awsSecretKey.getBytes(UTF8_CHARSET)
  secretKeySpec = new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM)
  mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
  mac.init(secretKeySpec)

  def sign(params: Map[String, String]): String = {
    var finalParams = params + ("AWSAccessKeyId" -> awsAccessKeyId)
    if (!finalParams.contains("Timestamp")) {
      finalParams = finalParams + ("Timestamp" -> getTimestamp)
    }
    val sortedParamMap = TreeMap[String, String](finalParams.toList: _*)
    val canonicalQS = canonicalize(sortedParamMap)
    toSign =
      REQUEST_METHOD + "\n" +
        endpoint + "\n" +
        REQUEST_URI + "\n" +
        canonicalQS

    val signed = hmac(toSign)
    val sig = percentEncodeRfc3986(signed)

    "http://" + endpoint + REQUEST_URI + "?" +
      canonicalQS + "&Signature=" + sig
  }

  private def hmac(stringToSign: String): String = {
    var signature: String = null
    try {
      val data = stringToSign.getBytes(UTF8_CHARSET)
      val rawHmac = mac.doFinal(data)
      val encoder = new Base64()
      signature = new String(encoder.encode(rawHmac))
    } catch {
      case e: UnsupportedEncodingException =>
        throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e)
    }

    signature
  }

  private def getTimestamp = {
    val cal = Calendar.getInstance()
    val dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dfm.setTimeZone(TimeZone.getTimeZone("GMT"))
    dfm.format(cal.getTime())
  }

  private def canonicalize(sortedParamMap: SortedMap[String, String]) : String = {
    if (sortedParamMap.isEmpty) {
      return "";
    }

    val buffer = new StringBuffer()
    val iter = sortedParamMap.iterator

    while (iter.hasNext) {
      val kvpair = iter.next()
      buffer.append(percentEncodeRfc3986(kvpair._1))
      buffer.append("=")
      buffer.append(percentEncodeRfc3986(kvpair._2))
      if (iter.hasNext) {
        buffer.append("&")
      }
    }

    buffer.toString()
  }

  private def percentEncodeRfc3986(s: String): String = {
    var out: String = null
    try {
      out = URLEncoder.encode(s, UTF8_CHARSET)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
    } catch {
      case e: UnsupportedEncodingException => out = s
    }

    out;
  }

}
