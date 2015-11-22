package ganesa.api.rakuten

import com.typesafe.config.Config
import ganesa.api.{Const, ShopApi}
import ganesa.model._
import ganesa.util.{FilenameWithoutExtension, HttpUtil}
import ganesa.util.StringUtil._

import scala.xml.XML

class RakutenIchibaApi(
  override val config: Config
) extends ShopApi {

  val appId = config.getString("authentication.rakuten.appId")
  val endPoint = config.getString("authentication.rakuten.endpoint")
  val itemUrl = s"https://$endPoint/services/api/Product/Search/20140305"
  val categoryUrl = s"https://$endPoint/services/api/IchibaGenre/Search/20140222"
  val MAX_HITS = 3000
  val MAX_DEPTH = 5
  val MAX_HITS_EACH_REQUEST = 30

  override def getCategories(params: Map[String, String]): ShopCategory = {
    getCategoriesFromXml("category/rakutenIchiba.xml")
  }

  override def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String],
                        f: (Seq[ShopItem]) => Seq[Item]): Seq[Item] = {
    def getItemsInner(params: Map[String, String]): Seq[Item] = {
      val query = getItemQuery(itemUrl, params, category.id, searchCriteria)
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val items = {
          (xml \\ "root" \ "Products" \ "Product").map(n => {
            val usedExcludeSalesItemCount = (n \ "usedExcludeSalesItemCount").text.toIntOpt.getOrElse(0)
            if (!searchCriteria.availability || usedExcludeSalesItemCount > 0) {
              val smallImageUrl = (n \ "smallImageUrl").text
              val productURlPC = (n \ "productUrlPC").text
              val jan = getJan(smallImageUrl, productURlPC)
              new ShopItem(
                0,
                (n \ "productName").text,
                (n \ "usedExcludeSalesMinPrice").text.toIntOpt.getOrElse(0),
                productURlPC,
                if (usedExcludeSalesItemCount > 0) "在庫あり"
                else "在庫なし",
                jan,
                ""
              )
            } else new ShopItem()
          })
        }
        f(filter(items).filterNot(_.availability.isEmpty))
      }).getOrElse(Seq())
    }
    val count = {
      val itemCount = getItemCount(category.id, searchCriteria, params)
      if ((itemCount % MAX_HITS_EACH_REQUEST) > 0) itemCount / MAX_HITS_EACH_REQUEST + 1
      else itemCount / MAX_HITS_EACH_REQUEST
    }
    (1 to count).par.flatMap(offset => {
      if (cancel.get()) Seq()
      else {
        getItemsInner(params + ("page" -> offset.toString))
      }
    }).seq

  }

  private def getJan(smallImageUrl: String, productUrlPC: String): String = {
    val jan: String = smallImageUrl match {
      case FilenameWithoutExtension(fileName) => {
        val pattern = """(\d{7})(\d{13})(.*)""".r
        fileName match {
          case pattern(a, b, c) => b
          case _ => ""
        }
      }
      case _ => ""
    }
    if (!jan.isEmpty) jan
    else {
      val line = HttpUtil.callApiIterator(productUrlPC, buddhaFace, interval, connectTimeout, readTimeout)
        .filter(_.contains("JAN")).toList.headOption.getOrElse("")
      val pattern = """(.*JAN:)(\d{13})(.*)""".r
      line match {
        case pattern(a, b, c) => b
        case _ => ""
      }
    }
  }

  override def getAllItemAndTaskCount(categoryIds: Seq[ShopCategory], searchCriteria: SearchCriteria,
                                      params: Map[String, String]): (Int, Int) = {
    val allItemCount = categoryIds.par.map(shopCategory => getItemCount(shopCategory.id, searchCriteria, params)).sum
    val allTaskCount = allItemCount / MAX_HITS_EACH_REQUEST
    if ((allItemCount % MAX_HITS_EACH_REQUEST) > 0) (allItemCount, allTaskCount + 1)
    else (allItemCount, allTaskCount)
  }

  override def getSearchCriteriaAvailable(): SearchCriteriaAvailable = {
    SearchCriteriaAvailable(
      /* Min Price */      true,
      /* Max Price */      true,
      /* Availability */   true,
      /* Sort */           true,
      Seq(RISortStandard, RISortReleaseDate, RISortSeller, RISortSatisfied),
      /* Sort Order */     false,
      /* Keyword Search */ true)
  }

  override def getCategoriesFromShop(categoryId: String, params: Map[String, String] = Map(), depth: Int = 1): Seq[ShopCategory] = {
    if (depth > MAX_DEPTH) Seq()
    else {
      val query = HttpUtil.makeQuery(categoryUrl, params + ("applicationId"->appId, "genreId"->categoryId, "format"->"xml"))
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val categories = {
          (xml \\ "root" \ "children" \ "child").map(n => {
            val id = (n \ "genreId").text
            new ShopCategory(
              id,
              (n \ "genreName").text,
              "",
              "",
              "",
              getCategoriesFromShop(id, params, depth + 1)
            )
          })
        }
        categories
      }).getOrElse(Seq())
    }
  }

  override def printCategories(): Unit = {
    Const.RakutenCategories.foreach(n => printCategoriesInternal(n._1, n._2))
  }

  private def getItemCount(categoryId: String, searchCriteria: SearchCriteria, params: Map[String, String]): Int = {
    val query = getItemQuery(itemUrl, params, categoryId, searchCriteria)
    HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
      val xml = XML.loadString(content)
      val count = (xml \\ "root" \ "count").text.toIntOpt.getOrElse(0)
      if (count > MAX_HITS) MAX_HITS
      else count
    }).getOrElse(0)
  }

  private def getItemQuery(itemUrl: String, params: Map[String, String], categoryId: String,
                           searchCriteria: SearchCriteria): String = {
    val price: Map[String, String] = {
      var _price: Map[String, String] = Map()
      if (searchCriteria.minPrice > 0) {
        _price = _price + ("minPrice" -> searchCriteria.minPrice.toString)
      }
      if (searchCriteria.maxPrice > 0) {
        _price = _price + ("maxPrice" -> searchCriteria.maxPrice.toString)
      }
      _price
    }
    HttpUtil.makeQuery(itemUrl,
      params +
        (
          "applicationId"->appId,
          "hits"->MAX_HITS_EACH_REQUEST.toString,
          "genreId"->categoryId,
          "sort" -> s"${searchCriteria.sort.id}",
          "format"->"xml",
          "availability" -> {if (searchCriteria.availability) "1" else "0"}) ++ price
    )
  }
}
