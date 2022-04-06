package tests;


import anotations.AureTestCaseId;
import anotations.AzurePlanId;
import anotations.AzureTestPlanSuitId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import testbase.TestBase;

import static org.junit.jupiter.api.Assertions.fail;

@AzureTestPlanSuitId(id = 5)
@ExtendWith(TestBase.class)
@AzurePlanId(id = 3)
public class AzureIntegrationTests{

    @Test
    @AureTestCaseId(id = 1)
    public void testAzure() {
        fail();
    }

    @Test
    @AureTestCaseId(id = 6)
    public void testAzure2() {
    }
}
