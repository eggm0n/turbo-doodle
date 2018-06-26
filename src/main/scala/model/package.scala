package object model {
  abstract sealed class Priority(val value: String)
  case object High extends Priority("high")
  case object Medium extends Priority("medium")
  case object Low extends Priority("low")

  object Priority {
    private def values = Set(High, Medium, Low)

    def unsafeFromString(value: String): Priority = {
      values.find(_.value == value).get
    }
  }

  case class Task(id: Option[Long], description: String, priority: Priority)

  case object TaskNotFoundError
}
