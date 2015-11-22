package ganesa.api.amazon

import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.config.Config
import ganesa.application.CommissionCalculator
import ganesa.model._
import ganesa.util.{NumberUtil, StringUtil, HttpUtil}
import ganesa.util.StringUtil._

import scala.xml.{Node, XML}

object AmazonApi {
  def apply(amazonApi: AmazonApiEnum, config: Config,
            commissionCalculator: CommissionCalculator, keywordSearch: Boolean) = {
    amazonApi match {
      case ProductAdvertisingApi => {
        new AmazonProductAdvertisingApi(config, commissionCalculator, keywordSearch)
      }
      case MWSApi => {
        new AmazonMWSApi(config, commissionCalculator, keywordSearch)
      }
    }
  }
}

trait AmazonApi {
  val config: Config
  val keywordSearch: Boolean
  val commissionCalculator: CommissionCalculator
  val awsAccessKeyId: String = config.getString("authentication.amazon.awsAccessKeyId")
  val awsSecretKey: String = config.getString("authentication.amazon.awsSecretKey")
  val associateTag: String = config.getString("authentication.amazon.associateTag")
  val endpoint: String = config.getString("authentication.amazon.endpoint")
  val buddhaFace = config.getInt("settings.general.buddhaFace")
  val interval = config.getInt("settings.general.interval")
  val connectTimeout = config.getInt("settings.general.connectTimeout")
  val readTimeout = config.getInt("settings.general.readTimeout")
  val cancel = new AtomicBoolean(false)
  def getItems(amazonCategory: String, shopItems: Seq[ShopItem], params: Map[String, String] = Map()): Map[String, Seq[AmazonItem]]
}

