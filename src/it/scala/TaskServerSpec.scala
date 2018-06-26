import cats.effect.IO
import config.Config
import db.Database
import io.circe.Json
import io.circe.literal._
import org.http4s.circe._
import org.http4s.client.blaze.Http1Client
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import io.circe.optics.JsonPath._
import org.http4s.server.{Server => Http4sServer}
import org.http4s.server.blaze.BlazeBuilder
import repository.TaskRepository
import service.TaskService

class TaskServerSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  private lazy val client = Http1Client[IO]().unsafeRunSync()

  private lazy val config = Config.load("test.conf").unsafeRunSync()

  private lazy val urlStart = s"http://${config.server.host}:${config.server.port}"

  private val server = createServer().unsafeRunSync()

  override def afterAll(): Unit = {
    client.shutdown.unsafeRunSync()
    server.shutdown.unsafeRunSync()
  }

  "Task server" should {
    "create a task" in {
      val description = "my task 1"
      val importance = "high"
      val createJson =json"""
        {
          "description": $description,
          "importance": $importance
        }"""
      val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/tasks")).withBody(createJson).unsafeRunSync()
      val json = client.expect[Json](request).unsafeRunSync()
      root.id.long.getOption(json).nonEmpty shouldBe true
      root.description.string.getOption(json) shouldBe Some(description)
      root.importance.string.getOption(json) shouldBe Some(importance)
    }

    "update a task" in {
      val id = createTask("my task 2", "low")

      val description = "updated task"
      val importance = "medium"
      val updateJson = json"""
        {
          "description": $description,
          "importance": $importance
        }"""
      val request = Request[IO](method = Method.PUT, uri = Uri.unsafeFromString(s"$urlStart/tasks/$id")).withBody(updateJson).unsafeRunSync()
      client.expect[Json](request).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": $description,
          "importance": $importance
        }"""
    }

    "return a single task" in {
      val description = "my task 3"
      val importance = "medium"
      val id = createTask(description, importance)
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/tasks/$id")).unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": $description,
          "importance": $importance
        }"""
    }

    "delete a task" in {
      val description = "my task 4"
      val importance = "low"
      val id = createTask(description, importance)
      val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/tasks/$id"))
      client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent

      val getRequest = Request[IO](method = Method.GET, uri = Uri.unsafeFromString(s"$urlStart/tasks/$id"))
      client.status(getRequest).unsafeRunSync() shouldBe Status.NotFound
    }

    "return all tasks" in {
      // Remove all existing tasks
      val json = client.expect[Json](Uri.unsafeFromString(s"$urlStart/tasks")).unsafeRunSync()
      root.each.id.long.getAll(json).foreach { id =>
        val deleteRequest = Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(s"$urlStart/tasks/$id"))
        client.status(deleteRequest).unsafeRunSync() shouldBe Status.NoContent
      }

      // Add new tasks
      val description1 = "my task 1"
      val description2 = "my task 2"
      val importance1 = "high"
      val importance2 = "low"
      val id1 = createTask(description1, importance1)
      val id2 = createTask(description2, importance2)

      // Retrieve tasks
      client.expect[Json](Uri.unsafeFromString(s"$urlStart/tasks")).unsafeRunSync shouldBe json"""
        [
          {
            "id": $id1,
            "description": $description1,
            "importance": $importance1
          },
          {
            "id": $id2,
            "description": $description2,
            "importance": $importance2
          }
        ]"""
    }
  }

  private def createTask(description: String, importance: String): Long = {
    val createJson =json"""
      {
        "description": $description,
        "importance": $importance
      }"""
    val request = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"$urlStart/tasks")).withBody(createJson).unsafeRunSync()
    val json = client.expect[Json](request).unsafeRunSync()
    root.id.long.getOption(json).nonEmpty shouldBe true
    root.id.long.getOption(json).get
  }

  private def createServer(): IO[Http4sServer[IO]] = {
    for {
      transactor <- Database.transactor(config.database)
      _ <- Database.initialize(transactor)
      repository = new TaskRepository(transactor)
      server <- BlazeBuilder[IO]
        .bindHttp(config.server.port, config.server.host)
        .mountService(new TaskService(repository).service, "/").start
    } yield server
  }
}
