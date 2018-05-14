package io.realworld.ktor.model

import io.ktor.server.testing.*
import io.realworld.ktor.*
import kotlinx.coroutines.experimental.*
import org.arquillian.cube.docker.impl.client.containerobject.dsl.*
import org.jboss.arquillian.junit.*
import org.junit.*
import org.junit.runner.*

/*
@RunWith(Arquillian::class)
class IntegrationTest {
    @DockerContainer
    val mongodb = Container.withContainerName("mongodb-integration")
        .fromImage("jonmorehouse/ping-pong")
        .withPortBinding(27017)
        .build()

    @Test
    fun name() {
        withTestApplication({
            mainModule(
                runBlocking { Db(mongodb.ipAddress, mongodb.getBindPort(27017)) },
                MyJWT("testsecret")
            )
        }) {

        }
    }
}
*/