class AmazonProductAdvertisingApi(
  override val config: Config,
  override val commissionCalculator: CommissionCalculator,
  override val keywordSearch: Boolean = false) extends AmazonApi {

  val MAX_ITEM = 10
  val searchTypes = {
    if (keywordSearch) {
      Seq(AmazonApiSearchType("EAN"), AmazonApiSearchType("ISBN"), AmazonApiSearchType("KEYWORD"))
    } else {
      Seq(AmazonApiSearchType("EAN"), AmazonApiSearchType("ISBN"))
    }
  }

  override def getItems(amazonCategory: String, shopItems: Seq[ShopItem], params: Map[String, String] = Map()): Map[String, Seq[AmazonItem]] = {
    def getItemsWithId(shopItems: Seq[ShopItem], searchType: AmazonApiSearchType): Map[String, Seq[AmazonItem]] = {
      val helper = new SignedRequestsHelper(awsAccessKeyId, awsSecretKey, endpoint)
      val itemId = {
        searchType match {
          case EanCodeSearch => shopItems.map(_.janCode).mkString(",")
          case IsbnCodeSearch => shopItems.map(_.isbnCode).mkString(",")
        }
      }
      val _params = params ++ Map(
        "AssociateTag" -> associateTag,
        "Service" -> "AWSECommerceService",
        "Version" -> "2013-08-01",
        "Operation" -> "ItemLookup",
        "SearchIndex"->amazonCategory,
        "IdType"->searchType.name,
        "ItemId"->itemId,
        "ResponseGroup"->"ItemAttributes,OfferSummary,SalesRank"
      )
      val url = helper.sign(_params)
      HttpUtil.callApi(url, buddhaFace, interval, connectTimeout, readTimeout).map(response => {
        val xml = XML.loadString(response)
        val items: Seq[(String, AmazonItem)] = (xml \\ "ItemLookupResponse" \ "Items" \ "Item") map(item => {
          val code = (item \ "ItemAttributes" \ searchType.name).text
          (
            code,
            getAmazonItem(item)
          )
        })
        items.foldLeft(Map[String, Seq[AmazonItem]]())((a: Map[String, Seq[AmazonItem]], b: (String, AmazonItem)) => {
          val (key, value) = b
          val values: Seq[AmazonItem] = a.get(key).map(_ :+ value).getOrElse(Seq(value))
          a + (key->values)
        })
      }).getOrElse(Map())
    }
    def getItemWithTitle(shopItems: Seq[ShopItem]): Map[String, Seq[AmazonItem]] = {
      val helper = new SignedRequestsHelper(awsAccessKeyId, awsSecretKey, endpoint)
      shopItems.par.map(shopItem => {
        val _params = params ++ Map(
          "AssociateTag" -> associateTag,
          "Service" -> "AWSECommerceService",
          "Operation" -> "ItemSearch",
          "SearchIndex" -> amazonCategory,
          "Keywords" -> StringUtil.normalizeKeyword(shopItem.title),
          "ResponseGroup"->"ItemAttributes,OfferSummary,SalesRank"
        )
        val url = helper.sign(_params)
        HttpUtil.callApi(url, buddhaFace, interval, connectTimeout, readTimeout).map(response => {
          val xml = XML.loadString(response)
          (xml \\ "ItemSearchResponse" \ "Items" \ "Item").headOption.map(item => {
            (
              shopItem.title,
              Seq(getAmazonItem(item))
            )
          }).getOrElse((shopItem.title, Seq(new AmazonItem())))
        }).getOrElse((shopItem.title, Seq(new AmazonItem())))
      }).seq.toMap
    }

    def getAmazonItem(item: Node): AmazonItem = {
      val lowestNewPrice = (item \ "OfferSummary" \ "LowestNewPrice" \ "Amount").text.toIntOpt.getOrElse(0)
      val height = NumberUtil.inchToCm((item \ "ItemAttributes" \ "PackageDimensions" \ "Height").text.toIntOpt.getOrElse(0))
      val length = NumberUtil.inchToCm((item \ "ItemAttributes" \ "PackageDimensions" \ "Length").text.toIntOpt.getOrElse(0))
      val width = NumberUtil.inchToCm((item \ "ItemAttributes" \ "PackageDimensions" \ "Width").text.toIntOpt.getOrElse(0))
      val weight = NumberUtil.poundToGram((item \ "ItemAttributes" \ "PackageDimensions" \ "Weight").text.toIntOpt.getOrElse(0))
      val productGroup = (item \ "ItemAttributes" \ "ProductGroup").text
      new AmazonItem(
        (item \ "OfferSummary" \ "TotalNew").text.toIntOpt.getOrElse(0),
        (item \ "OfferSummary" \ "TotalUsed").text.toIntOpt.getOrElse(0),
        lowestNewPrice,
        (item \ "OfferSummary" \ "LowestUsedPrice" \ "Amount").text.toIntOpt.getOrElse(0),
        (item \ "SalesRank").text.toIntOpt.getOrElse(0),
        (item \ "DetailPageURL").text,
        (item \ "ItemAttributes" \ "EAN").text,
        (item \ "ItemAttributes" \ "ISBN").text,
        (item \ "ItemAttributes" \ "Title").text,
        commissionCalculator.calculate(lowestNewPrice, productGroup, new ItemDimensions(length, width, height, weight))
      )
    }

    def getItemsInternal(items: Seq[ShopItem], searchType: AmazonApiSearchType): Map[String, Seq[AmazonItem]] = {
      searchType match {
        case EanCodeSearch => getItemsWithId(items, searchType)
        case IsbnCodeSearch => getItemsWithId(items, searchType)
        case KeywordSearch => getItemWithTitle(items)
      }
    }

    val (jan, isbn, other) = {
      val (_jan, janIsEmpty) = shopItems.partition(!_.janCode.isEmpty)
      val (_isbn, other) = janIsEmpty.partition(!_.isbnCode.isEmpty)
      (_jan, _isbn, other)
    }
    searchTypes.zip(Seq(jan, isbn, other)).par.flatMap(n => {
      val (searchType, shopItems) = n
      shopItems.grouped(MAX_ITEM).toParArray.flatMap(items => {
        if (cancel.get()) Map[String, Seq[AmazonItem]]()
        else getItemsInternal(items, searchType)
      }).seq.toMap
    }).seq.toMap
  }

}
class AmazonMWSApi(
  override val config: Config,
  override val commissionCalculator: CommissionCalculator,
  override val keywordSearch: Boolean = false) extends AmazonApi {
  override def getItems(amazonCategory: String, shopItems: Seq[ShopItem], params: Map[String, String]): Map[String, Seq[AmazonItem]] = ???
}
