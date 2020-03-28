/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ai.deepcode.javaclient;

import ai.deepcode.javaclient.requests.FileContent;
import ai.deepcode.javaclient.requests.FileContentRequest;
import ai.deepcode.javaclient.requests.FileHashRequest;
import ai.deepcode.javaclient.responses.CreateBundleResponse;
import ai.deepcode.javaclient.responses.GetAnalysisResponse;
import ai.deepcode.javaclient.responses.GetFiltersResponse;
import ai.deepcode.javaclient.responses.LoginResponse;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    response = DeepCodeRestApi.newLogin();
    assertEquals(response.getStatusCode(), 200);
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
    status = DeepCodeRestApi.checkSession(token).getStatusCode();
    System.out.printf("Check Session call with token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(401, status);

    token = "blablabla";
    status = DeepCodeRestApi.checkSession(token).getStatusCode();
    System.out.printf("Check Session call with token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(401, status);

    token = DeepCodeRestApi.newLogin().getSessionToken();
    status = DeepCodeRestApi.checkSession(token).getStatusCode();
    System.out.printf(
        "Check Session call with newly requested but not yet logged token [%1$s] return [%2$d] code.\n",
        token, status);
    assertEquals(
        "Check Session call with newly requested but not yet logged token should return 304 code.",
        304,
        status);

    token = loggedToken;
    status = DeepCodeRestApi.checkSession(token).getStatusCode();
    System.out.printf(
        "Check Session call with logged user's token [%1$s] return [%2$d] code.\n", token, status);
    assertEquals(
        "Check Session call with logged user's token should return 200 code.", 200, status);
  }

  @Test
  public void _030_createBundle_from_source() {
    System.out.println("\n--------------Create Bundle from Source----------------\n");
    int status = DeepCodeRestApi.checkSession(loggedToken).getStatusCode();
    assertEquals(200, status);
    FileContent fileContent = new FileContent("/test.js", testFileContent);
    FileContentRequest files = new FileContentRequest(Collections.singletonList(fileContent));
    CreateBundleResponse response = DeepCodeRestApi.createBundle(loggedToken, files);
    assertNotNull(response);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] %3$s \nBundleId: [%2$s]\n",
        response.getStatusCode(), response.getBundleId(), response.getStatusDescription());
    assertEquals(200, response.getStatusCode());
  }

  @Test
  public void _031_createBundle_wrong_request() {
    System.out.println("\n--------------Create Bundle with wrong requests----------------\n");
    FileContent fileContent = new FileContent("/test.js", testFileContent);
    FileContentRequest files = new FileContentRequest(Collections.singletonList(fileContent));
    CreateBundleResponse response;
    final String brokenToken = "fff";
    response = DeepCodeRestApi.createBundle(brokenToken, files);
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with malformed token should not be accepted by server",
        401,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with malformed token [%1$s] is not accepted by server with Status code [%2$d].\n",
        brokenToken, response.getStatusCode());

    final String incompleteLoginToken = DeepCodeRestApi.newLogin().getSessionToken();
    response = DeepCodeRestApi.createBundle(incompleteLoginToken, files);
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with incomplete login token should not be accepted by server",
        401,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with incomplete login token is not accepted by server with Status code [%2$d].\n",
        brokenToken, response.getStatusCode());

    response =
        DeepCodeRestApi.createBundle(loggedToken, new FileContentRequest(Collections.emptyList()));
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
  public void _035_createBundle_with_hash() {
    System.out.println("\n--------------Create Bundle with Hash----------------\n");
    int status = DeepCodeRestApi.checkSession(loggedToken).getStatusCode();
    assertEquals(200, status);

    MessageDigest digest;
    File file = new File(getClass().getClassLoader().getResource("test1.js").getFile());
    final String absolutePath = file.getAbsolutePath();
    System.out.println("File Path: " + absolutePath);

    String fileText;
    try {
      fileText = new String(Files.readAllBytes(Path.of(absolutePath)), StandardCharsets.UTF_8);
      digest = MessageDigest.getInstance("SHA-256");
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] encodedhash = digest.digest(fileText.getBytes(StandardCharsets.UTF_8));
    String hash = bytesToHex(encodedhash);
    System.out.println("File hash: " + hash);

    FileHashRequest files = new FileHashRequest(Collections.singletonMap("/" + absolutePath, hash));
    CreateBundleResponse response = DeepCodeRestApi.createBundle(loggedToken, files);
    assertNotNull(response);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        response.getStatusCode(),
        response.getBundleId(),
        response.getStatusDescription(),
        response.getMissingFiles(),
        response.getUploadURL());
    assertEquals(200, response.getStatusCode());
  }

  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  @Test
  public void _040_getAnalysis() {
    System.out.println("\n--------------Get Analysis----------------\n");
    String token = loggedToken;
    String bundleId =
        "gh/ArtsiomCh/DEEPCODE_PRIVATE_BUNDLE/83a47f630d9ad28bda6cbb068317565dce5fadce4d71f754e9a99794f2e4fb15";
    GetAnalysisResponse response = DeepCodeRestApi.getAnalysis(token, bundleId);
    assertNotNull(response);
    System.out.println(
        "Get Analysis call for test file: \n"
            + testFileContent
            + "\nreturns Status code: "
            + response.getStatusCode()
            + "\nreturns Body: "
            + response);
    //    assertEquals("DONE", response.getStatus());
    assertEquals("Get Analysis request not succeed", 200, response.getStatusCode());
  }

  @Test
  public void _050_getFilters() {
    System.out.println("\n--------------Get Filters----------------\n");
    String token = loggedToken;
    GetFiltersResponse response = DeepCodeRestApi.getFilters(token);
    assertNotNull(response);
    final String errorMsg =
        "Get Filters return status code: ["
            + response.getStatusCode()
            + "] "
            + response.getStatusDescription()
            + "\n";
    assertEquals(errorMsg, response.getStatusCode(), 200);

    System.out.println(
        "Get Filters call returns next filters:"
            + "\nextensions: "
            + response.getExtensions()
            + "\nconfigFiles: "
            + response.getConfigFiles());
  }
}
