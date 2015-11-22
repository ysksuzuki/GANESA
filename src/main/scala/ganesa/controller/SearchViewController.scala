package ganesa.controller

import java.awt.Desktop
import java.io.File
import java.lang
import java.net.URI
import java.util.concurrent.atomic.{AtomicReference}
import java.util.function.Predicate
import javafx.application.Platform
import javafx.beans.value.{ObservableValue, ChangeListener}
import javafx.collections.transformation.{SortedList, FilteredList}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{TreeItem, CheckBoxTreeItem}
import javafx.{concurrent => jfxc}

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import com.typesafe.config.Config

import ganesa.api.amazon.{AmazonApi}
import ganesa.api.{ItemSearchApi, Const, ShopApi}
import ganesa.application.{CommissionCalculator, ItemCsvHandler}
import ganesa.model._
import ganesa.util.{HttpUtil, Formatter, ConfigUtil}
import ganesa.util.StringUtil._

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.Service
import scalafx.scene.control.cell.CheckBoxTreeCell
import scalafx.scene.control._
import scalafx.util.StringConverter
import scalafxml.core.macros.sfxml
import scalafx.collections.transformation._
import scala.util.control.Exception._

trait SearchViewControllerInterface {
  def csvWrite(file: File, encode: String)
  def csvRead(file: File, encode: String)
}

