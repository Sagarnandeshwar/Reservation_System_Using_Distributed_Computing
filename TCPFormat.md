# TCP Communication Guidelines

All TCP communications are done through serializing/deserializing a `Vector<Object>` and sending it through an `Object(Input/Output)Stream`.

## Client ==> MW (Port 7050)
 * `["methodName", arg0, arg1, ...]`

## Client <== MW (Port 7050)
 * `[returnValue, isRemoteException]`
 * No need for method name since Client (and MW thread servicing said client) is blocking and is expecting next reply to be the response to its latest request.

## MW ==> RM (Port 7051 - 7053)
 * `[threadID, "methodName", arg0, arg1, ...]`
 * ThreadID required. Expect MW client liaison threads to sync-write messages into the output buffer leading to the appropriate resource manager.

## MW <== RM (Port 7051 - 7053)
 * `[threadID, returnValue, isRemoteException]`
 * ThreadID required as singleton static RM liaison receiving thread must know which thread deliver responses to and wake (using a size 1 blocking queue).

# Miscellaneous

* Don't `toString()` stuff. Just send it in the vector and cast back at receiving end since the content of each position is known.
* Including or not threadID can be debated. For now, add it. We can just ignore it if we don't end up using it.

## Input/OutputStream Manipulation
```java
FileOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
Vector<Object> msg = new Vector<>(Arrays.asList{1, 2, true, "hello!", {1,2,3,4}, "bye!"});

FileOutputStream in = new ObjectInputStream(clientSocket.getInputStream());
Vector<Object> response = (Vector<Object>) in.readObject();
```
* The type of each index must be known and follow method argument order so that casting back at the receiving end does not produce cast exceptions.

See [test client](OSerializationTestClient.java) and [test server](OSerializationTestServer.java) for concrete examples.
