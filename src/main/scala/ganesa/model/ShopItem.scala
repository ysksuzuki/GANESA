package ganesa.model

import scalafx.beans.property.{ObjectProperty, StringProperty}

case class ShopCategory(
    id: String,

    title: String,
    sortOrder: String,
    amazonCategory: String,
    criteria: String = "",
    children: Seq[ShopCategory] = Seq()
) {
    override def toString(): String = title
}

case class ShopItem(
    index: Int = 0,
    title: String = "",
    price: Int = 0,
    detailPage: String = "",
    availability: String = "",
    janCode: String = "",
    isbnCode: String = ""
)

case class AmazonItem(
    totalNew: Int = 0,
    totalUsed: Int = 0,
    lowestNewPrice: Int = 0,
    lowestUsedPrice: Int = 0,
    salesRank: Int = 0,
    detailPage: String = "",
    janCode: String = "",
    isbnCode: String = "",
    title: String = "",
    commission: FBACommission = new FBACommission()
)

case class Item(
  shopItem: ShopItem = new ShopItem(),
  amazonItem: AmazonItem = AmazonItem(),
  _anticipatoryProfit: Int = 0
    ) {

    val title: StringProperty = StringProperty(shopItem.title)
    val detailPage: StringProperty = StringProperty(shopItem.detailPage)
    val purchasePrice: ObjectProperty[Int] = ObjectProperty(shopItem.price)
    val stock: StringProperty = StringProperty(shopItem.availability)
    val amazonTitle: StringProperty = StringProperty(amazonItem.title)
    val newPrice: ObjectProperty[Int] = ObjectProperty(amazonItem.lowestNewPrice)
    val ranking: ObjectProperty[Int] = ObjectProperty(amazonItem.salesRank)
    val itemPage: StringProperty = StringProperty(amazonItem.detailPage)
    val newItemCount: ObjectProperty[Int] = ObjectProperty(amazonItem.totalNew)
    val anticipatoryProfit: ObjectProperty[Int] = ObjectProperty(_anticipatoryProfit)
    val shopJanCode: StringProperty = StringProperty(shopItem.janCode)
    val amazonJanCode: StringProperty = StringProperty(amazonItem.janCode)
    val shopIsbn: StringProperty = StringProperty(shopItem.isbnCode)
    val amazonIsbn: StringProperty = StringProperty(amazonItem.isbnCode)
}
