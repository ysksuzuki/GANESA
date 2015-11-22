package ganesa.application

import ganesa.model._

import scala.math.BigDecimal.RoundingMode

class CommissionCalculator(parameters: CommissionParameters) {

  def calculate(price: Int, productGroup: String, i: ItemDimensions): FBACommission = {
    val (shipping, weightCommission, weightExtra, deliveryCommissionDiv) =
      calculateDelivery(price, productGroup, i)
    new FBACommission(
      calculateSales(price, productGroup),
      calculateStorage(i),
      shipping,
      weightCommission,
      weightExtra,
      productGroup,
      deliveryCommissionDiv,
      i
    )
  }

  private def isExpensive(price: Int): Boolean = price >= parameters.expensivePriceThreshold

  private def calculateSales(price: Int, productGroup: String) = {
    val rate = parameters.salesCommissions.getOrElse(productGroup, parameters.salesCommissions.getOrElse("OTHER", 0))
    (BigDecimal(price) * (BigDecimal(rate) / BigDecimal("100"))).setScale(0, RoundingMode.HALF_UP).toInt
  }

  private def calculateStorage(i: ItemDimensions) = {
    val duration = 1
    (parameters.storageCostBase * ((i._length * i._width * i._height) / BigDecimal(1000)) * BigDecimal(duration))
      .setScale(0, RoundingMode.HALF_UP).toInt
  }

  private def calculateDelivery(price: Int, productGroup: String, i: ItemDimensions) = {
    val div = {
      if (isLarge(i)) {
        val total = i._length + i._width + i._height
        if (total < BigDecimal(100)) LargeDiv1
        else if (total < BigDecimal(140)) LargeDiv2
        else LargeDiv3
      }
      else if (isExpensive(price)) Expensive
      else if (isMedia(productGroup)) {
        if (isSmall(i)) MediaSmall
        else MediaMedium
      } else {
        if (isSmall(i)) NoMediaSmall
        else NoMediaMedium
      }
    }
    val commission = parameters.shippingAgencyCommissions.getOrElse(div.id, new ShippingAgencyCommission())
    (commission.shipping, commission.weight, calculateExtra(commission.weightExtra, i), div)
  }

  private def isMedia(productGroup: String): Boolean =  parameters.mediaGroup.contains(productGroup)
  private def isSmall(i: ItemDimensions): Boolean = {
    i < parameters.smallThreshold
  }
  private def isMedium(i: ItemDimensions): Boolean = {
    i >= parameters.smallThreshold && i < parameters.mediumThreshold
  }
  private def isLarge(i: ItemDimensions): Boolean = {
    i >= parameters.mediumThreshold
  }
  private def calculateExtra(weightExtra: Int, i: ItemDimensions): Int = {
    if (weightExtra == 0) 0
    else {
      val mediumPacking = 150
      if (isMedium(i)) {
        val totalWeight = ((i._weight + BigDecimal(mediumPacking)) / BigDecimal(1000)).setScale(0, RoundingMode.UP)
        ((totalWeight - BigDecimal(2)) * BigDecimal(weightExtra)).toInt
      } else 0
    }
  }
}
