
[![Maven Test Build](https://github.com/jexxa-projects/JLegMedMicrometer/actions/workflows/mavenBuild.yml/badge.svg)](https://github.com/jexxa-projects/JLegMedMicrometer/actions/workflows/mavenBuild.yml)
[![New Release](https://github.com/jexxa-projects/JLegMedMicrometer/actions/workflows/newRelease.yml/badge.svg)](https://github.com/jexxa-projects/JLegMedMicrometer/actions/workflows/newRelease.yml)

# JLegMedMicrometer
This template can be used to start your own JLegMed application
 
##  Requirements

*   Java 25+ installed
*   IDE with maven support 
*   [Optional] Docker or Kubernetes if you want to run your application in a container. See [here](README-GitHub.md) for more information.   
*   [Optional] A locally running [developer stack](deploy/developerStack.yml) providing a Postgres database, ActiveMQ broker, and Swagger-UI 

## Build the Project

*   Check out the new project in your favorite IDE

*   [Optional] **With** running [developer stack](deploy/developerStack.yml):
    ```shell
    mvn clean install
    
    java -jar "-Djlegmed.config.import=src/test/resources/jlegmed-test.properties" target/jlegmedmicrometer-jar-with-dependencies.jar
    ```


## Start Developing your Project

### Set up your project on GitHub  

To continuously build and deploy your application, we recommend using GitHub as described [here](README-GitHub.md).

### Package Structure
To organize our code, we recommend the following package structure. Since messages are treated as first-class objects, we strongly recommend a fine-grained packages structure:

``` 
(com.github.jlegmedproject)
    flowgraph // Provides app specific flow graphs 
    plugins   // Provides app specific filter 
    |    <technology 1>
    |    ...
    |    <technology n>
    dto
        |    consumedmessage  // Messages received by the app
        |    publishedmessage // Messages published by the app
        |    domainevent      // Domain events published by the app
        |    telemetrydata    // Telemetry data published by the app
        |    flowdata         // Data structures used only within the app
        |    flowerror        // Exceptions occurred during processing
 
```

### Cleanup Readme

After successfully setting up your new project, you should clean up the text of README as described [here](https://www.makeareadme.com)    
