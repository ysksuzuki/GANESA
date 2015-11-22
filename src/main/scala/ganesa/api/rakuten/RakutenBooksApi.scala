package ganesa.api.rakuten

import com.typesafe.config.Config
import ganesa.api.{Const, ShopApi}
import ganesa.model._
import ganesa.util.HttpUtil
import ganesa.util.StringUtil._

import scala.xml.XML

class RakutenBooksApi(
    override val config: Config
) extends ShopApi {

  val appId = config.getString("authentication.rakutenBooks.appId")
  val endPoint = config.getString("authentication.rakutenBooks.endpoint")
  val categoryUrl = s"https://${endPoint}/services/api/BooksGenre/Search/20121128"
  val itemUrl = s"https://${endPoint}/services/api/BooksTotal/Search/20130522"
  val MAX_HITS = 3000
  val MAX_DEPTH = 5
  val MAX_HITS_EACH_REQUEST = 30

  override def getCategories(params: Map[String, String]): ShopCategory = {
    getCategoriesFromXml("category/rakutenBooks.xml")
  }

  override def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String],
                        f: (Seq[ShopItem]) => Seq[Item]): Seq[Item] = {
    def getItemsInner(params: Map[String, String]): Seq[Item] = {
      val query = getItemQuery(itemUrl, params, category.id, searchCriteria)
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val items = {
          (xml \\ "root" \ "Items" \ "Item").map(n => {
            val (jan, isbn) = {
              val _isbn = (n \ "isbn").text
              if (_isbn.length == 13) (_isbn, "")
              else ((n \ "jan").text, _isbn)
            }
            new ShopItem(
              0,
              (n \ "title").text,
              (n \ "itemPrice").text.toIntOpt.getOrElse(0),
              (n \ "itemUrl").text,
              RakutenBooksAvailability((n \ "availability").text).name,
              jan,
              isbn
            )
          })
        }
        f(filter(items))
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

  override def printCategories(): Unit = {
    Const.RakutenBooksCategories.foreach(n => printCategoriesInternal(n._1, n._2))
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
      /* Min Price */     false,
      /* Max Price */     false,
      /* Availability */  true,
      /* Sort */          true,
      Seq(RBSortStandard, RBSortSales, RBSortReleaseDateOld, RBSortReleaseDateNew, RBSortItemPriceCheep,
        RBSortItemPriceExpensive, RBSortReviewCount, RBSortReviewAverage),
      /* Sort Order */    false,
      /* Keyword Search*/ true)
  }

  override def getCategoriesFromShop(categoryId: String, params: Map[String, String], depth: Int): Seq[ShopCategory] = {
    if (depth > MAX_DEPTH) Seq()
    else {
      val query = HttpUtil.makeQuery(categoryUrl, params + ("applicationId"->appId, "booksGenreId"->categoryId, "format"->"xml"))
      HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
        val xml = XML.loadString(content)
        val categories = {
          (xml \\ "root" \ "children" \ "child").map(n => {
            val id = (n \ "booksGenreId").text
            new ShopCategory(
              id,
              (n \ "booksGenreName").text,
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

  private def getItemCount(categoryId: String, searchCriteria: SearchCriteria, params: Map[String, String]): Int = {
    val query = getItemQuery(itemUrl, params, categoryId, searchCriteria)
    HttpUtil.callApi(query, buddhaFace, interval, connectTimeout, readTimeout).map(content => {
      val xml = XML.loadString(content)
      val count = (xml \\ "root" \ "count").text.toIntOpt.getOrElse(0)
      if (count > MAX_HITS) MAX_HITS
      else count
    }).getOrElse(0)
  }

  private def getItemQuery(itemUrl: String, params: Map[String, String],
                           categoryId: String, searchCriteria: SearchCriteria): String = {
    HttpUtil.makeQuery(itemUrl,
      params +
        (
          "applicationId"->appId,
          "hits"->MAX_HITS_EACH_REQUEST.toString,
          "booksGenreId"->categoryId,
          "sort" -> s"${searchCriteria.sort.id}",
          "format"->"xml",
          "availability" -> {if (searchCriteria.availability) "1" else "0"})
    )
  }
}
