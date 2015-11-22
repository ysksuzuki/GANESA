package ganesa.api.yahoo

import com.typesafe.config.Config
import ganesa.api.{Const, ShopApi}
import ganesa.model._
import ganesa.util.HttpUtil
import ganesa.util.StringUtil._

import scala.xml.{XML}

class YahooShoppingApi(
  override val config: Config
) extends ShopApi {

  val appId = config.getString("authentication.yahoo.appId")
  val endPoint = config.getString("authentication.yahoo.endpoint")
  val categoryUrl = s"http://$endPoint/categorySearch"
  val itemUrl = s"http://$endPoint/itemSearch"
  val MAX_HITS = 1000
  val MAX_HITS_EACH_REQUEST = 50
  val MAX_DEPTH = 5

  override def getCategories(params: Map[String, String] = Map()): ShopCategory = {
    getCategoriesFromXml("category/yahooShopping.xml")
  }

  override def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String] = Map(),
                        f: Seq[ShopItem] => Seq[Item]): Seq[Item] = {
    def getItemsInner(params: Map[String, String]): Seq[Item] = {
      val query = getItemQuery(itemUrl, params, category.id, searchCriteria)
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val items = {
          (xml \\ "Hit").map(n => {
            new ShopItem(
              (n \@ "index").toIntOpt.getOrElse(0),
              (n \ "Name").text,
              (n \ "Price").text.toIntOpt.getOrElse(0),
              (n \ "Url").text,
              (n \ "Availability").text,
              (n \ "JanCode").text,
              (n \ "IsbnCode").text
            )
          })
        }
        f(filter(items))
      }).getOrElse(Seq())
    }
    val count = getItemCount(category.id, searchCriteria, params)
    (0 until count by MAX_HITS_EACH_REQUEST).par.flatMap(offset => {
      if (cancel.get()) Seq()
      else {
        getItemsInner(params + ("offset" -> offset.toString))
      }
    }).seq
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
      /* Min price */      true,
      /* Max price */      true,
      /* Availability */   true,
      /* Sort */           true,
      Seq(YSSortPrice, YSSortName, YSSortScore, YSSortSold, YSSortAffiliate, YSSortReviewCount),
      /* Sort Order */     true,
      /* Keyword Search */ true)
  }

  override def getCategoriesFromShop(categoryId: String, params: Map[String, String] = Map(), depth: Int = 1): Seq[ShopCategory] = {
    if (depth > MAX_DEPTH) Seq()
    else {
      val query = HttpUtil.makeQuery(categoryUrl, params + ("appId"->appId, "category_id"->categoryId))
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val categories = {
          (xml \\ "Categories" \ "Children" \ "Child").map(n => {
            val id = (n \ "Id").text
            new ShopCategory(
              id,
              (n \ "Title" \ "Medium").text,
              n \@ "sortOrder",
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

  def printCategories(): Unit = {
    Const.YahooShoppingCategories.foreach(n => printCategoriesInternal(n._1, n._2))
  }

  private def getItemCount(categoryId: String, searchCriteria: SearchCriteria, params: Map[String, String]): Int = {
    val query = getItemQuery(itemUrl, params, categoryId, searchCriteria)
    HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
      val xml = XML.loadString(content)
      val count = (xml \\ "ResultSet" \@ "totalResultsAvailable").toIntOpt.getOrElse(0)
      if (count > MAX_HITS) MAX_HITS
      else count
    }).getOrElse(0)
  }

  private def getItemQuery(itemUrl: String, params: Map[String, String],
                           categoryId: String, searchCriteria: SearchCriteria): String = {
    val price: Map[String, String] = {
      var _price: Map[String, String] = Map()
      if (searchCriteria.minPrice > 0) {
        _price = _price + ("price_from" -> searchCriteria.minPrice.toString)
      }
      if (searchCriteria.maxPrice > 0) {
        _price = _price + ("price_to" -> searchCriteria.maxPrice.toString)
      }
      _price
    }
    HttpUtil.makeQuery(itemUrl,
      params +
        (
          "appId" -> appId,
          "hits" -> MAX_HITS_EACH_REQUEST.toString,
          "category_id" -> categoryId,
          "sort" -> s"${searchCriteria.order.id}${searchCriteria.sort.id}",
          "availability" -> {if (searchCriteria.availability) "1" else "0"}) ++ price
    )
  }
}
