package ganesa.model

object Shop {
  val table = Map(
    YahooShopping.name -> YahooShopping,
    RakutenIchiba.name -> RakutenIchiba,
    RakutenBooks.name -> RakutenBooks,
    Yodobashi.name -> Yodobashi,
    BicCamera.name -> BicCamera
  )
  def apply(name: String): Shop = table.getOrElse(name, YahooShopping)
}

sealed abstract class Shop(val name: String) {}
case object YahooShopping extends Shop("Yahoo!Shopping")
case object RakutenIchiba extends Shop("楽天市場")
case object RakutenBooks extends Shop("楽天ブックス")
case object Yodobashi extends Shop("ヨドバシ.com")
case object BicCamera extends Shop("ビックカメラ")

object AmazonApiEnum {
  val table = Map(
    ProductAdvertisingApi.name -> ProductAdvertisingApi,
    MWSApi.name -> MWSApi
  )
  def apply(name: String): AmazonApiEnum = table.getOrElse(name, ProductAdvertisingApi)
}

sealed abstract class AmazonApiEnum(val name: String) {}
case object ProductAdvertisingApi extends AmazonApiEnum("ProductAdvertisingApi")
case object MWSApi extends AmazonApiEnum("MWS")

object AmazonApiSearchType {
  val table = Map(
    EanCodeSearch.name -> EanCodeSearch,
    IsbnCodeSearch.name -> IsbnCodeSearch,
    KeywordSearch.name -> KeywordSearch
  )
  def apply(name: String): AmazonApiSearchType = table.getOrElse(name, EanCodeSearch)
}

sealed abstract class AmazonApiSearchType(val name: String) {}
case object EanCodeSearch extends AmazonApiSearchType("EAN")
case object IsbnCodeSearch extends AmazonApiSearchType("ISBN")
case object KeywordSearch extends AmazonApiSearchType("KEYWORD")

sealed abstract class Operation(val name: String) {}
case object Idle extends Operation("アイドル")
case object Search extends Operation("検索")
case object CsvWrite extends Operation("CSV出力")
case object CsvRead extends Operation("CSV読込")

object Order {
  val table = Map(
    Ascending.name -> Ascending,
    Descending.name -> Descending
  )
  def apply(name: String): Order = table.getOrElse(name, Ascending)
}

sealed abstract class Order(val name: String, val id: String) {}
case object Ascending extends Order("昇順", "+")
case object Descending extends Order("降順", "-")

object Sort {
  val table = Map(
    YSSortPrice.name -> YSSortPrice,
    YSSortName.name -> YSSortName,
    YSSortScore.name -> YSSortScore,
    YSSortSold.name -> YSSortSold,
    YSSortAffiliate.name -> YSSortAffiliate,
    YSSortReviewCount.name -> YSSortReviewCount,
    RISortStandard.name -> RISortStandard,
    RISortReleaseDate.name -> RISortReleaseDate,
    RISortSeller.name -> RISortSeller,
    RISortSatisfied.name -> RISortSatisfied,
    RBSortStandard.name -> RBSortStandard,
    RBSortSales.name -> RBSortSales,
    RBSortReleaseDateOld.name -> RBSortReleaseDateOld,
    RBSortReleaseDateNew.name -> RBSortReleaseDateNew,
    RBSortItemPriceCheep.name -> RBSortItemPriceCheep,
    RBSortItemPriceExpensive.name -> RBSortItemPriceExpensive,
    RBSortReviewCount.name -> RBSortReviewCount,
    RBSortReviewAverage.name -> RBSortReviewAverage,
    BicSortStandard.name -> BicSortStandard,
    BicSortNew.name -> BicSortNew,
    BicSortCheap.name -> BicSortCheap,
    BicSortExpensive.name -> BicSortExpensive,
    BicSortReleaseDate.name -> BicSortReleaseDate,
    BicSortDeliveryDate.name -> BicSortDeliveryDate,
    BicSortReviewCount.name -> BicSortReviewCount,
    BicSortReviewPoint.name -> BicSortReviewPoint
  )
  def apply(name: String): Sort = table.getOrElse(name, YSSortScore)
}

