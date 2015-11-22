package ganesa.model

import scala.xml.{Elem, XML}

import ganesa.util.StringUtil._

object CommissionParametersFactory {
    def apply(xmlPath: String = "") = {
        val resourcePath  = {
            if (xmlPath.isEmpty) "commission/amazonCommission.xml"
            else xmlPath
        }
        val resource = getClass.getClassLoader.getResource(resourcePath)
        val xml = XML.load(resource)
        new CommissionParameters(
            getSalesCommissions(xml),
            getStorageCostBase(xml),
            getMediaGroup(xml),
            getShippingAgencyCommissions(xml),
            getExpensivePriceThreshold(xml),
            getSizeThreshold(xml, "smallThreshold"),
            getSizeThreshold(xml, "mediumThreshold")
        )
    }

    private def getSalesCommissions(xml: Elem): Map[String, Int] = {
        (xml \\ "amazonCommission" \ "sales" \ "rate").map(rate => {
            rate \@ "category" -> rate.text.toIntOpt.getOrElse(0)
        }).toMap
    }

    private def getStorageCostBase(xml: Elem): BigDecimal = {
        BigDecimal((xml \\ "amazonCommission" \ "fba" \ "storageCostBase").text)
    }

    private def getMediaGroup(xml: Elem): Seq[String] = {
        (xml \\ "amazonCommission" \ "fba" \ "mediaCategory" \ "category").map(category => {
            category.text
        })
    }

    private def getShippingAgencyCommissions(xml: Elem): Map[String, ShippingAgencyCommission] = {
        (xml \\ "amazonCommission" \ "fba" \ "deliveryCommissions" \ "commission").map(commission => {
            commission \@ "id" -> new ShippingAgencyCommission(
                (commission \@ "shipping").toIntOpt.getOrElse(0),
                (commission \@ "weight").toIntOpt.getOrElse(0),
                (commission \@ "weightExtra").toIntOpt.getOrElse(0)
            )
        }).toMap
    }

    private def getExpensivePriceThreshold(xml: Elem): Int = {
        (xml \\ "amazonCommission" \ "fba" \ "shippingCommissions" \ "expensivePriceThreshold").text.toIntOpt.getOrElse(0)
    }

    private def getSizeThreshold(xml: Elem, threshold: String): ItemDimensions = {
        val smallThreshold = xml \\ "amazonCommission" \ "fba" \ "shippingCommissions" \ threshold
        new ItemDimensions(
            BigDecimal((smallThreshold \@ "length").toIntOpt.getOrElse(0)),
            BigDecimal((smallThreshold \@ "width").toIntOpt.getOrElse(0)),
            BigDecimal((smallThreshold \@ "height").toIntOpt.getOrElse(0)),
            BigDecimal((smallThreshold \@ "weight").toIntOpt.getOrElse(0))
        )
    }
}
case class CommissionParameters(
    salesCommissions: Map[String, Int],
    storageCostBase: BigDecimal,
    mediaGroup: Seq[String],
    shippingAgencyCommissions: Map[String, ShippingAgencyCommission],
    expensivePriceThreshold: Int,
    smallThreshold: ItemDimensions,
    mediumThreshold: ItemDimensions
)

case class ItemDimensions(length: BigDecimal = 0, width: BigDecimal = 0,
                          height: BigDecimal = 0, weight: BigDecimal = 0) {
    val defaultLength = BigDecimal(25)
    val defaultWidth = BigDecimal(18)
    val defaultHeight = BigDecimal(2)
    val defaultWeight = BigDecimal(250)

    val _length = if (length == 0) defaultLength else length
    val _width = if (width == 0) defaultWidth else width
    val _height = if (height == 0) defaultHeight else height
    val _weight = if (weight == 0) defaultWeight else weight

    def <=(i: ItemDimensions) = length <= i.length && width <= i.width && height <= i.height && weight <= i.weight
    def >=(i: ItemDimensions) = length >= i.length && width >= i.width && height >= i.height && weight >= i.weight
    def <(i: ItemDimensions) = length < i.length && width < i.width && height < i.height && weight < i.weight
    def >(i: ItemDimensions) = length > i.length && width > i.width && height > i.height && weight > i.weight

    override def toString(): String =
        s"${length}cm x ${width}cm x ${height}cm / ${weight}g"
}

case class ShippingAgencyCommission(shipping: Int = 0, weight: Int = 0, weightExtra: Int = 0)

case class FBACommission(
    sales: Int = 0,
    storage: Int = 0,
    shipping: Int = 0,
    weight: Int = 0,
    weightExtra: Int = 0,
    productGroup: String = "",
    deliveryCommission: AmazonDeliveryCommission = NoneCommission,
    itemDimensions: ItemDimensions = new ItemDimensions()
) {
    def total() = sales + storage + shipping + weight + weightExtra
}
