package ganesa.api.biccamera

import com.typesafe.config.Config
import ganesa.api.ShopApi
import ganesa.model._
import ganesa.util.{Formatter, Selector, HttpUtil}
import ganesa.util.StringUtil._

class BicCameraApi(
  override val config: Config
) extends ShopApi {

  val endPoint = config.getString("authentication.biccamera.endpoint")
  val itemUrl: String = s"http://$endPoint/bc/disp/CSfDispListPage_001.jsp"
  val maxHits = config.getString("settings.biccamera.maxHits").toIntOpt.getOrElse(0)
  val MAX_HITS_EACH_REQUEST = 25
  val IN_STOCK = "在庫あり"

  override def getCategories(params: Map[String, String]): ShopCategory = {
    getCategoriesFromXml("category/biccamera.xml")
  }

  override def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String], f: (Seq[ShopItem]) => Seq[Item]): Seq[Item] = {
    def getItemsInner(params: Map[String, String]): Seq[Item] = {
      val query = getItemQuery(itemUrl, params, category, searchCriteria)
      HttpUtil.callApiNode(query, buddhaFace, interval, connectTimeout, readTimeout, "MS932").map(node => {
        val $ = new Selector(node \\ "html")
        val items = $("div#listType02 ul li").map(n => {
          val $$ = new Selector(n)
          val itemUrl: String = $$("p.bic_item a") \@ "href"
          val jan = {
            if (!itemUrl.isEmpty) getJan(itemUrl)
            else ""
          }
          new ShopItem(
            0,
            $$("p.bic_item a").text,
            Formatter.parsePrice($$("p.bic_spprice span").text),
            itemUrl,
            {
              val availability = $$("p.zaiko").text
              if (!availability.isEmpty) availability
              else $$("p.soldout").text
            },
            jan,
            "")
        })
        val _items = filter(items)
        if (searchCriteria.availability) f(_items.filter(_.availability == IN_STOCK))
        else f(_items)
      }).getOrElse(Seq())
    }
    val count = {
      val itemCount = getItemCount(category, searchCriteria, params)
      if ((itemCount % MAX_HITS_EACH_REQUEST) > 0) itemCount / MAX_HITS_EACH_REQUEST + 1
      else itemCount / MAX_HITS_EACH_REQUEST
    }
    (1 to count).par.flatMap(offset => {
      if (cancel.get()) Seq()
      else {
        getItemsInner(params + ("p" -> offset.toString))
      }
    }).seq
  }

  override def printCategories(): Unit = {}

  override def getSearchCriteriaAvailable(): SearchCriteriaAvailable = {
    new SearchCriteriaAvailable(
      /* Min Price */      true,
      /* Max Price */      true,
      /* Availability */   true,
      /* Sort */           true,
      Seq(BicSortStandard, BicSortNew, BicSortCheap, BicSortExpensive, BicSortReleaseDate,
        BicSortDeliveryDate, BicSortReviewCount, BicSortReviewPoint),
      /* Sort Order */     true,
      /* Keyword Search */ true)
  }

  override def getAllItemAndTaskCount(categoryIds: Seq[ShopCategory], searchCriteria: SearchCriteria,
                                      params: Map[String, String]): (Int, Int) = {

    val allItemCount = categoryIds.par.map(shopCategory => getItemCount(shopCategory, searchCriteria, params)).sum
    val allTaskCount = allItemCount / MAX_HITS_EACH_REQUEST
    if ((allItemCount % MAX_HITS_EACH_REQUEST) > 0) (allItemCount, allTaskCount + 1)
    else (allItemCount, allTaskCount)
  }

  override def getCategoriesFromShop(categoryId: String, params: Map[String, String], depth: Int): Seq[ShopCategory] = Seq()

  private def getJan(itemUrl: String): String = {
    val content = HttpUtil.callApiIterator(itemUrl, buddhaFace, interval, connectTimeout, readTimeout, "MS932")
    val line = content.filter(_.contains("JANコード")).toList.headOption.getOrElse("")
    val pattern = """(.*)(\d{13})(.*)""".r
    line match {
      case pattern(a, b, c) => b
      case _ => ""
    }
  }
  private def getItemCount(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String]): Int = {
    val query = getItemQuery(itemUrl, params, category, searchCriteria)
    val content = HttpUtil.callApiIterator(query, buddhaFace, interval, connectTimeout, readTimeout, "MS932")
    val line = content.filter(_.contains("検索結果")).toList.headOption.getOrElse("")
    val pattern = """(.*検索結果)(\d*)(件中.*)""".r
    val count = line match {
      case pattern(a, b, c) => b
      case _ => ""
    }
    val intCount = count.toIntOpt.getOrElse(0)
    if (intCount > maxHits) maxHits
    else intCount
  }
  private def getItemQuery(itemUrl: String, params: Map[String, String],
                           category: ShopCategory, searchCriteria: SearchCriteria): String = {
    val categoryCriteria: Map[String, String] = {
      val criteria = category.criteria.split("=")
      if (criteria.length > 1) Map((criteria(0), criteria(1)))
      else Map()
    }
    val availability: Map[String, String] = {
      if (searchCriteria.availability) Map(("sold_out_tp1", "1"))
      else Map()
    }
    val price: Map[String, String] = {
      var _price: Map[String, String] = Map()
      if (searchCriteria.minPrice > 0) {
        _price = _price + ("min" -> searchCriteria.minPrice.toString)
      }
      if (searchCriteria.maxPrice > 0) {
        _price = _price + ("max" -> searchCriteria.maxPrice.toString)
      }
      _price
    }
    HttpUtil.makeQuery(itemUrl,
      params + ("dispNo" -> category.id, "sort" -> searchCriteria.sort.id)
        ++ categoryCriteria
        ++ price
        ++ availability,
      "MS932"
    )
  }
}
