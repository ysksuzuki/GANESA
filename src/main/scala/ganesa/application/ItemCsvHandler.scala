package ganesa.application

import java.io.File

import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import ganesa.model._
import ganesa.util.StringUtil._

object ItemCsvHandler {

  val TITLE_SHOP = "タイトル(Shop)"
  val DETAIL_PAGE = "詳細ページ"
  val PURCHASE_PRICE = "仕入値(新品)"
  val STOCK = "在庫有無"
  val JAN_SHOP = "Jan(Shop)"
  val ISBN_SHOP = "Isbn(Shop)"
  val TITLE_AMAZON = "タイトル(Amazon)"
  val NEW_PRICE = "新品価格"
  val RANKING = "ランキング"
  val ITEM_PAGE = "商品ページ"
  val NEW_ITEM_COUNT = "新品数"
  val JAN_AMAZON = "Jan(Amazon)"
  val ISBN_AMAZON = "Isbn(Amazon)"
  val ANTICIPATORY_PROFIT = "見込利益(新品)"
  val SALES_COMMISSION = "販売手数料"
  val STORAGE_COMMISSION = "月間在庫保管手数料"
  val SHIPPING_COMMISSION = "出荷作業手数料"
  val WEIGHT_COMMISSION = "発送重量手数料"
  val WEIGHT_EXTRA_COMMISSION = "発送重量超過手数料"
  val PRODUCT_GROUP = "Product Group"
  val DELIVERY_COMMISSION_DIV = "配送手数料区分"
  val LENGTH = "length"
  val WIDTH = "width"
  val HEIGHT = "height"
  val WEIGHT = "weight"

  val header: Seq[String] = Seq(
    TITLE_SHOP, DETAIL_PAGE, PURCHASE_PRICE, STOCK, JAN_SHOP, ISBN_SHOP,
    TITLE_AMAZON, NEW_PRICE, RANKING, ITEM_PAGE, NEW_ITEM_COUNT, JAN_AMAZON, ISBN_AMAZON, ANTICIPATORY_PROFIT,
    SALES_COMMISSION, STORAGE_COMMISSION, SHIPPING_COMMISSION, WEIGHT_COMMISSION, WEIGHT_EXTRA_COMMISSION,
    PRODUCT_GROUP, DELIVERY_COMMISSION_DIV, LENGTH, WIDTH, HEIGHT, WEIGHT
  )
  val chunkSize: Int = 1000
  val DEFAULT_ENCODE = "UTF-8"

  def write(items: Seq[Item], file: File, encode: String = DEFAULT_ENCODE) = {
    val writer = CSVWriter.open(file, encode)
    writer.writeRow(header)
    items.map(i => Seq(
      i.shopItem.title, i.shopItem.detailPage, i.shopItem.price, i.shopItem.availability, i.shopItem.janCode, i.shopItem.isbnCode,
      i.amazonItem.title, i.amazonItem.lowestNewPrice, i.amazonItem.salesRank, i.amazonItem.detailPage,
      i.amazonItem.totalNew, i.amazonItem.janCode, i.amazonItem.isbnCode, i._anticipatoryProfit,
      i.amazonItem.commission.sales, i.amazonItem.commission.storage, i.amazonItem.commission.shipping,
      i.amazonItem.commission.weight, i.amazonItem.commission.weightExtra, i.amazonItem.commission.productGroup,
      i.amazonItem.commission.deliveryCommission.name, i.amazonItem.commission.itemDimensions.length,
      i.amazonItem.commission.itemDimensions.width, i.amazonItem.commission.itemDimensions.height,
      i.amazonItem.commission.itemDimensions.weight
    ))
      .grouped(chunkSize).foreach(i => writer.writeAll(i))
    writer.close()
  }

  def read(file: File, encode: String = DEFAULT_ENCODE) = {
    val reader = CSVReader.open(file, encode)
    reader.allWithHeaders().map(i => {
      new Item(
        new ShopItem(0,
          i.getOrElse(TITLE_SHOP, ""),
          i.getOrElse(PURCHASE_PRICE, "").toIntOpt.getOrElse(0),
          i.getOrElse(DETAIL_PAGE, ""),
          i.getOrElse(STOCK, ""),
          i.getOrElse(JAN_SHOP, ""),
          i.getOrElse(ISBN_SHOP, "")),
        new AmazonItem(
          i.getOrElse(NEW_ITEM_COUNT, "").toIntOpt.getOrElse(0),
          0,
          i.getOrElse(NEW_PRICE, "").toIntOpt.getOrElse(0),
          0,
          i.getOrElse(RANKING, "").toIntOpt.getOrElse(0),
          i.getOrElse(ITEM_PAGE, ""),
          i.getOrElse(JAN_AMAZON, ""),
          i.getOrElse(ISBN_AMAZON, ""),
          i.getOrElse(TITLE_AMAZON, ""),
          new FBACommission(
            i.getOrElse(SALES_COMMISSION, "").toIntOpt.getOrElse(0),
            i.getOrElse(STORAGE_COMMISSION, "").toIntOpt.getOrElse(0),
            i.getOrElse(SHIPPING_COMMISSION, "").toIntOpt.getOrElse(0),
            i.getOrElse(WEIGHT_COMMISSION, "").toIntOpt.getOrElse(0),
            i.getOrElse(WEIGHT_EXTRA_COMMISSION, "").toIntOpt.getOrElse(0),
            i.getOrElse(PRODUCT_GROUP, ""),
            AmazonDeliveryCommission(i.getOrElse(DELIVERY_COMMISSION_DIV, "")),
            new ItemDimensions(
              BigDecimal(i.getOrElse(LENGTH, "").toIntOpt.getOrElse(0)),
              BigDecimal(i.getOrElse(WIDTH, "").toIntOpt.getOrElse(0)),
              BigDecimal(i.getOrElse(HEIGHT, "").toIntOpt.getOrElse(0)),
              BigDecimal(i.getOrElse(WEIGHT, "").toIntOpt.getOrElse(0))
            )
          )
        )
      )
    })
  }
}
