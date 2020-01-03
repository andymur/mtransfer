# Problem

Design and implement a RESTful API (including data model and the backing implementation) for
money transfers between accounts.

# Solution

Done using Dropwizard (with Jersey, H2). Service is running by Jetty.

# How to test the service

One can simply run ```mvn clean test``` to launch all the tests including tests of the persistence layer 
and ```MoneyTransferAcceptanceTest``` as a integration test.

If you want to test REST resource please find ```AccountResourceTest```

If you want to test persistence layer please find ```PersistenceServiceTest```

Make sure that ```8090``` is free to use.

# How to use the service

Just build it using ```mvn clean install``` and run with ```java -jar target/mtransfer-1.0-SNAPSHOT.jar server mtransfer.yml```

Make sure that ```8090``` is free to use.

Basically ```account``` resource will be ready with CRUD operations like

* ```PUT``` (for adding an account)
* ```GET``` (for reading an account state)
* ```DELETE``` (for deleting an account)
* ```POST``` for transfer operation

Enjoy!