package ganesa.util

case class RetryException(throwables: List[Throwable]) extends Exception(throwables.toString())

object RetryUtil {

  import scala.util.control.Exception.allCatch

  def retry[T](retryLimit: Int)(f: => T): T =
    retry(retryLimit, 0, classOf[Throwable])(f)

  def retry[T](retryLimit: Int, retryInterval: Int)(f: => T): T =
    retry(retryLimit, retryInterval, classOf[Throwable])(f)

  def retry[T](retryLimit: Int, catchExceptionClasses: Class[_]*)(f: => T): T =
    retry(retryLimit, 0, e => catchExceptionClasses.exists(_.isAssignableFrom(e.getClass)))(f)

  def retry[T](retryLimit: Int, shouldCatch: Throwable => Boolean)(f: => T): T =
    retry(retryLimit, 0, shouldCatch)(f)

  def retry[T](retryLimit: Int, retryInterval: Int, catchExceptionClasses: Class[_]*)(f: => T): T =
    retry(retryLimit, retryInterval, e => catchExceptionClasses.exists(_.isAssignableFrom(e.getClass)))(f)

  def retry[T](retryLimit: Int, retryInterval: Int, shouldCatch: Throwable => Boolean)(f: => T): T = {
    @annotation.tailrec
    def retry0(errors: List[Throwable], f: => T): T = {
      allCatch.either(f) match {
        case Right(r) => r
        case Left(e) =>
          if (shouldCatch(e)) {
            if (errors.size < retryLimit - 1) {
              Thread.sleep(retryInterval)
              retry0(e :: errors, f)
            } else {
              throw RetryException(e :: errors)
            }
          } else throw e
      }
    }
    retry0(Nil, f)
  }

}
