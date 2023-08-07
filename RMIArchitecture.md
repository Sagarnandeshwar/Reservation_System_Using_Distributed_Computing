# RMI Architecture
## Middleware
The RMI architecture consists of a middleware that holds RMI remote references to all 3 resource managers (located at hardcoded ports & name binds), while itself also being a resource manager for customers.

When the Client calls one of its interface methods, it will:
* __[Only concerns flights/cars/rooms]__: Forward the call using the same arguments by calling the corresponding method in the appropriate resource manager via RMI.
* __[Only concerns customers]__: Simply run the called method locally (inherited from ResourceManager) with no change.
* __[Mix]__: The called method will be overriden by the middleware so that parts of the method concerning customers execute locally, and parts concerning resources are executed via RMI on the appropriate resourcemanager. The called method in the resource managers will likewise be overriden so as to only contain the parts concerning the resource manager's own resource (flight/car/room).

## Resource Managers
The resource manager are all identical to each other, and are each able to store more than one type of resource, but the middleware will never call methods unrelated to its designated resource (eg. `addFlight()` on the Cars resource manager). They are differentiated by their registry bind names.

## Client
The client is not changed, but it should only connect to the middleware and not the resource managers despite being able to.