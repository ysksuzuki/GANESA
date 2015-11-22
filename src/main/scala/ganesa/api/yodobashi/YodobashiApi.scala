package ganesa.api.yodobashi

import com.typesafe.config.Config
import ganesa.api.ShopApi
import ganesa.model._
import ganesa.util.{Formatter, Selector, HttpUtil}
import ganesa.util.StringUtil._

class YodobashiApi(
  override val config: Config
) extends ShopApi {

  val endPoint = config.getString("authentication.yodobashi.endpoint")
  val itemUrl = s"http://$endPoint"
  val maxHits = config.getString("settings.yodobashi.maxHits").toIntOpt.getOrElse(0)
  val MAX_HITS_EACH_REQUEST = 48
  val IN_STOCK = "在庫あり"
  val IN_FEW_STOCK = "在庫残少 ご注文はお早めに！"

  override def getCategories(params: Map[String, String]): ShopCategory = {
    getCategoriesFromXml("category/yodobashi.xml")
  }

  override def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String], f: (Seq[ShopItem]) => Seq[Item]): Seq[Item] = {
    def getItemsInner(params: Map[String, String]): Seq[Item] = {
      val query = getItemQuery(itemUrl, params, category, searchCriteria)
      HttpUtil.callApiNode(query, buddhaFace, interval, connectTimeout, readTimeout).map(node => {
        val $ = new Selector(node \\ "html")
        val items = $("div#spt_hznList div.inner").map(n => {
          val $$ = new Selector(n)
          val (itemDetailUrl, title) =
            $$("a").headOption.map(a => (a \@ "href", (a \ "div" \ "strong").text)).getOrElse(("", ""))
          new ShopItem(
          0,
          title,
          Formatter.parsePrice($$("div.pInfo ul li strong.red").text.replaceAll("￥", "")),
          s"$itemUrl$itemDetailUrl",
          {
            val available = $$("div.pInfo ul li span.green").text
            if (!available.isEmpty) available
            else {
              val bookNow = $$("div.pInfo ul li span.blue").text
              if (!bookNow.isEmpty) bookNow
              else $$("div.pInfo ul li span.red").text
            }
          },
          "",
          "")
        })
        val _items = filter(items)
        if (searchCriteria.availability) {
          f(_items.filter(a => a.availability == IN_STOCK || a.availability == IN_FEW_STOCK))
        }
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
        getItemsInner(params + ("page" -> offset.toString))
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
      Seq(YodobashiCoincidenceRanking, YodobashiNewArrivalRanking, YodobashiReleaseDateDesc,
        YodobashiSellPriceDesc, YodobashiSellPriceAsc),
      /* Sort Order */     true,
      /* Keyword Search */ true)
  }

  override def getAllItemAndTaskCount(categoryIds: Seq[ShopCategory], searchCriteria: SearchCriteria, params: Map[String, String]): (Int, Int) = {
    val allItemCount = categoryIds.par.map(shopCategory => getItemCount(shopCategory, searchCriteria, params)).sum
    val allTaskCount = allItemCount / MAX_HITS_EACH_REQUEST
    if ((allItemCount % MAX_HITS_EACH_REQUEST) > 0) (allItemCount, allTaskCount + 1)
    else (allItemCount, allTaskCount)
  }

  override def getCategoriesFromShop(categoryId: String, params: Map[String, String], depth: Int): Seq[ShopCategory] = Seq()

  private def getItemCount(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String]): Int = {
    val query = getItemQuery(itemUrl, params, category, searchCriteria)
    val content = HttpUtil.callApiIterator(query, buddhaFace, interval, connectTimeout, readTimeout)
    val line = content.filter(_.contains("件ヒット")).toList.headOption.getOrElse("")
    val pattern = """(.*で)(\d*)(件ヒット.*)""".r
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
    val availability: Map[String, String] = {
      if (searchCriteria.availability) Map(("discontinued", "false"))
      else Map()
    }
    val price: Map[String, String] = {
      var _price: Map[String, String] = Map()
      if (searchCriteria.minPrice > 0) {
        _price = _price + ("lower" -> searchCriteria.minPrice.toString)
      }
      if (searchCriteria.maxPrice > 0) {
        _price = _price + ("upper" -> searchCriteria.maxPrice.toString)
      }
      _price
    }
    HttpUtil.makeQuery(s"$itemUrl/${category.title}/ct/${category.id}",
      params +
        (
          "count" -> MAX_HITS_EACH_REQUEST.toString,
          "sorttyp" -> searchCriteria.sort.id)
        ++ price
        ++ availability
    )
  }
}
