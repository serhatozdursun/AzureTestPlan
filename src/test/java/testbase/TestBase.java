package testbase;

import anotations.AureTestCaseId;
import anotations.AzurePlanId;
import anotations.AzureTestPlanSuitId;

import configuration.Configuration;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.extension.*;
import utils.ReuseStoreData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.baseURI;

public class TestBase implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {


    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {

        var response = RestAssured
                .given()
                .header("Authorization", "Basic " + Configuration.getInstance().getStringValueOfProp("pat"))
                .basePath("/_apis/test/runs/{runId}")
                .contentType(ContentType.JSON)
                .pathParams("runId", ReuseStoreData.get("RunId"))
                .queryParam("api-version", "6.0")
                .body("{" +
                        "    \"postProcessState\": \"Complete\"," +
                        "    \"state\": \"Completed\"" +
                        "}")
                .patch()
                .body();
        response.prettyPrint();

    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        var outcome = extensionContext.getExecutionException().isPresent()==true?"Failed":"Passed";
        var caseId = extensionContext.getTestMethod().get().getAnnotation(AureTestCaseId.class).id();
        var testCaseInfo = (HashMap<Integer, HashMap<String, Object>>)ReuseStoreData.get("TestCaseInfo");
        var testCaseRevision =testCaseInfo.get(caseId).get("System.Rev");
        var testCaseTitle =testCaseInfo.get(caseId).get("testCaseTitle");
        var testPoint =testCaseInfo.get(caseId).get("testPoint");

        var response = RestAssured
                .given()
                .header("Authorization", "Basic " + Configuration.getInstance().getStringValueOfProp("pat"))
                .contentType(ContentType.JSON)
                .queryParam("api-version", "6.0")
                .basePath("_apis/test/Runs/{runId}/results")
                .pathParams("runId", ReuseStoreData.get("RunId"))
                .body("[" +
                        "    {" +
                        "        \"durationInMs\": 2558," + // you can calculate and pass correct test time
                        "        \"testCaseRevision\": \""+testCaseRevision+"\"," +
                        "        \"computerName\": \"computer\"," +
                        "        \"startedDate\": \"2020-10-01T14:09:04.068028700\"," +
                        "        \"state\": \"Completed\"," +
                        "        \"testCaseTitle\": \""+testCaseTitle+"\"," +
                        "        \"testPoint\": {" +
                        "            \"id\": "+testPoint+"" +
                        "        }," +
                        "        \"outcome\": \""+outcome+"\"," +
                        "        \"testCase\": {" +
                        "            \"name\": \""+testCaseTitle+"\"," +
                        "            \"id\": "+caseId+
                        "        }," +
                        "        \"testRun\": {" +
                        "            \"name\": \""+ReuseStoreData.get("testRunName")+"\"," +
                        "            \"id\": "+ReuseStoreData.get("RunId")+"," +
                        "            \"url\": \""+Configuration.getInstance().getStringValueOfProp("azure.url")
                                            +"/_apis/test/Runs/"+ReuseStoreData.get("RunId")+"\"" +
                        "        }" +
                        "    }" +
                        "]")
                .log()
                .all()
                .post()
                .body();
        response.prettyPrint();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        var azureTestPlanSuitId = extensionContext.getTestClass().get().getAnnotation(AzureTestPlanSuitId.class);
        var azurePlanId = extensionContext.getTestClass().get().getAnnotation(AzurePlanId.class);
        ReuseStoreData.put("azureTestPlanSuitId", azureTestPlanSuitId.id());
        ReuseStoreData.put("azurePlanId", azurePlanId.id());
        baseURI = Configuration.getInstance().getStringValueOfProp("azure.url");
        var response = RestAssured
                .given()
                .basePath("/_apis/testplan/Plans/{planId}/Suites/{suiteId}/TestCase?api-version=6.0-preview.2")
                .pathParams("planId", ReuseStoreData.get("azurePlanId"))
                .pathParams("suiteId", ReuseStoreData.get("azureTestPlanSuitId"))
                .header("Authorization", "Basic " + Configuration.getInstance().getStringValueOfProp("pat"))
                .when()
                .get()
                .body();

        var responseMap = response.as(new TypeRef<HashMap<String, Object>>() {
        });
        var values = (List<HashMap<String, Object>>) responseMap.get("value");

        Map testCaseInfo = new HashMap<Integer, HashMap<String, Object>>();
        for (var value : values) {
            var workItem = (HashMap<String, Object>) value.get("workItem");
            var caseId = (int) workItem.get("id");
            var name = String.valueOf(workItem.get("name"));
            var revNum = ((List<HashMap<String, Object>>) workItem.get("workItemFields"));
            var systemRev = revNum.stream().filter(i -> i.containsKey("System.Rev")).map(i -> i.get("System.Rev")).findFirst().get();
            var testPoint = ((HashMap<String ,HashMap<String,String> >) value.get("links")).get("testPoints").get("href");
            testPoint = testPoint.substring(testPoint.length()-1);
            var testPointFinal = Integer.parseInt(testPoint);
            testCaseInfo.put(caseId, new HashMap<String, Object>() {{
                put("testCaseTitle", name);
                put("System.Rev", systemRev);
                put("testPoint",testPointFinal);
            }});
        }

        ReuseStoreData.put("TestCaseInfo", testCaseInfo);

        var testClassName = extensionContext.getTestClass().get().getName();
        testClassName += 1000 + new Random().nextInt(0, 9999);
        ReuseStoreData.put("testRunName",testClassName);
        var createTestRun = RestAssured
                .given()
                .basePath("/_apis/test/runs")
                .header("Authorization", "Basic " + Configuration.getInstance().getStringValueOfProp("pat"))
                .queryParam("api-version", "6.0")
                .contentType(ContentType.JSON)
                .body("{" +
                        "  \"name\": \"" + testClassName + "\"," +
                        "  \"isAutomated\": true," +
                        "  \"plan\": {" +
                        "    \"id\": \"" + ReuseStoreData.get("azurePlanId") + "\"" +
                        "  }" +
                        "}")
                .post()
                .body();

        ReuseStoreData.put("RunId", createTestRun.jsonPath().get("id"));

    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        var testCaseId = extensionContext.getTestMethod().get().getAnnotation(AureTestCaseId.class).id();
        var testCaseInfo = (HashMap<Integer, String>) ReuseStoreData.get("TestCaseInfo");
        if (!testCaseInfo.containsKey(testCaseId)) {
            throw new IllegalArgumentException();
            // or log
        }
    }
}
