package ganesa.model

trait Configuration
case class OperationSettings(
  buddhaFace: Int,
  interval: Int,
  connectTimeout: Int,
  readTimeout: Int,
  excludeWords: Seq[String]) extends Configuration
case class Authentication(
  awsAccessKeyId: String,
  awsSecretKey: String,
  associateTag: String) extends Configuration
