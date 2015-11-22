package ganesa.model

case class SearchCriteria (
  val minPrice: Int,
  val maxPrice: Int,
  val availability: Boolean,
  val sort: Sort,
  val order: Order
)

case class SearchCriteriaAvailable (
  val minPrice: Boolean,
  val maxPrice: Boolean,
  val availability: Boolean,
  val sort: Boolean,
  val sortOption: Seq[Sort],
  val order: Boolean,
  val keywordSearch: Boolean
)
