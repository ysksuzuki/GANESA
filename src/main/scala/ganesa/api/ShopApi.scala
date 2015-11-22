package ganesa.api

import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.config.Config
import ganesa.api.biccamera.BicCameraApi
import ganesa.api.rakuten.{RakutenIchibaApi, RakutenBooksApi}
import ganesa.api.yahoo.YahooShoppingApi
import ganesa.api.yodobashi.YodobashiApi
import ganesa.model._
import ganesa.util.IOUtil
import scala.util.Properties
import scala.xml.{NodeSeq, XML}
import scala.collection.JavaConversions._

object ShopApi {
  def apply(shop: Shop, config: Config): ShopApi = {
    shop match {
      case YahooShopping => new YahooShoppingApi(config)
      case RakutenBooks => new RakutenBooksApi(config)
      case RakutenIchiba => new RakutenIchibaApi(config)
      case BicCamera => new BicCameraApi(config)
      case Yodobashi => new YodobashiApi(config)
    }
  }
}

trait ShopApi {
  val config: Config
  val buddhaFace = config.getInt("settings.general.buddhaFace")
  val interval = config.getInt("settings.general.interval")
  val connectTimeout = config.getInt("settings.general.connectTimeout")
  val readTimeout = config.getInt("settings.general.readTimeout")
  val cancel = new AtomicBoolean(false)
  def getCategories(params: Map[String, String] = Map()): ShopCategory
  def getCategoriesFromShop(categoryId: String, params: Map[String, String] = Map(), depth: Int = 1): Seq[ShopCategory]
  def getItems(category: ShopCategory, searchCriteria: SearchCriteria, params: Map[String, String] = Map(),
               f: Seq[ShopItem] => Seq[Item]): Seq[Item]
  def getAllItemAndTaskCount(categories: Seq[ShopCategory], searchCriteria: SearchCriteria,
                             params: Map[String, String] = Map()): (Int, Int)
  def getSearchCriteriaAvailable(): SearchCriteriaAvailable

  protected def filter(items: Seq[ShopItem]): Seq[ShopItem] = {
    val excludeWords = config.getStringList("settings.general.excludeWords")
    items.filter(i => excludeWords.forall(w => !i.title.toUpperCase.contains(w.toUpperCase())))
  }

  protected def getCategoriesFromXml(file: String): ShopCategory = {
    def sub(node: NodeSeq): Seq[ShopCategory] = {
      val categories = node \ "category"
      if (categories.isEmpty) Seq()
      else {
        categories.map(n => {
          new ShopCategory(
            n \@ "id",
            n \@ "title",
            n \@ "sortOrder",
            n \@ "amazonCategory",
            n \@ "criteria",
            sub(n))
        })
      }
    }
    val resource = getClass.getClassLoader().getResource(file)
    val xml = XML.load(resource)
    val root = xml \\ "root"
    new ShopCategory(root \@ "id", root \@ "title", "", "", "", sub(xml \\ "root"))
  }

  val file = "src/main/resources/category/rakutenIchiba.xml"
  def printCategoriesInternal(root: (String, String, String, String), categories: Seq[(String, String, String)]) = {
    val (id, title, sortOrder, amazonCategory) = root
    def printCategoriesInnerSub(categories: ShopCategory) = {
      def sub(categories: ShopCategory, indent: String): Unit = {
        val tag = indent + "<category id=\"%s\" title=\"%s\" sortOrder=\"%s\" amazonCategory=\"%s\" "
        if (!categories.children.isEmpty) {
          //println((tag + ">").format(categories.id, categories.title, categories.sortOrder, amazonCategory))
          IOUtil.writeText(
            file, (tag + s">${Properties.lineSeparator}").format(categories.id, categories.title, categories.sortOrder, amazonCategory), true)
          categories.children.foreach(sub(_, indent + "    "))
          //println(indent + "</category>")
          IOUtil.writeText(file, indent + s"</category>${Properties.lineSeparator}", true)
        }else{
          //println((tag + "/>").format(categories.id, categories.title, categories.sortOrder, amazonCategory))
          IOUtil.writeText(
            file, (tag + s"/>${Properties.lineSeparator}").format(categories.id, categories.title, categories.sortOrder, amazonCategory), true)
        }
      }
      sub(categories, "        ")
    }
    //println("    " + "<category id=\"%s\" title=\"%s\" sortOrder=\"%s\" amazonCategory=\"%s\" >".format(id, title, sortOrder, amazonCategory))
    IOUtil.writeText(
      file, "    " + "<category id=\"%s\" title=\"%s\" sortOrder=\"%s\" amazonCategory=\"%s\" >%s"
        .format(id, title, sortOrder, amazonCategory, Properties.lineSeparator), true)
    categories.foreach(category => {
      val (id, title, sortOrder) = category
      val children = getCategoriesFromShop(id)
      printCategoriesInnerSub(new ShopCategory(id, title, sortOrder, "", "", children))
    })
    //println("    " + "</category>")
    IOUtil.writeText(file, "    " + s"</category>${Properties.lineSeparator}", true)
  }

  def printCategories(): Unit

}


