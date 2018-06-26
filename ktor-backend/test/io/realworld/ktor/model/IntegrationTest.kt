package io.realworld.ktor.model

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
