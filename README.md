# Ktorbased realworld backend

## Description

* This is a realworld backend implementation using ktor.
* It uses a mongodb client implemented using kotlin coroutines (<https://github.com/soywiz/ktor-cio-clients/tree/master/ktor-client-cio-mongodb>).
* This first implementation uses untyped extensible objects backed by a map using Kotlin's properties, so no reflection or serialization is used. A typed version will come later.

## Running

### MongoDB

You have to execute a mongodb server in your machine in the default 27017 port without authentication..

### Compile Ktor 0.9.2-SNAPSHOT with authentication optionalbranch from source

You have to download ktor <https://github.com/ktorio/ktor> and to checkout the `authenticate.optional` branch and execute `./gradlew install`

### Execute the ktor-based realworld backend

Now you can go to the folder `ktor-backend` and execute `gradle run`.
It should run the API at <http://127.0.0.1:8080/> (the `/` endpoint should return 
`{"ok":"ok"}`).

### Execute a compatible frontend pointing to the 127.0.0.1:8080 backend

There is a folder `react-redux-realworld-example-app` 
containing the react sample, but pointing to the local 
endpoint.

You can start it with `npm install && npm start`.

It should open a browser pointing to 
<http://127.0.0.1:4100>.

If everything went fine, you should be able to test the 
backend locally.
