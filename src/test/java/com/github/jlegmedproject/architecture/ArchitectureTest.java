package com.github.jlegmedproject.architecture;

import com.github.jlegmedproject.MyJLegMedMicrometer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.jexxa.jlegmedtest.architecture.ArchitectureRules;


/**
 * This test can be used to validate the architecture of your application
 */
class ArchitectureTest {

    @Test
    void testDTOs()
    {
        //Arrange
        var dtoRules = ArchitectureRules.dtoRules(MyJLegMedMicrometer.class);

        //Act/assert
        assertDoesNotThrow(dtoRules::validate);
    }

    @Test
    void testFilter()
    {
        //Arrange
        var filterRules = ArchitectureRules.filterRules(MyJLegMedMicrometer.class);

        //Act/assert
        assertDoesNotThrow(filterRules::validate);
    }
}

