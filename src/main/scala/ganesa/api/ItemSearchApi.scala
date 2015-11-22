package ganesa.api

import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

import ganesa.api.amazon.AmazonApi
import ganesa.model._

class ItemSearchApi(val amazonApi: AmazonApi, val shopApi: ShopApi) {
  private val logger: Logger = Logger(LoggerFactory.getLogger(ItemSearchApi.this.getClass))

  def getItems(categories: Seq[ShopCategory], searchCriteria: SearchCriteria,
               updateProgress: (Long, Long) => Unit,
               updateValue: (Seq[Item]) => Unit,
               updateMessage: String => Unit): Seq[Item] = {
    logger.info(s"Categories=[${categories.map(c => s"${c.title}(${c.id})").mkString(", ")}]")
    logger.info(s"SearchCriteria=[minPrice=${searchCriteria.minPrice}, maxPrice=${searchCriteria.maxPrice}, "
      + s"availability=${searchCriteria.availability}, sort=${searchCriteria.sort.name}, "
      + s"order=${searchCriteria.order.name}, keywordSearch=${amazonApi.keywordSearch}]")
    val progress = new AtomicInteger(0)
    val (itemCount, taskCount) = shopApi.getAllItemAndTaskCount(categories, searchCriteria)
    updateMessage(s"Searching... (Target: $itemCount items)")
    categories.par.flatMap(category => {
      shopApi.getItems(category, searchCriteria, Map(), { shopItems: Seq[ShopItem] => {
        val result = getAmazonItems(shopItems, category)
        val curr = progress.incrementAndGet()
        updateProgress(curr, taskCount)
        updateValue(result)
        result
      }})
    }).seq
  }

  def getCategories(params: Map[String, String] = Map()): ShopCategory = {
    shopApi.getCategories(params)
  }

  def getSearchCriteriaAvailable(): SearchCriteriaAvailable = shopApi.getSearchCriteriaAvailable()

  def cancel(): Unit = {
    shopApi.cancel.set(true)
    amazonApi.cancel.set(true)
  }

  private def getAmazonItems(shopItems: Seq[ShopItem], category: ShopCategory) = {
    if (shopItems.isEmpty) Seq()
    else {
      val amazonItems: Map[String, Seq[AmazonItem]] = amazonApi.getItems(category.amazonCategory, shopItems)
      itemAggregate(shopItems, amazonItems)
    }
  }

  private def itemAggregate(shopItems: Seq[ShopItem], amazonItems: Map[String, Seq[AmazonItem]]): Seq[Item] = {
    shopItems.flatMap(shopItem => {
      val amazonItem: Seq[AmazonItem] =
        amazonItems.getOrElse(shopItem.janCode,
          amazonItems.getOrElse(shopItem.isbnCode,
            amazonItems.getOrElse(shopItem.title, Seq(new AmazonItem()))))
      itemAggregateInternal(shopItem, amazonItem)
    })
  }

  private def itemAggregateInternal(shopItem: ShopItem, amazonItem: Seq[AmazonItem]): Seq[Item] = {
    def calcProfit(amazonPrice: Int, shopPrice: Int, commission: Int): Int = {
      if (amazonPrice == 0) 0
      else amazonPrice - commission - shopPrice
    }
    amazonItem.map(item => {
      new Item(shopItem, item,
        calcProfit(item.lowestNewPrice, shopItem.price, item.commission.total()))
    })
  }

}
