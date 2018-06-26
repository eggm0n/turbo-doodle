package service

import cats.effect.IO
import fs2.Stream
import io.circe.Json
import io.circe.literal._
import model.{High, Low, Medium, Task}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{Request, Response, Status, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import repository.TaskRepository

class TaskServiceSpec extends WordSpec with MockFactory with Matchers {
  private val repository = stub[TaskRepository]

  private val service = new TaskService(repository).service

  "TaskService" should {
    "create a task" in {
      val id = 1
      val task = Task(None, "my task", Low)
      (repository.createTask _).when(task).returns(IO.pure(task.copy(id = Some(id))))
      val createJson = json"""
        {
          "description": ${task.description},
          "importance": ${task.importance.value}
        }"""
      val response = serve(Request[IO](POST, uri("/tasks")).withBody(createJson).unsafeRunSync())
      response.status shouldBe Status.Created
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${task.description},
          "importance": ${task.importance.value}
        }"""
    }

    "update a task" in {
      val id = 1
      val task = Task(None, "updated task", Medium)
      (repository.updateTask _).when(id, task).returns(IO.pure(Right(task.copy(id = Some(id)))))
      val updateJson = json"""
        {
          "description": ${task.description},
          "importance": ${task.importance.value}
        }"""

      val response = serve(Request[IO](PUT, Uri.unsafeFromString(s"/tasks/$id")).withBody(updateJson).unsafeRunSync())
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${task.description},
          "importance": ${task.importance.value}
        }"""
    }

    "return a single task" in {
      val id = 1
      val task = Task(Some(id), "my task", High)
      (repository.getTask _).when(id).returns(IO.pure(Right(task)))

      val response = serve(Request[IO](GET, Uri.unsafeFromString(s"/tasks/$id")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        {
          "id": $id,
          "description": ${task.description},
          "importance": ${task.importance.value}
        }"""
    }

    "return all tasks" in {
      val id1 = 1
      val task1 = Task(Some(id1), "my task 1", High)
      val id2 = 2
      val task2 = Task(Some(id2), "my task 2", Medium)
      val tasks = Stream(task1, task2)
      (repository.getTasks _).when().returns(tasks)

      val response = serve(Request[IO](GET, uri("/tasks")))
      response.status shouldBe Status.Ok
      response.as[Json].unsafeRunSync() shouldBe json"""
        [
         {
           "id": $id1,
           "description": ${task1.description},
           "importance": ${task1.importance.value}
         },
         {
           "id": $id2,
           "description": ${task2.description},
           "importance": ${task2.importance.value}
         }
        ]"""
    }

    "delete a task" in {
      val id = 1
      (repository.deleteTask _).when(id).returns(IO.pure(Right(())))

      val response = serve(Request[IO](DELETE, Uri.unsafeFromString(s"/tasks/$id")))
      response.status shouldBe Status.NoContent
    }
  }

  private def serve(request: Request[IO]): Response[IO] = {
    service.orNotFound(request).unsafeRunSync()
  }
}
