package ganesa.util

import java.io.File

import scala.annotation.tailrec
import scalax.io.Resource
import sbt._

object IOUtil {

  def writeText(file: String, text: String, append: Boolean = false) = {
    Using.fileWriter(IO.utf8, append)(new File(file)) { out => out.write(text) }
  }

  def download(url: String, dest: String = "data") = {
    val data = Resource.fromURL(url).byteArray
    url match {
      case Filename(file) => {
        Resource.fromFile(new java.io.File(dest, file)).write(data)
        Some(file)
      }
      case _ => None
    }
  }

  def fileSearch(file: File)(implicit f: File => Boolean = { file => true }) = {
    @tailrec
    def fileSearchInner(files: List[File], result: List[File]): List[File] = {
      if (files.forall(_.isFile)) {
        result ::: files.filter(f)
      } else {
        fileSearchInner(files.filter(_.isDirectory).flatMap(_.listFiles()), files.filter(_.isFile).filter(f) ::: result)
      }
    }
    fileSearchInner(List(file), Nil)
  }

  def detectSeparator(uri: String) = {
    if (PlatformUtil.isLinux) "/"
    else {
      if (uri.contains("/")) "/"
      else "\\"
    }
  }
}

object Filename {
  def unapply(uri: String): Option[String] = {
    if (uri.isEmpty) None
    else uri.split(IOUtil.detectSeparator(uri)).reverse.toList.headOption
  }
}

object Extension {
  def unapply(uri: String): Option[String] = {
    uri match {
      case Filename(name) => {
        if (name.contains(".")) {
          name.split("\\.").reverse.toList.headOption
        } else None
      }
      case _ => None
    }
  }
}

object FilenameWithoutExtension {
  def unapply(uri: String): Option[String] = {
    uri match {
      case Filename(name) => {
        if (name.contains(".")) {
          name.split("\\.").toList.headOption
        } else None
      }
      case _ => None
    }
  }
}

object Parent {
  def unapply(uri: String): Option[List[String]] = {
    uri.split(IOUtil.detectSeparator(uri)).toList match {
      case parent :+ file => {
        if (!parent.isEmpty) Some(parent)
        else None
      }
      case _ => None
    }
  }
}
