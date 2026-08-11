package padej.soup.core.server.websocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.server.ServerEndpoints;

public class ServerRedirect {
   private static final int TIMEOUT_SECONDS = 10;
   private final HttpClient httpClient = HttpClient.newHttpClient();

   public String fetchServer() {
      try {
         String redirectUrl = ServerEndpoints.getRedirectUrl();
         LoggerUtil.info("Fetching server from: " + redirectUrl);
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create(redirectUrl)).timeout(Duration.ofSeconds(10L)).GET().build();
         HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
         if (response.statusCode() == 200) {
            String server = response.body().trim();
            if (server.contains(":")) {
               LoggerUtil.info("Received server from redirect: " + server);
               return server;
            }

            LoggerUtil.error("Invalid server format received: " + server);
         } else if (response.statusCode() == 503) {
            LoggerUtil.error("No servers available from redirect (503)");
         } else {
            LoggerUtil.error("Redirect server returned status: " + response.statusCode());
         }
      } catch (Exception var5) {
         LoggerUtil.error("Failed to fetch server: " + var5.getMessage());
      }

      return null;
   }
}
