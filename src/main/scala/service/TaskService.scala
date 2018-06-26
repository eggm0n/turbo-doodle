package service

import cats.effect.IO
import model.{Priority, Task, TaskNotFoundError}
import org.http4s.{HttpService, MediaType, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import repository.TaskRepository
import io.circe.generic.auto._
import io.circe.syntax._
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.headers.{Location, `Content-Type`}

class TaskService(repository: TaskRepository) extends Http4sDsl[IO] {
  private implicit val encodeImportance: Encoder[Priority] = Encoder.encodeString.contramap[Priority](_.value)

  private implicit val decodeImportance: Decoder[Priority] = Decoder.decodeString.map[Priority](Priority.unsafeFromString)

  val service = HttpService[IO] {
    case GET -> Root / "tasks" =>
      Ok(Stream("[") ++ repository.getTasks.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))

    case GET -> Root / "tasks" / LongVar(id) =>
      for {
        getResult <- repository.getTask(id)
        response <- taskResult(getResult)
      } yield response

    case req @ POST -> Root / "tasks" =>
      for {
        task <- req.decodeJson[Task]
        createdTask <- repository.createTask(task)
        response <- Created(createdTask.asJson, Location(Uri.unsafeFromString(s"/tasks/${createdTask.id.get}")))
      } yield response

    case req @ PUT -> Root / "tasks" / LongVar(id) =>
      for {
        task <-req.decodeJson[Task]
        updateResult <- repository.updateTask(id, task)
        response <- taskResult(updateResult)
      } yield response

    case DELETE -> Root / "tasks" / LongVar(id) =>
      repository.deleteTask(id).flatMap {
        case Left(TaskNotFoundError) => NotFound()
        case Right(_)                => NoContent()
      }
  }

  private def taskResult(result: Either[TaskNotFoundError.type, Task]) = {
    result match {
      case Left(TaskNotFoundError) => NotFound()
      case Right(task)             => Ok(task.asJson)
    }
  }
}
