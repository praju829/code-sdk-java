/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ai.deepcode.javaclient;

import ai.deepcode.javaclient.requests.*;
import ai.deepcode.javaclient.responses.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeepCodeRestApiTest {

  private final String testFileContent =
      "public class AnnotatorTest {\n"
          + "  public static void delay(long millis) {\n"
          + "    try {\n"
          + "      Thread.sleep(millis);\n"
          + "    } catch (InterruptedException e) {\n"
          + "      e.printStackTrace();\n"
          + "    }\n"
          + "  }\n"
          + "}\n";

  // !!! Will works only with already logged sessionToken
  private static final String loggedToken = System.getenv("DEEPCODE_API_KEY");
  private final String deepcodedLoggedToken = System.getenv("DEEPCODE_API_KEY_STAGING");

  private static String bundleId = null;

  private static String userAgent = "Java-client-Test";

  @Test
  public void _010_newLogin() {
    System.out.println("\n--------------New Login----------------\n");
    LoginResponse response = null;
    response = DeepCodeRestApi.newLogin(userAgent);
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getLoginURL().contains(response.getSessionToken()));
    System.out.printf(
        "New login request passed with returned: \nsession token: %1$s \nlogin URL: %2$s\n",
        response.getSessionToken(), response.getLoginURL());
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

    token = DeepCodeRestApi.newLogin(userAgent).getSessionToken();
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
  public void _022_setBaseUrl() {
    System.out.println("\n--------------Set base URL----------------\n");
    try {
      doSetBaseUrlTest("", "blabla", 401);
      doSetBaseUrlTest("https://www.google.com/", "blabla", 404);
      doSetBaseUrlTest("https://www.deepcoded.com/", "blabla", 401);
      doSetBaseUrlTest("https://www.deepcoded.com/", deepcodedLoggedToken, 200);
    } finally {
      DeepCodeRestApi.setBaseUrl("");
    }
  }

  private void doSetBaseUrlTest(String baseUrl, String token, int expectedStatusCode) {
    DeepCodeRestApi.setBaseUrl(baseUrl);
    EmptyResponse response = DeepCodeRestApi.checkSession(token);
    int status = response.getStatusCode();
    String description = response.getStatusDescription();
    System.out.printf(
        "Check Session call to [%3$s] with token [%1$s] return [%2$d] code: [%4$s]\n",
        token, status, baseUrl, description);
    assertEquals(expectedStatusCode, status);
  }

  @Test
  public void _025_getFilters() {
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
    assertEquals(errorMsg, 200, response.getStatusCode());

    System.out.printf(
        "Get Filters call returns next filters: \nextensions: %1$s \nconfigFiles: %2$s\n",
        response.getExtensions(), response.getConfigFiles());
  }

  @Test
  public void _030_createBundle_from_source() {
    System.out.println("\n--------------Create Bundle from Source----------------\n");
    int status = DeepCodeRestApi.checkSession(loggedToken).getStatusCode();
    assertEquals(200, status);
    FileContent fileContent = new FileContent("/AnnotatorTest.java", testFileContent);
    FileContentRequest files = new FileContentRequest(Collections.singletonList(fileContent));
    CreateBundleResponse response = DeepCodeRestApi.createBundle(loggedToken, files);
    assertNotNull(response);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] %3$s \nBundleId: [%2$s]\n",
        response.getStatusCode(), response.getBundleId(), response.getStatusDescription());
    assertEquals(200, response.getStatusCode());
    bundleId = response.getBundleId();
  }

  @Test
  public void _031_createBundle_wrong_request() {
    System.out.println("\n--------------Create Bundle with wrong requests----------------\n");
    FileContent fileContent = new FileContent("/AnnotatorTest.java", testFileContent);
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

    final String incompleteLoginToken = DeepCodeRestApi.newLogin(userAgent).getSessionToken();
    response = DeepCodeRestApi.createBundle(incompleteLoginToken, files);
    assertNotNull(response);
    assertEquals(
        "Create Bundle call with incomplete login token should not be accepted by server",
        401,
        response.getStatusCode());
    System.out.printf(
        "Create Bundle call with incomplete login token is not accepted by server with Status code [%2$d].\n",
        brokenToken, response.getStatusCode());

    // seems to be a bug on server: it returns 200
    /*
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
    */
  }

  @Test
  public void _035_createBundle_with_hash() {
    System.out.println("\n--------------Create Bundle with Hash----------------\n");
    FileHashRequest files = createFileHashRequest(null);
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

  @Test
  public void _036_Check_Bundle() {
    System.out.println("\n--------------Check Bundle----------------\n");
    FileHashRequest fileHashRequest = createFileHashRequest(null);
    CreateBundleResponse createBundleResponse =
        DeepCodeRestApi.createBundle(loggedToken, fileHashRequest);
    assertNotNull(createBundleResponse);
    System.out.printf(
        "\nCreate Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        createBundleResponse.getStatusCode(),
        createBundleResponse.getBundleId(),
        createBundleResponse.getStatusDescription(),
        createBundleResponse.getMissingFiles(),
        createBundleResponse.getUploadURL());
    assertEquals(200, createBundleResponse.getStatusCode());
    assertFalse("List of missingFiles is empty.", createBundleResponse.getMissingFiles().isEmpty());

    CreateBundleResponse checkBundleResponse =
        DeepCodeRestApi.checkBundle(loggedToken, createBundleResponse.getBundleId());
    assertNotNull(checkBundleResponse);
    System.out.printf(
        "\nCheck Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        checkBundleResponse.getStatusCode(),
        checkBundleResponse.getBundleId(),
        checkBundleResponse.getStatusDescription(),
        checkBundleResponse.getMissingFiles(),
        checkBundleResponse.getUploadURL());
    assertEquals(200, checkBundleResponse.getStatusCode());
    assertFalse("List of missingFiles is empty.", checkBundleResponse.getMissingFiles().isEmpty());
    assertEquals(
        "Checked and returned bundleId's are different.",
        createBundleResponse.getBundleId(),
        checkBundleResponse.getBundleId());

    EmptyResponse uploadFileResponse = doUploadFile(createBundleResponse, fileHashRequest);

    assertNotNull(uploadFileResponse);
    System.out.printf(
        "\nUpload Files call for file %3$s \nStatus code [%1$d] %2$s\n",
        uploadFileResponse.getStatusCode(),
        uploadFileResponse.getStatusDescription(),
        createBundleResponse.getMissingFiles().get(0));
    assertEquals(200, uploadFileResponse.getStatusCode());

    CreateBundleResponse createBundleResponse1 =
        DeepCodeRestApi.checkBundle(loggedToken, createBundleResponse.getBundleId());
    assertNotNull(createBundleResponse1);
    System.out.printf(
        "\nCheck Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        createBundleResponse1.getStatusCode(),
        createBundleResponse1.getBundleId(),
        createBundleResponse1.getStatusDescription(),
        createBundleResponse1.getMissingFiles(),
        createBundleResponse1.getUploadURL());
    assertEquals(200, createBundleResponse1.getStatusCode());
    assertTrue(
        "List of missingFiles is NOT empty.", createBundleResponse1.getMissingFiles().isEmpty());
    assertEquals(
        "Checked and returned bundleId's are different.",
        createBundleResponse.getBundleId(),
        checkBundleResponse.getBundleId());
  }

  private FileHashRequest createFileHashRequest(String fakeFileName) {
    int status = DeepCodeRestApi.checkSession(loggedToken).getStatusCode();
    assertEquals(200, status);
    final File testFile =
        new File(getClass().getClassLoader().getResource("AnnotatorTest.java").getFile());
    MessageDigest digest;
    String absolutePath = testFile.getAbsolutePath();
    String deepCodedPath =
        (absolutePath.startsWith("/") ? "" : "/")
            + ((fakeFileName == null)
                ? absolutePath
                : absolutePath.replace("AnnotatorTest.java", fakeFileName));
    System.out.printf("\nFile: %1$s\n", deepCodedPath);
    System.out.println("-----------------");

    // Append with System.currentTimeMillis() to make new Hash.
    try (FileOutputStream fos = new FileOutputStream(absolutePath, true)) {
      fos.write(String.valueOf(System.currentTimeMillis()).getBytes());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    String fileText;
    try {
      // ?? com.intellij.openapi.util.io.FileUtil#loadFile(java.io.File, java.nio.charset.Charset)
      fileText = Files.readString(Paths.get(absolutePath));
      digest = MessageDigest.getInstance("SHA-256");
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    System.out.println(fileText);
    System.out.println("-----------------");

    byte[] encodedhash = digest.digest(fileText.getBytes(StandardCharsets.UTF_8));
    String hash = bytesToHex(encodedhash);
    System.out.printf("File hash: %1$s\n", hash);

    return new FileHashRequest(Collections.singletonMap(deepCodedPath, hash));
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
  public void _037_ExtendBundle() {
    System.out.println("\n--------------Extend Bundle----------------\n");
    FileHashRequest fileHashRequest = createFileHashRequest(null);
    CreateBundleResponse createBundleResponse =
        DeepCodeRestApi.createBundle(loggedToken, fileHashRequest);
    assertNotNull(createBundleResponse);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        createBundleResponse.getStatusCode(),
        createBundleResponse.getBundleId(),
        createBundleResponse.getStatusDescription(),
        createBundleResponse.getMissingFiles(),
        createBundleResponse.getUploadURL());
    assertEquals(200, createBundleResponse.getStatusCode());
    assertFalse("List of missingFiles is empty.", createBundleResponse.getMissingFiles().isEmpty());

    FileHashRequest newFileHashRequest = createFileHashRequest("test2.js");
    ExtendBundleRequest extendBundleRequest =
        new ExtendBundleRequest(newFileHashRequest.getFiles(), Collections.emptyList());
    CreateBundleResponse extendBundleResponse =
        DeepCodeRestApi.extendBundle(
            loggedToken, createBundleResponse.getBundleId(), extendBundleRequest);
    assertNotNull(extendBundleResponse);
    System.out.printf(
        "Extend Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        extendBundleResponse.getStatusCode(),
        extendBundleResponse.getBundleId(),
        extendBundleResponse.getStatusDescription(),
        extendBundleResponse.getMissingFiles(),
        extendBundleResponse.getUploadURL());
    assertEquals(200, extendBundleResponse.getStatusCode());
    assertFalse("List of missingFiles is empty.", extendBundleResponse.getMissingFiles().isEmpty());
  }

  @Test
  public void _040_UploadFiles() {
    System.out.println("\n--------------Upload Files by Hash----------------\n");
    FileHashRequest fileHashRequest = createFileHashRequest(null);
    CreateBundleResponse createBundleResponse =
        DeepCodeRestApi.createBundle(loggedToken, fileHashRequest);
    assertNotNull(createBundleResponse);
    System.out.printf(
        "Create Bundle call return:\nStatus code [%1$d] %3$s \n bundleId: %2$s\n missingFiles: %4$s\n uploadUrl: %5$s\n",
        createBundleResponse.getStatusCode(),
        createBundleResponse.getBundleId(),
        createBundleResponse.getStatusDescription(),
        createBundleResponse.getMissingFiles(),
        createBundleResponse.getUploadURL());
    assertEquals(200, createBundleResponse.getStatusCode());
    assertFalse("List of missingFiles is empty.", createBundleResponse.getMissingFiles().isEmpty());

    EmptyResponse response = doUploadFile(createBundleResponse, fileHashRequest);

    assertNotNull(response);
    System.out.printf(
        "\nUpload Files call for file %3$s \nStatus code [%1$d] %2$s\n",
        response.getStatusCode(),
        response.getStatusDescription(),
        createBundleResponse.getMissingFiles().get(0));
    assertEquals(200, response.getStatusCode());
  }

  private EmptyResponse doUploadFile(
      CreateBundleResponse createBundleResponse, FileHashRequest fileHashRequest) {
    final File testFile =
        new File(getClass().getClassLoader().getResource("AnnotatorTest.java").getFile());
    final String absolutePath = testFile.getAbsolutePath();
    String fileText;
    try {
      // ?? com.intellij.openapi.util.io.FileUtil#loadFile(java.io.File, java.nio.charset.Charset)
      fileText = Files.readString(Paths.get(absolutePath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final String filePath = createBundleResponse.getMissingFiles().get(0);
    final String fileHash = fileHashRequest.getFiles().get(filePath);
    final List<FileHash2ContentRequest> requestBody =
        Collections.singletonList(new FileHash2ContentRequest(fileHash, fileText));
    return DeepCodeRestApi.UploadFiles(
        loggedToken, createBundleResponse.getBundleId(), requestBody);
  }

  @Test
  public void _090_getAnalysis() {
    System.out.println("\n--------------Get Analysis----------------\n");
    assertNotNull(
        "`bundleId` should be initialized at `_030_createBundle_from_source()`", bundleId);
    assertAndPrintGetAnalysisResponse(
        DeepCodeRestApi.getAnalysis(loggedToken, bundleId, null, false));
    System.out.println("\n---- With `Linters` param:\n");
    assertAndPrintGetAnalysisResponse(
        DeepCodeRestApi.getAnalysis(loggedToken, bundleId, null, true));
    System.out.println("\n---- With `severity=2` param:\n");
    assertAndPrintGetAnalysisResponse(
            DeepCodeRestApi.getAnalysis(loggedToken, bundleId, 2, false));
  }

  private void assertAndPrintGetAnalysisResponse(GetAnalysisResponse response) {
    assertNotNull(response);
    System.out.printf(
        "Get Analysis call for test file: \n-----------\n %1$s \n-----------\nreturns Status code: %2$s \n%3$s\n",
        testFileContent, response.getStatusCode(), response);
    //    assertEquals("DONE", response.getStatus());
    assertEquals("Get Analysis request not succeed", 200, response.getStatusCode());
  }
}