sealed abstract class Sort(val name: String, val id: String)
// YahooShopping
case object YSSortPrice extends Sort("商品価格", "price")
case object YSSortName extends Sort("ストア名", "name")
case object YSSortScore extends Sort("おすすめ順", "score")
case object YSSortSold extends Sort("売れ筋順", "sold")
case object YSSortAffiliate extends Sort("アフィリエイト料率順", "affiliate")
case object YSSortReviewCount extends Sort("レビュー数順", "review_count")
// Rakuten Ichiba
case object RISortStandard extends Sort("楽天標準ソート順", "standard")
case object RISortReleaseDate extends Sort("発売日順(降順)", "-releaseDate")
case object RISortSeller extends Sort("売上順(降順)", "-seller")
case object RISortSatisfied extends Sort("満足順(降順)", "-satisfied")
// Rakuten Books
case object RBSortStandard extends Sort("標準", "standard")
case object RBSortSales extends Sort("売れている", "sales")
case object RBSortReleaseDateOld extends Sort("発売日(古い)", "+releaseDate")
case object RBSortReleaseDateNew extends Sort("発売日(新しい)", "-releaseDate")
case object RBSortItemPriceCheep extends Sort("価格が安い", "+itemPrice")
case object RBSortItemPriceExpensive extends Sort("価格が高い", "-itemPrice")
case object RBSortReviewCount extends Sort("レビューの件数が多い", "reviewCount")
case object RBSortReviewAverage extends Sort("レビューの評価(平均)が多い", "reviewAverage")
// Biccamera
case object BicSortStandard extends Sort("標準", "00")
case object BicSortNew extends Sort("新着順", "01")
case object BicSortCheap extends Sort("価格が安い", "02")
case object BicSortExpensive extends Sort("価格が高い", "03")
case object BicSortReleaseDate extends Sort("発売日が新しい順", "04")
case object BicSortDeliveryDate extends Sort("納期が早い順", "05")
case object BicSortReviewCount extends Sort("レビューの多い順", "08")
case object BicSortReviewPoint extends Sort("レビューの評価順", "09")
// Yodobashi
case object YodobashiCoincidenceRanking extends Sort("人気順", "COINCIDENCE_RANKING")
case object YodobashiNewArrivalRanking extends Sort("新着順", "NEW_ARRIVAL_RANKING")
case object YodobashiReleaseDateDesc extends Sort("発売日(新)順", "RELEASE_DATE_DESC")
case object YodobashiSellPriceDesc extends Sort("価格(高)順", "SELL_PRICE_DESC")
case object YodobashiSellPriceAsc extends Sort("価格(安)順", "SELL_PRICE_ASC")


object RakutenBooksAvailability {
  val table = Map(
    Available.id -> Available,
    ThreeToSeven.id -> ThreeToSeven,
    ThreeToNine.id -> ThreeToNine,
    OrderFromMakers.id -> OrderFromMakers,
    Reservation.id -> Reservation,
    ConfirmToMakers.id -> ConfirmToMakers
  )
  def apply(id: String): RakutenBooksAvailability = table.getOrElse(id, Available)
}

sealed abstract class RakutenBooksAvailability(val id: String, val name: String)
case object Available extends RakutenBooksAvailability("1", "在庫あり")
case object ThreeToSeven extends RakutenBooksAvailability("2", "通常3〜7日程度で発送")
case object ThreeToNine extends RakutenBooksAvailability("3", "通常3〜9日程度で発送")
case object OrderFromMakers extends RakutenBooksAvailability("4", "メーカー取り寄せ")
case object Reservation extends RakutenBooksAvailability("5", "予約受付中")
case object ConfirmToMakers extends RakutenBooksAvailability("6", "メーカーに在庫確認")


object AmazonDeliveryCommission {
  val table = Map(
    MediaSmall.name -> MediaSmall,
    MediaMedium.name -> MediaMedium,
    NoMediaSmall.name -> NoMediaSmall,
    NoMediaMedium.name -> NoMediaMedium,
    LargeDiv1.name -> LargeDiv1,
    LargeDiv2.name -> LargeDiv2,
    LargeDiv3.name -> LargeDiv3,
    Expensive.name -> Expensive,
    NoneCommission.name -> NoneCommission
  )
  def apply(name: String): AmazonDeliveryCommission = table.getOrElse(name, NoneCommission)
}

sealed abstract class AmazonDeliveryCommission(val id: String, val name: String)
case object MediaSmall extends AmazonDeliveryCommission("media.small", "メディア小型")
case object MediaMedium extends AmazonDeliveryCommission("media.medium", "メディア標準")
case object NoMediaSmall extends AmazonDeliveryCommission("noMedia.small", "メディア以外小型")
case object NoMediaMedium extends AmazonDeliveryCommission("noMedia.medium", "メディア以外標準")
case object LargeDiv1 extends AmazonDeliveryCommission("large.div1", "大型区分1")
case object LargeDiv2 extends AmazonDeliveryCommission("large.div2", "大型区分2")
case object LargeDiv3 extends AmazonDeliveryCommission("large.div3", "大型区分2")
case object Expensive extends AmazonDeliveryCommission("expensive", "高額商品")
case object NoneCommission extends AmazonDeliveryCommission("", "")
