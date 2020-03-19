/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ai.deepcode.javaclient;

import ai.deepcode.javaclient.requests.FileContent;
import ai.deepcode.javaclient.requests.FileContentRequest;
import ai.deepcode.javaclient.responses.CreateBundleResponse;
import ai.deepcode.javaclient.responses.GetAnalysisResponse;
import ai.deepcode.javaclient.responses.LoginResponse;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeepCodeRestApiTest {

  private final String testFileContent =
      "(function($) { \n"
          + "    \n"
          + "    var todos = storage.getTODOs(pullRequestJson).filter(function(todo) {}); \n"
          + "    \n"
          + "}(AJS.$));";

  // !!! Will works only with already logged sessionToken
  private final String loggedToken =
      "aeedc7d1c2656ea4b0adb1e215999f588b457cedf415c832a0209c9429c7636e";
  private final String secondLoggedToken =
      "c6ea36d5f67534826d9cd875ae3d7f2257ac59f74230d4c8bae4490c5cd66fe4";

  @Test
  public void _010_newLogin() {
    System.out.println("\n--------------New Login----------------\n");
    LoginResponse response = null;
    try {
      response = DeepCodeRestApi.newLogin();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    assertEquals(
        "https://www.deepcode.ai/login-api?sessionToken=" + response.getSessionToken(),
        response.getLoginURL());
    System.out.println(
        "New login request passed with returned:"
            + "\nsession token: "
            + response.getSessionToken()
            + "\nlogin URL: "
            + response.getLoginURL());
  }

  @Test
  public void _020_checkSession() {
    System.out.println("\n--------------Check Session----------------\n");
    String token = "";
    int status = 0;
    try {
      status = DeepCodeRestApi.checkSession(token).getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    System.out.printf("Check Session call with token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(401, status);

    token = "blablabla";
    try {
      status = DeepCodeRestApi.checkSession(token).getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    System.out.printf("Check Session call with token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(401, status);

    try {
      token = getNewNotLoggedToken();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    try {
      status = DeepCodeRestApi.checkSession(token).getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    System.out.printf(
        "Check Session call with newly requested but not yet logged token [%1$s] return [%2$d] code.\n",
        token, status);
    assertEquals(
        "Check Session call with newly requested but not yet logged token should return 304 code.",
        304,
        status);

    token = loggedToken;
    try {
      status = DeepCodeRestApi.checkSession(token).getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    System.out.printf(
        "Check Session call with logged user's token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(
        "Check Session call with logged user's token should return 200 code.", 200, status);
  }

  private String getNewNotLoggedToken() throws IOException {
    LoginResponse response = DeepCodeRestApi.newLogin();
    assertNotNull(response);
    return response.getSessionToken();
  }

  @Test
  public void _030_createBundle() {
    System.out.println("\n--------------Create Bundle----------------\n");
    String token = loggedToken;
    int status = 0;
    try {
      status = DeepCodeRestApi.checkSession(token).getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertEquals(200, status);
    FileContent fileContent = new FileContent("/test.js", testFileContent);
    FileContentRequest files = new FileContentRequest(Collections.singletonList(fileContent));
    CreateBundleResponse response;
    try {
      response = DeepCodeRestApi.createBundle(token, files);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] \nBundleId: [%2$s]\n",
        response.getStatusCode(), response.getBundleId());

    final String brokenToken = "fff";
    try {
      response = DeepCodeRestApi.createBundle(brokenToken, files);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with malformed token should not be accepted by server",
        401,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with malformed token [%1$s] is not accepted by server with Status code [%2$d].\n",
        brokenToken, response.getStatusCode());

    final String incompleteLoginToken;
    try {
      incompleteLoginToken = getNewNotLoggedToken();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    try {
      response = DeepCodeRestApi.createBundle(incompleteLoginToken, files);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with incomplete login token should not be accepted by server",
        401,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with incomplete login token is not accepted by server with Status code [%2$d].\n",
        brokenToken, response.getStatusCode());

    try {
      response =
          DeepCodeRestApi.createBundle(token, new FileContentRequest(Collections.emptyList()));
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with malformed (empty) files array should not be accepted by server",
        400,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with malformed (empty) files array is not accepted by server with Status code [%1$d].\n",
        response.getStatusCode());
  }

  @Test
  public void _040_getAnalysis() {
    System.out.println("\n--------------Get Analysis----------------\n");
    String token = loggedToken;
    String bundleId =
        "gh/ArtsiomCh/DEEPCODE_PRIVATE_BUNDLE/83a47f630d9ad28bda6cbb068317565dce5fadce4d71f754e9a99794f2e4fb15";
    GetAnalysisResponse response = null;
    try {
      response = DeepCodeRestApi.getAnalysis(token, bundleId);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    assertNotNull(response);
    System.out.println(
        "Get Analysis call for test file: \n"
            + testFileContent
            + "\nreturns Status code: "
            + response.getStatusCode()
            + "\nreturns Body: "
            + response);
    //    assertEquals("DONE", response.getStatus());
  }
}
