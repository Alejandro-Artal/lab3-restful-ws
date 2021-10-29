package rest.addressbook

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import java.net.URI

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AddressBookServiceTest {

    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun cleanRepository() {
        addressBook.clear()
    }

    @Test
    fun serviceIsAlive() {
        // Request the address book

        //Saving the state (this will be used later)
        val oldId = addressBook.nextId

        val response = restTemplate.getForEntity("http://localhost:$port/contacts", Array<Person>::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(0, response.body?.size)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        //Testing that GET /contacts is safe and idempotent by checking that the state is the same
        //as before the petition
        assert(addressBook.personList.isEmpty())
        assertEquals(addressBook.nextId, oldId)

    }

    @Test
    fun createUser() {

        // Prepare data
        val juan = Person(name = "Juan")
        val juanURI: URI = URI.create("http://localhost:$port/contacts/person/1")

        //Saving part of the state (this will be used later)
        var oldId = addressBook.nextId
        var listSize = addressBook.personList.size

        // Create a new user
        var response = restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)

        assertEquals(201, response.statusCode.value())
        assertEquals(juanURI, response.headers.location)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        var juanUpdated = response.body
        assertEquals(juan.name, juanUpdated?.name)
        assertEquals(1, juanUpdated?.id)
        assertEquals(juanURI, juanUpdated?.href)

        // Check that the new user exists
        response = restTemplate.getForEntity(juanURI, Person::class.java)

        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        juanUpdated = response.body
        assertEquals(juan.name, juanUpdated?.name)
        assertEquals(1, juanUpdated?.id)
        assertEquals(juanURI, juanUpdated?.href)

        //////////////////////////////////////////////////////////////////////
        // Verify that POST /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is not safe and not idempotent
        //////////////////////////////////////////////////////////////////////

        //Testing that POST /contacts is not safe by checking that the state has changed

        assertNotEquals(addressBook.personList.size, listSize)
        assertNotEquals(addressBook.nextId, oldId)

        //Testing that it is not idempotent by checking that the state is different after doing the same petition.

        //Saving part of the state
        oldId = addressBook.nextId
        listSize = addressBook.personList.size

        restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)

        assertNotEquals(1, addressBook.personList.count { person -> person.name=="Juan" })
        assertNotEquals(addressBook.personList.size, listSize)
        assertNotEquals(addressBook.nextId, oldId)
    }

    @Test
    fun createUsers() {
        // Prepare server
        val salvador = Person(id = addressBook.nextId(), name = "Salvador")
        addressBook.personList.add(salvador)

        // Prepare data
        val juan = Person(name = "Juan")
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        val maria = Person(name = "Maria")
        val mariaURI = URI.create("http://localhost:$port/contacts/person/3")

        // Create a new user
        var response = restTemplate.postForEntity("http://localhost:$port/contacts", juan, Person::class.java)
        assertEquals(201, response.statusCode.value())
        assertEquals(juanURI, response.headers.location)

        // Create a second user
        response = restTemplate.postForEntity("http://localhost:$port/contacts", maria, Person::class.java)
        assertEquals(201, response.statusCode.value())
        assertEquals(mariaURI, response.headers.location)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)

        var mariaUpdated = response.body
        assertEquals(maria.name, mariaUpdated?.name)
        assertEquals(3, mariaUpdated?.id)
        assertEquals(mariaURI, mariaUpdated?.href)

        //Saving part of the state (this will be used later)
        var oldId = addressBook.nextId
        var listSize = addressBook.personList.size

        // Check that the new user exists
        response = restTemplate.getForEntity(mariaURI, Person::class.java)

        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        mariaUpdated = response.body
        assertEquals(maria.name, mariaUpdated?.name)
        assertEquals(3, mariaUpdated?.id)
        assertEquals(mariaURI, mariaUpdated?.href)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts/person/3 is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        //Testing that GET /contacts/person/3 is safe and idempotent by checking that the state is the same
        //as before the petition

        assertEquals(addressBook.personList.size, listSize)
        assertEquals(addressBook.nextId, oldId)
        assertEquals(addressBook.personList.count{person -> person.name=="Salvador"}, 1)
        assertEquals(addressBook.personList.count{person -> person.name=="Juan"}, 1)
        assertEquals(addressBook.personList.count{person -> person.name=="Maria"}, 1)

    }

    @Test
    fun listUsers() {

        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        //Saving part of the state (this will be used later)
        var oldId = addressBook.nextId
        var listSize = addressBook.personList.size

        // Test list of contacts
        val response = restTemplate.getForEntity("http://localhost:$port/contacts", Array<Person>::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(2, response.body?.size)
        assertEquals(juan.name, response.body?.get(1)?.name)

        //////////////////////////////////////////////////////////////////////
        // Verify that GET /contacts is well implemented by the service, i.e
        // complete the test to ensure that it is safe and idempotent
        //////////////////////////////////////////////////////////////////////

        //Testing that GET /contacts is safe and idempotent by checking that the state is the same
        //as before the petition
        assertEquals(addressBook.personList.size, listSize)
        assertEquals(addressBook.nextId, oldId)
        assertEquals(addressBook.personList.count{person -> person.name=="Salvador"}, 1)
        assertEquals(addressBook.personList.count{person -> person.name=="Juan"}, 1)

    }

    @Test
    fun updateUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        // Update Maria
        val maria = Person(name = "Maria")

        //Saving part of the state (this will be used later)
        val oldPerson = addressBook.personList.get(1)

        var response = restTemplate.exchange(juanURI, HttpMethod.PUT, HttpEntity(maria), Person::class.java)
        assertEquals(204, response.statusCode.value())

        // Verify that the update is real
        response = restTemplate.getForEntity(juanURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        val updatedMaria = response.body
        assertEquals(maria.name, updatedMaria?.name)
        assertEquals(2, updatedMaria?.id)
        assertEquals(juanURI, updatedMaria?.href)

        // Verify that only can be updated existing values
        restTemplate.execute("http://localhost:$port/contacts/person/3", HttpMethod.PUT,
            {
                it.headers.contentType = MediaType.APPLICATION_JSON
                ObjectMapper().writeValue(it.body, maria)
            },
            { assertEquals(404, it.statusCode.value()) }
        )

        //////////////////////////////////////////////////////////////////////
        // Verify that PUT /contacts/person/2 is well implemented by the service, i.e
        // complete the test to ensure that it is idempotent but not safe
        //////////////////////////////////////////////////////////////////////

        //Testing that PUT /contacts/person/2 is not safe by checking that the state has changed
        assertNotEquals(addressBook.personList.get(1).name, oldPerson.name)

        //Testing that it is idempotent by checking that the state is the same after doing the same petition.

        //Saving part of the state
        var oldId = addressBook.nextId

        var response2 = restTemplate.exchange(juanURI, HttpMethod.PUT, HttpEntity(maria), Person::class.java)
        assertEquals(204, response2.statusCode.value())

        assertEquals(addressBook.personList.size, 2)
        assertEquals(addressBook.nextId, oldId)
        assertEquals(addressBook.personList.count{person -> person.name=="Salvador"}, 1)
        assertEquals(addressBook.personList.count{person -> person.name=="Maria"}, 1)

    }

    @Test
    fun deleteUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        //Saving part of the state (this will be used later)
        var listSize = addressBook.personList.size

        // Delete a user
        restTemplate.execute(juanURI, HttpMethod.DELETE, {}, { assertEquals(204, it.statusCode.value()) })

        // Verify that the user has been deleted
        restTemplate.execute(juanURI, HttpMethod.GET, {}, { assertEquals(404, it.statusCode.value()) })

        //////////////////////////////////////////////////////////////////////
        // Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
        // complete the test to ensure that it is idempotent but not safe
        //////////////////////////////////////////////////////////////////////

        //Testing that DELETE /contacts/person/2 is not safe by checking that the state has changed
        assertNotEquals(addressBook.personList.size, listSize)

        //Testing that it is idempotent by checking that the state is the same after doing the same petition.

        //Saving part of the state
        var oldId = addressBook.nextId

        restTemplate.execute(juanURI, HttpMethod.DELETE, {}, {})

        assertEquals(addressBook.personList.size, 1)
        assertEquals(addressBook.nextId, oldId)
        assertEquals(addressBook.personList.count{person -> person.name=="Salvador"}, 1)

    }

    @Test
    fun findUsers() {
        // Prepare server
        val salvador = Person(name = "Salvador", id = addressBook.nextId())
        val juan = Person(name = "Juan", id = addressBook.nextId())
        val salvadorURI = URI.create("http://localhost:$port/contacts/person/1")
        val juanURI = URI.create("http://localhost:$port/contacts/person/2")
        addressBook.personList.add(salvador)
        addressBook.personList.add(juan)

        // Test user 1 exists
        var response = restTemplate.getForEntity(salvadorURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        var person = response.body
        assertEquals(salvador.name, person?.name)
        assertEquals(salvador.id, person?.id)
        assertEquals(salvador.href, person?.href)

        // Test user 2 exists
        response = restTemplate.getForEntity(juanURI, Person::class.java)
        assertEquals(200, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        person = response.body
        assertEquals(juan.name, person?.name)
        assertEquals(juan.id, person?.id)
        assertEquals(juan.href, person?.href)

        // Test user 3 doesn't exist
        restTemplate.execute("http://localhost:$port/contacts/person/3", HttpMethod.GET, {}, { assertEquals(404, it.statusCode.value()) })
    }

}
