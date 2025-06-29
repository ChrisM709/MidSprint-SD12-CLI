package com.airport.http.client;

import com.airport.domain.Passenger;
import com.airport.domain.Aircraft;
import com.airport.domain.Airport;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RESTClientTest {

    // reate a MockWebServer which will act as a fake http server
    private static MockWebServer mockServer;

    // RESTClient under test, which will send its requests to our mock server
    private RESTClient client;

    // This method runs once before any @Test methods. It starts the MockWebServer
    // and points our RESTClient at it.
    @BeforeAll
    static void startMockServer() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    // shuts down the MockWebServer so no ports remain in use
    @AfterAll
    static void shutdownMockServer() throws Exception {
        mockServer.shutdown();
    }

    // ensures that for every test, we re-create a fresh RESTClient
    @BeforeEach
    void setUp() {
        String baseUrl = mockServer.url("/").toString();
        client = new RESTClient(baseUrl);
    }

    @Test
    void getAllPassengers_emptyArrayResultsInEmptyList() {
        mockServer.enqueue(new MockResponse()
            .setBody("[]")
            .setResponseCode(200)
        );

        List<Passenger> list = client.getAllPassengers();

        assertNotNull(list, "Should never return null");
        assertTrue(list.isEmpty(), "Empty JSON array should produce empty list");
    }

    @Test
    void getAllPassengers_parsesCorrectly() {
        mockServer.enqueue(new MockResponse()
                .setBody(
                        """
                                [
                                  {"id":1,"birthday":"1990-01-01","firstName":"Alice","lastName":"Smith","phoneNumber":"555-1234","flights":[]},
                                  {"id":2,"birthday":"1985-05-05","firstName":"Bob","lastName":"Jones","phoneNumber":"555-5678","flights":[]}
                                ]
                                """));

        List<Passenger> p = client.getAllPassengers();

        assertAll("Passengers",
                () -> assertEquals("Alice", p.get(0).getFirstName()),
                () -> assertEquals("Jones", p.get(1).getLastName()));
    }

    @Test
    void getAllAircraft_emptyArrayResultsInEmptyList() {
        mockServer.enqueue(new MockResponse().setBody("[]"));

        assertTrue(client.getAllAircraft().isEmpty());
    }

    @Test
    void getAllAircraft_parsesNonEmptyArrayCorrectly() {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        [
                          {"id":10,"tailNumber":"C-ABC","model":"Boeing 737"},
                          {"id":20,"tailNumber":"C-XYZ","model":"Airbus A320"}
                        ]
                        """));

        List<Aircraft> list = client.getAllAircraft();

        assertAll("Aircraft list",
                () -> assertEquals(2, list.size(), "Should parse two aircraft"),
                () -> assertEquals("C-ABC", list.get(0).getTailNumber()),
                () -> assertEquals("Boeing 737", list.get(0).getModel()),
                () -> assertEquals("C-XYZ", list.get(1).getTailNumber()),
                () -> assertEquals("Airbus A320", list.get(1).getModel()));
    }

    @Test
    void getAllAirport_parsesCorrectly(){
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                [
                  {
                    "id": 1,
                    "name":"Toronto Pearson International Airport",
                    "code": "YYZ",
                    "city": { "id": 100, "name": "Toronto", "state": "ON", "population": 2700000 }
                },
                 {
                    "id": 2,
                    "name":"Vancouver International Airport",
                    "code": "YVR",
                    "city": { "id": 200, "name": "Vancouver", "state": "BC", "population": 675000 }
                }   
            
            ]
            """)
            );
            List<Airport> airports = client.getAllAirports();

            assertAll("Airports parsed",
            () -> assertEquals(2, airports.size()),
            () -> assertEquals("YYZ", airports.get(0).getCode()),
            () -> assertEquals("Toronto", airports.get(0).getCity().getName()),
            () -> assertEquals("YVR", airports.get(1).getCode()),
            () -> assertEquals("Vancouver", airports.get(1).getCity().getName())
            );
        }

    @Test
    void getAirportsByCityId_returnsCorrectAirports(){
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                    [
                  { 
                    "id": 3, 
                    "name": "Halifax Stanfield International Airport",
                    "code": "YHZ",
                    "city": { "id": 300, "name": "Halifax", "state": "NS", "population": 431000 }
                  }
              ]

            """)
        );

        List<Airport> airports = client.getAirportsByCityId(300L);

        assertAll("Airports by City ID",
            () -> assertEquals(1, airports.size()),
            () -> assertEquals("YHZ", airports.get(0).getCode()),
            () -> assertEquals("Halifax", airports.get(0).getCity().getName())
        );
    }

    @Test
    void getAirportsByCityId_handlesErrorResponse() {
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        long testCityID = 42;
        List<Airport> result = client.getAirportsByCityId(testCityID);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Return an emtpy list on error.");

    }

    @Test
    void getAllAirports_handlesErrorResponse(){
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        List<Airport> result = client.getAllAirports();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return an empty list on server error.");
    }

    @Test
    void getAirportsByAircraft_returnsCorrectAirports() {
        mockServer.enqueue(new MockResponse().setResponseCode(200).setBody("""
                    [
                  { 
                    "id": 4, 
                    "name": "Calgary International Airport",
                    "code": "YYC",
                    "city": { "id": 400, "name": "Calgary", "state": "AB", "population": 1231000 }
                  }
              ]

            """));
        long testAircraftID = 42;
        List<Airport> airports = client.getAirportsByAircraft(testAircraftID);

        assertAll("Airports by Aircraft ID",
        () -> assertEquals(1, airports.size()),
        () -> assertEquals("YYC", airports.get(0).getCode()),
        () -> assertEquals("Calgary", airports.get(0).getCity().getName())
        );
    }

    @Test
    void getAirportsByAircraft_handlesErrorResponse() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        long testAircraftID = 42;
        List<Airport> result = client.getAirportsByAircraft(testAircraftID);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Return an empty list on server error.");
    }
}
