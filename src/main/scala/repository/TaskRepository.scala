package repository

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.Stream
import model.{Priority, Task, TaskNotFoundError}
import doobie._
import doobie.implicits._

class TaskRepository(transactor: Transactor[IO]) {
  private implicit val importanceMeta: Meta[Priority] = Meta[String].xmap(Priority.unsafeFromString, _.value)

  def getTasks: Stream[IO, Task] = {
    sql"SELECT id, description, importance FROM task".query[Task].stream.transact(transactor)
  }

  def getTask(id: Long): IO[Either[TaskNotFoundError.type, Task]] = {
    sql"SELECT id, description, importance FROM task WHERE id = $id".query[Task].option.transact(transactor).map {
      case Some(task) => Right(task)
      case None => Left(TaskNotFoundError)
    }
  }

  def createTask(task: Task): IO[Task] = {
    sql"INSERT INTO task (description, importance) VALUES (${task.description}, ${task.importance})".update.withUniqueGeneratedKeys[Long]("id").transact(transactor).map { id =>
      task.copy(id = Some(id))
    }
  }

  def deleteTask(id: Long): IO[Either[TaskNotFoundError.type, Unit]] = {
    sql"DELETE FROM task WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(())
      } else {
        Left(TaskNotFoundError)
      }
    }
  }

  def updateTask(id: Long, task: Task): IO[Either[TaskNotFoundError.type, Task]] = {
    sql"UPDATE task SET description = ${task.description}, importance = ${task.importance} WHERE id = $id".update.run.transact(transactor).map { affectedRows =>
      if (affectedRows == 1) {
        Right(task.copy(id = Some(id)))
      } else {
        Left(TaskNotFoundError)
      }
    }
  }
}
