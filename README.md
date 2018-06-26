# turbo-doodle
[http4s](http://http4s.org/), [doobie](http://tpolecat.github.io/doobie/),
and [circe](https://github.com/circe/circe).


## End points
The end points are:

Method | Url         | Description
------ | ----------- | -----------
GET    | /tasks      | Returns all tasks.
GET    | /tasks/{id} | Returns the task for the specified id, 404 when no task present with this id.
POST   | /tasks      | Creates a task, give as body JSON with the description and priority, returns a 201 with the created task.
PUT    | /tasks/{id} | Updates an existing task, give as body JSON with the description and priority, returns a 200 with the updated task when a task is present with the specified id, 404 otherwise.
DELETE | /tasks/{id} | Deletes the task with the specified task, 404 when no task present with this id.

Examples:

Create a task:
```curl -X POST --header "Content-Type: application/json" --data '{"description": "my task", "priority": "high"}' http://localhost:8080/tasks```

Get all tasks:
```curl http://localhost:8080/tasks```

Get a single task (assuming the id of the task is 1):
```curl http://localhost:8080/tasks/1```

Update a task (assuming the id of the task is 1):
```curl -X PUT --header "Content-Type: application/json" --data '{"description": "my task", "priority": "low"}' http://localhost:8080/tasks/1```

Delete a task (assuming the id of the task is 1):
```curl -X DELETE http://localhost:8080/tasks/1```

## Tests
This example project contains both unit tests, which mock the repository that accesses the database, and
integration tests that use the [http4s](http://http4s.org/) HTTP client to perform actual requests.

## Running
You can run the microservice with `sbt run`. By default it listens to port number 8080, you can change
this in the `application.conf`.