@sfxml
class SearchViewController(
  private val categoryTreeView: TreeView[ShopCategory],
  private val shopComboBox: ComboBox[String],
  private val progressLabel: Label,
  private val progressBar: ProgressBar,
  private val searchButton: Button,
  private val cancelButton: Button,
  // Table view
  private val itemTable: TableView[Item],
  private val titleColumn: TableColumn[Item, String],
  private val detailPageColumn: TableColumn[Item, String],
  private val purchasePriceColumn: TableColumn[Item, Int],
  private val stockColumn: TableColumn[Item, String],
  private val amazonTitleColumn: TableColumn[Item, String],
  private val newPriceColumn: TableColumn[Item, Int],
  private val rankingColumn: TableColumn[Item, Int],
  private val itemPageColumn: TableColumn[Item, String],
  private val newItemCountColumn: TableColumn[Item, Int],
  private val anticipatoryProfitColumn: TableColumn[Item, Int],
  // Detail view
  private val netShopLabel: Label,
  private val titleLabel: Label,
  private val detailPageLink: Hyperlink,
  private val purchasePriceLabel: Label,
  private val stockLabel: Label,
  private val amazonTitleLabel: Hyperlink,
  private val newPriceLink: Hyperlink,
  private val itemPageLink: Hyperlink,
  private val newItemCountLabel: Label,
  private val shopJanLabel: Label,
  private val amazonJanLabel: Label,
  private val shopIsbnLabel: Label,
  private val amazonIsbnLabel: Label,
  // Search criteria
  private val minPrice: TextField,
  private val minPriceSpecify: CheckBox,
  private val maxPrice: TextField,
  private val maxPriceSpecify: CheckBox,
  private val availabilityTrue: RadioButton,
  private val availabilityFalse: RadioButton,
  private val sort: ComboBox[Sort],
  private val ascending: RadioButton,
  private val descending: RadioButton,
  private val keywordSearch: CheckBox,
  // Search result filter
  private val keywordFilter: TextField,
  private val keywordNotFilter: CheckBox,
  private val minProfitFilter: TextField,
  private val maxProfitFilter: TextField,
  private val minPriceFilter: TextField,
  private val maxPriceFilter: TextField,
  private val minRankingFilter: TextField,
  private val maxRankingFilter: TextField,
  private val failedFilter: CheckBox,
  private val nullRankingFilter: CheckBox

) extends SearchViewControllerInterface {
  private val EMPTY_LABEL = "-"
  private val logger: Logger = Logger(LoggerFactory.getLogger(SearchViewController.this.getClass))
  private val itemData: ObservableList[Item] = FXCollections.observableArrayList()
  private val filteredData = new FilteredList[Item](itemData, new Predicate[Item] {
    override def test(t: Item): Boolean = true
  })
  private val sortedData = {
    val sortedData = new SortedList(filteredData)
    sortedData.comparatorProperty().bind(itemTable.comparatorProperty())
    sortedData
  }
  private val sortedBuffer = new SortedBuffer[Item](sortedData)
  private val operation = new AtomicReference[Operation](Idle)
  private var csvFile: Option[File] = Option.empty
  private var csvEncode: Option[String] = Option.empty

  initialize()
  def initialize(): Unit = {
    initializeTopArea()
    initializeSearchCriteriaAtOnce()
    initializeSearchResultFilterAtOnce()
    shopComboBox.getSelectionModel.selectedItemProperty().addListener {
      (o: javafx.beans.value.ObservableValue[_ <: String], oldVal: String, newVal: String) => {
        val config = ConfigUtil.load()
        val itemSearchApi =
          loadItemSearchApi(config, new CommissionCalculator(CommissionParametersFactory()), Shop(newVal))
        loadTreeItems(itemSearchApi)
        initializeSearchCriteria(itemSearchApi)
        initializeItemTable()
        initializeDetailView(newVal)
      }
    }
    shopComboBox.getSelectionModel().select(0)
  }

  private def initializeTopArea() = {
    Const.shops.foreach(shopComboBox += _)
    progressLabel.text_=("")
    progressBar.visible_=(false)
    cancelButton.visible_=(false)

    progressLabel.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        Option(newValue).foreach(text => {
          if (text.startsWith("[Success]") || text.startsWith("[Failed]")) {
            progressBar.visible_=(false)
            cancelButton.visible_=(false)
            searchButton.disable_=(false)
            initializeSearchResultFilter(false)
          }
        })
      }
    })
  }

  private def initializeSearchCriteriaAtOnce() = {
    sort.converter_=(new StringConverter[Sort] {
      override def fromString(string: String): Sort = Sort(string)
      override def toString(t: Sort): String = t.name
    })
    sort.cellFactory = {_ =>
      new ListCell[Sort] {
        item.onChange {(_, _, newSort) =>
          text = {
            if (newSort == null) ""
            else newSort.name
          }
        }
      }
    }
    minPriceSpecify.selectedProperty().addListener(new ChangeListener[lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = {
        Option(newValue).foreach(selected => {
          minPrice.disable_=(!selected)
        })
      }
    })
    maxPriceSpecify.selectedProperty().addListener(new ChangeListener[lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = {
        Option(newValue).foreach(selected => {
          maxPrice.disable_=(!selected)
        })
      }
    })
  }

  private def initializeSearchCriteria(itemSearchApi: ItemSearchApi): Unit = {
    val searchCriteriaAvailable = itemSearchApi.getSearchCriteriaAvailable()
    // Item price criteria
    minPrice.disable_=(true)
    maxPrice.disable_=(true)
    minPriceSpecify.selected_=(false)
    minPriceSpecify.disable_=(!searchCriteriaAvailable.minPrice)
    maxPriceSpecify.selected_=(false)
    maxPriceSpecify.disable_=(!searchCriteriaAvailable.maxPrice)
    // sort criteria
    sort.disable_=(!searchCriteriaAvailable.sort)
    sort.getItems.clear()
    searchCriteriaAvailable.sortOption.foreach(sort += _)
    sort.getSelectionModel.select(0)
    descending.disable_=(!searchCriteriaAvailable.order)
    ascending.disable_=(!searchCriteriaAvailable.order)
    // Availability criteria
    availabilityTrue.disable_=(!searchCriteriaAvailable.availability)
    availabilityFalse.disable_=(!searchCriteriaAvailable.availability)
  }

  private def filterItem(item: Item, keyword: String, minProfit: String, maxProfit: String,
                         minRanking: String, maxRanking: String, minPrice: String,
                         maxPrice: String, failedCheck: Boolean, nullRankingCheck: Boolean,
                         keywordNotCheck: Boolean): Boolean = {

    def compareInt(value: String, f: Int => Boolean): Boolean = {
      Option(value).map(s => {
        if (!s.isNumeric) true
        else {
          val target = s.toIntOpt.getOrElse(0)
          f(target)
        }
      }).getOrElse(true)
    }
    val keywordFilter = Option(keyword).map(s => {
      if (s.isEmpty) true
      else {
        if (keywordNotCheck) {
          !item.title.value.contains(keyword) && !item.amazonTitle.value.contains(keyword)
        } else {
          item.title.value.contains(keyword) || item.amazonTitle.value.contains(keyword)
        }
      }
    }).getOrElse(true)

    val minProfitFilter = compareInt(minProfit, n => item.anticipatoryProfit.value >= n)
    val maxProfitFilter = compareInt(maxProfit, n => item.anticipatoryProfit.value <= n)
    val minPriceFilter = compareInt(minPrice, n => item.purchasePrice.value >= n)
    val maxPriceFilter = compareInt(maxPrice, n => item.purchasePrice.value <= n)
    val minRankingFilter = compareInt(minRanking, n => n != 0 && item.ranking.value >= n)
    val maxRankingFilter = compareInt(maxRanking, n => n != 0 && item.ranking.value <= n)

    val failedFilter = {
      if (failedCheck) !item.amazonTitle.value.isEmpty
      else true
    }

    val nullRankingFilter = {
      if (nullRankingCheck) item.ranking.value != 0
      else true
    }

    keywordFilter && minProfitFilter && maxProfitFilter && minRankingFilter && maxRankingFilter && minPriceFilter && maxPriceFilter && failedFilter && nullRankingFilter
  }
  private def initializeSearchResultFilterAtOnce(): Unit = {
    keywordFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, newValue, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    keywordNotFilter.selectedProperty().addListener(new ChangeListener[lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              newValue, nullRankingFilter.selected.value, newValue)
          }
        })
      }
    })

    minProfitFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, newValue, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    maxProfitFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, newValue,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    minRankingFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              newValue, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    maxRankingFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, newValue,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    minPriceFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              newValue, maxPriceFilter.text.value,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    maxPriceFilter.text.addListener(new ChangeListener[String] {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, newValue,
              failedFilter.selected.value, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    failedFilter.selectedProperty().addListener(new ChangeListener[lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              newValue, nullRankingFilter.selected.value, keywordNotFilter.selected.value)
          }
        })
      }
    })

    nullRankingFilter.selectedProperty().addListener(new ChangeListener[lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = {
        filteredData.setPredicate(new Predicate[Item] {
          override def test(t: Item): Boolean = {
            filterItem(t, keywordFilter.text.value, minProfitFilter.text.value, maxProfitFilter.text.value,
              minRankingFilter.text.value, maxRankingFilter.text.value,
              minPriceFilter.text.value, maxPriceFilter.text.value,
              failedFilter.selected.value, newValue, keywordNotFilter.selected.value)
          }
        })
      }
    })
  }

  private def initializeSearchResultFilter(disable: Boolean): Unit = {
    Seq(keywordFilter, minProfitFilter, maxProfitFilter, minRankingFilter,  maxRankingFilter,
        minPriceFilter, maxPriceFilter)
      .foreach(textField => {
      textField.text_=("")
      textField.disable_=(disable)
    })
    Seq(failedFilter, nullRankingFilter, keywordNotFilter).foreach(checkBox => {
      checkBox.selected_=(false)
      checkBox.disable_=(disable)
    })
  }

  private def initializeItemTable(): Unit = {
    def numberCellFactory(column: TableColumn[Item, Int]): TableCell[Item, Int] = {
      new TableCell[Item, Int] {
        item.onChange { (_, _, newNumber) =>
          text = {
            if (newNumber == 0) EMPTY_LABEL
            else Formatter.formatNumber(newNumber)
          }
        }
      }
    }
    titleColumn.cellValueFactory = {_.value.title}
    detailPageColumn.cellValueFactory = {_.value.detailPage}
    purchasePriceColumn.cellValueFactory = {_.value.purchasePrice}
    purchasePriceColumn.cellFactory = numberCellFactory
    stockColumn.cellValueFactory = {_.value.stock}
    amazonTitleColumn.cellValueFactory = {_.value.amazonTitle}
    newPriceColumn.cellValueFactory = {_.value.newPrice}
    newPriceColumn.cellFactory = numberCellFactory
    rankingColumn.cellValueFactory = {_.value.ranking}
    rankingColumn.cellFactory = numberCellFactory
    itemPageColumn.cellValueFactory = {_.value.itemPage}
    newItemCountColumn.cellValueFactory = {_.value.newItemCount}
    anticipatoryProfitColumn.cellValueFactory = {_.value.anticipatoryProfit}
    anticipatoryProfitColumn.cellFactory = numberCellFactory

    itemTable.getSelectionModel.selectedItemProperty().addListener {
      (o: javafx.beans.value.ObservableValue[_ <: Item], oldVal: Item, newVal: Item) =>
        showItemDetail(shopComboBox.getSelectionModel.getSelectedItem, Option(newVal))
    }
    itemTable.setItems(sortedBuffer)
    itemData.clear()
  }

  private def initializeDetailView(netShop: String) = {
    showItemDetail(netShop, Option(new Item))
    def loadRemoteContents(url: String): Unit = {
      if (!url.isEmpty && url != EMPTY_LABEL) {
        val desktop: Desktop = Desktop.getDesktop
        desktop.browse(new URI(url))
      }
    }
    detailPageLink.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = loadRemoteContents(detailPageLink.text.value)
    })
    itemPageLink.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = loadRemoteContents(itemPageLink.text.value)
    })
    amazonTitleLabel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val amazonTitle = amazonTitleLabel.text.value
        if (amazonTitle != EMPTY_LABEL) {
          val endPoint = "http://mnrate.com/search"
          val kwd = {
            val jan = amazonJanLabel.text.value
            if (!jan.isEmpty) jan
            else amazonTitle
          }
          val url = HttpUtil.makeQuery(endPoint, Map("i" -> "All", "kwd" -> kwd))
          loadRemoteContents(url)
        }
      }
    })
    newPriceLink.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val commission = newPriceLink.userData.asInstanceOf[FBACommission]
        val alert = new Alert(AlertType.INFORMATION)
        alert.setTitle("Commission")
        alert.setHeaderText("手数料詳細")
        val detail =
          """ProductGroup: %s
            |サイズ / 重量: %s
            |FBA配送代行手数料区分: %s
            |
            |販売手数料: %s 円
            |月間在庫保管手数料: %s 円
            |出荷作業手数料: %s 円
            |発送重量手数料: %s 円
            |合計: %s 円
            |
            |※サイズ/重量の情報が取得できなかった場合、
            |25cm x 18cm x 2cm / 250g(標準サイズ) として計算しています。
          """.stripMargin.format(
              commission.productGroup,
              commission.itemDimensions.toString(),
              commission.deliveryCommission.name,
              Formatter.formatNumber(commission.sales),
              Formatter.formatNumber(commission.storage),
              Formatter.formatNumber(commission.shipping),
              Formatter.formatNumber(commission.weight + commission.weightExtra),
              Formatter.formatNumber(commission.total())
            )

        alert.setContentText(detail)
        alert.showAndWait()
      }
    })
  }

  private def showItemDetail(netShop: String, itemOpt: Option[Item]): Unit = {
    itemOpt.foreach(item => {
      netShopLabel.text_=(netShop)
      titleLabel.text_=(Formatter.formatText(item.title.value))
      detailPageLink.text_=(Formatter.formatText(item.detailPage.value))
      purchasePriceLabel.text_=(Formatter.formatNumber(item.purchasePrice.value))
      stockLabel.text_=(Formatter.formatText(item.stock.value))
      amazonTitleLabel.text_=(Formatter.formatText(item.amazonTitle.value))
      itemPageLink.text_=(Formatter.formatText(item.itemPage.value))
      newPriceLink
        .text_=(s"${Formatter.formatNumber(item.newPrice.value)} / ${Formatter.formatNumber(item.amazonItem.commission.total())}")
      newPriceLink.userData_=(item.amazonItem.commission)
      newItemCountLabel.text_=(Formatter.formatNumber(item.newItemCount.value))
      shopJanLabel.text_=(Formatter.formatText(item.shopJanCode.value))
      amazonJanLabel.text_=(Formatter.formatText(item.amazonJanCode.value))
      shopIsbnLabel.text_=(Formatter.formatText(item.shopIsbn.value))
      amazonIsbnLabel.text_=(Formatter.formatText(item.amazonIsbn.value))
    })
  }

  def loadTreeItems(itemSearchApi: ItemSearchApi): Unit = {
    val category = itemSearchApi.getCategories()
    val root = shopCategoryToTreeItem(category)
    root.setExpanded(true)
    categoryTreeView.setRoot(root)
    categoryTreeView.cellFactory = CheckBoxTreeCell.forTreeView
  }

  private def shopCategoryToTreeItem(category: ShopCategory): TreeItem[ShopCategory] = {
    def shopCategoryToTreeItemInner(category: ShopCategory, treeItem: CheckBoxTreeItem[ShopCategory]): Unit = {
      val node = new CheckBoxTreeItem[ShopCategory](category)
      treeItem.getChildren.add(node)
      if (!category.children.isEmpty) {
        category.children.foreach(shopCategoryToTreeItemInner(_, node))
      }
    }
    val root = new CheckBoxTreeItem[ShopCategory](category)
    shopCategoryToTreeItemInner(category, root)
    root.getChildren.get(0)
  }

  private var itemSearchApiUsing: Option[ItemSearchApi] = Option.empty

  private def getSearchCriteria(): SearchCriteria = {
    new SearchCriteria(
      if (minPriceSpecify.selected.value) minPrice.text.value.toIntOpt.getOrElse(0)
      else 0,
      if (maxPriceSpecify.selected.value) maxPrice.text.value.toIntOpt.getOrElse(0)
      else 0,
      availabilityTrue.selected.value,
      sort.getSelectionModel.getSelectedItem,
      if (descending.selected.value) Descending
      else Ascending)
  }

  object SearchWorker extends Service(new jfxc.Service[Seq[Item]]() {
    protected def createTask(): jfxc.Task[Seq[Item]] = {
      val config = ConfigUtil.load()
      val itemSearchApi = loadItemSearchApi(config, new CommissionCalculator(parameters = CommissionParametersFactory()),
        Shop(shopComboBox.getSelectionModel.getSelectedItem))
      itemSearchApiUsing = Option(itemSearchApi)
      new jfxc.Task[Seq[Item]] {
        protected def call(): Seq[Item] = {
          allCatch withApply(t => {
            logger.error(t.getMessage, t)
            updateMessage("[Failed] System Error.")
            Seq()
          }) andFinally {
            operation.set(Idle)
            itemSearchApiUsing = Option.empty
          } apply {
            updateMessage("Calculating...")
            val root: TreeItem[ShopCategory] = categoryTreeView.root.value
            val shopCategories = collectedCheckedNode(new ObservableBuffer(root.getChildren), List())
            if (shopCategories.isEmpty) {
              updateMessage("[Failed] Please select at least one category.")
              Seq()
            } else {
              val items =
                itemSearchApi.getItems(shopCategories, getSearchCriteria(),
                { (workDone: Long, max: Long) => updateProgress(workDone, max) },
                { (items: Seq[Item]) => {
                    Platform.runLater(new Runnable {
                      override def run(): Unit = {
                        itemData.insertAll(itemData.length, items)
                      }
                    })
                  }
                }, { (message: String) => updateMessage(message) })
              printStatistics(items)
              updateMessage(s"[Success] Total:${items.length} items Success:${items.count(!_.amazonTitle.value.isEmpty)} items")
              items
            }
          }
        }
      }
    }
  })

  def search(): Unit = {
    if (operation.compareAndSet(Idle, Search)) {
      searchButton.disable_=(true)
      progressBar.visible_=(true)
      cancelButton.visible_=(true)
      cancelButton.disable_=(false)
      initializeSearchResultFilter(true)
      itemData.clear()
      itemTable.sortOrder.clear()
      progressLabel.text.bind(SearchWorker.message)
      progressBar.progressProperty().bind(SearchWorker.progress)
      SearchWorker.restart()
    }
  }

  def cancel() = {
    cancelButton.disable_=(true)
    itemSearchApiUsing.foreach(api => api.cancel())
  }

  private def loadItemSearchApi(config: Config, commissionCalculator: CommissionCalculator, shop: Shop) = new ItemSearchApi(
    AmazonApi(
      AmazonApiEnum("ProductAdvertisingApi"),
      config,
      commissionCalculator,
      keywordSearch.selected.value
    ),
    ShopApi(shop, config)
  )
  private def printStatistics(items: Seq[Item]) = {
    logger.info(s"total=${items.length} jan-empty=${items.count(_.shopJanCode.value.isEmpty)} isbn-empty=${items.count(_.shopIsbn.value.isEmpty)}")
  }

  private def collectedCheckedNode(items: ObservableBuffer[TreeItem[ShopCategory]], result: List[ShopCategory]): List[ShopCategory] = {
    if (items.forall(_.getChildren.isEmpty)) {
      result ::: items.filter(_.asInstanceOf[CheckBoxTreeItem[ShopCategory]].isSelected).toList.map(_.getValue)
    } else {
      collectedCheckedNode(items.filterNot(_.getChildren.isEmpty).flatMap(n => new ObservableBuffer(n.getChildren)),
        items.filter(n => n.getChildren.isEmpty && n.asInstanceOf[CheckBoxTreeItem[ShopCategory]].isSelected).toList.map(_.getValue) ::: result)
    }
  }

  object CsvReadWorker extends Service(new jfxc.Service[Seq[Item]]() {
    protected def createTask(): jfxc.Task[Seq[Item]] = {
      new jfxc.Task[Seq[Item]] {
        protected def call(): Seq[Item] = {
          allCatch withApply {t => {
            logger.error(t.getMessage, t)
            updateMessage(s"[Failed]")
            Seq()
          }} andFinally {
            operation.set(Idle)
            csvFile = Option.empty
            csvEncode = Option.empty
          } apply {
            updateMessage("Reading...")
            val items = ItemCsvHandler.read(csvFile.get, csvEncode.get)
            itemData.clear()
            itemData.insertAll(0, items)
            updateMessage(s"[Success]")
            Seq()
          }
        }
      }
    }
  })

  override def csvRead(file: File, encode: String) = {
    if (operation.compareAndSet(Idle, CsvRead)) {
      operation.set(CsvWrite)
      csvFile = Option(file)
      csvEncode = Option(encode)
      progressLabel.text.bind(CsvReadWorker.message)
      progressBar.progressProperty().bind(CsvReadWorker.progress)
      CsvReadWorker.restart()
    }
  }

  object CsvWriteWorker extends Service(new jfxc.Service[Seq[Item]]() {
    protected def createTask(): jfxc.Task[Seq[Item]] = {
      new jfxc.Task[Seq[Item]] {
        protected def call(): Seq[Item] = {
          allCatch withApply {t => {
            logger.error(t.getMessage, t)
            updateMessage(s"[Failed]")
            Seq()
          }} andFinally {
            operation.set(Idle)
            csvFile = Option.empty
            csvEncode = Option.empty
          } apply {
            updateMessage("Writing...")
            ItemCsvHandler.write(itemData.toSeq, csvFile.get, csvEncode.get)
            updateMessage(s"[Success]")
            Seq()
          }
        }
      }
    }
  })

  override def csvWrite(file: File, encode: String) = {
    if (operation.compareAndSet(Idle, CsvWrite)) {
      csvFile = Option(file)
      csvEncode = Option(encode)
      progressLabel.text.bind(CsvWriteWorker.message)
      progressBar.progressProperty().bind(CsvWriteWorker.progress)
      CsvWriteWorker.restart()
    }
  }

}

