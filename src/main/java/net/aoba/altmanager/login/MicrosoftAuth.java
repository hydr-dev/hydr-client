package net.aoba.altmanager.login;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.logging.LogUtils;
import com.mojang.util.UndashedUuid;

import net.aoba.AobaClient;
import net.aoba.altmanager.Alt;
import net.aoba.mixin.interfaces.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.util.Util;

public class MicrosoftAuth {
	// TODO: I've seen plenty of other clients use Builders for this sort of thing..
	// Perhaps we could try using a builder to abstract HTTP requests a little bit.
	// Get this working first...
	private static MinecraftClient MC = AobaClient.MC;

	private static final String CLIENT_ID = "e92d34e9-149f-40fc-bbf2-5e5d4f3c43f4";
	private static final URI TOKEN_URL = createURI("https://login.live.com/oauth20_token.srf");
	private static final URI XBL_URL = createURI("https://user.auth.xboxlive.com/user/authenticate");
	private static final URI XBLAUTH_URL = createURI("https://xsts.auth.xboxlive.com/xsts/authorize");
	private static final URI XBLOGIN_URL = createURI("https://api.minecraftservices.com/authentication/login_with_xbox");
	private static final URI ENTITLEMENT_URL = createURI("https://api.minecraftservices.com/entitlements/mcstore");
	private static final URI PROFILE_URL = createURI("https://api.minecraftservices.com/minecraft/profile");
	
	private static HttpServer replyServer;

	private static URI createURI(String url) {
		try {
			URI uri = new URI(url);
			return uri;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Logs in to an alt.
	 * @param alt Alt to login to.
	 */
	public static void login(Alt alt) {
		try {
			LogUtils.getLogger().info("Logged in to user account");
			AuthToken token = renewAuthToken(alt.getAuthToken());
			XSTSToken xblToken = getXblToken(token);
			XSTSToken xstsToken = getXSTSToken(xblToken);
			MCAuthToken mcAuthToken = getMCAuthToken(xblToken, xstsToken);
			EntitlementToken entitlement = getEntitlementToken(mcAuthToken);
			
			if(entitlement.hasGame()) {
				ProfileToken profileToken = getProfileToken(mcAuthToken);
	
		        String uuid = profileToken.id;
		        String username = profileToken.name;

		        alt.setAuthToken(token);
		        
				MinecraftClient MC = MinecraftClient.getInstance();
				IMinecraftClient IMC = (IMinecraftClient)MC;
				Session session = new Session(username, UndashedUuid.fromStringLenient(uuid), mcAuthToken.accessToken, Optional.empty(), Optional.empty(), Session.AccountType.MSA);
				IMC.setSession(session);
	
		        UserApiService apiService;
		        apiService = IMC.getAuthenticationService().createUserApiService(session.getAccessToken());
		        IMC.setUserApiService(apiService);
		        IMC.setSocialInteractionsManager(new SocialInteractionsManager(MC, apiService));
		        IMC.setProfileKeys(ProfileKeys.create(apiService, session, MC.runDirectory.toPath()));
		        IMC.setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
		        IMC.setGameProfileFuture(CompletableFuture.supplyAsync(() -> MC.getSessionService().fetchProfile(MC.getSession().getUuidOrNull(), true), Util.getIoWorkerExecutor()));
				
			}else
				throw new Exception("User does not have game.");
		} catch (Exception e) {
			LogUtils.getLogger().error("Could not log in to Microsoft account.");
		}
	}

	public static AuthToken renewAuthToken(AuthToken authToken) throws IOException, InterruptedException {
		String payload = "client_id=" + CLIENT_ID + "&refresh_token=" + authToken.refreshToken + "&grant_type=refresh_token&redirect_uri=http://127.0.0.1:42069";

		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL)
				.build();

		HttpRequest request = HttpRequest.newBuilder(TOKEN_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("Device token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return AuthToken.fromJson(json);
	}
	
	/**
	 * Fetches the Xbl token from Microsoft.
	 * @param authToken The AuthToken containing the access code and refresh code.
	 * @return Xbl token as a XSTSToken object.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static XSTSToken getXblToken(AuthToken authToken) throws IOException, InterruptedException {
		String payload = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + authToken.accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";

		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder(XBL_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json").header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("Xbl token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return XSTSToken.fromJson(json);
	}
	
	/**
	 * Fetches the XSTS token from Microsoft.
	 * @param xblToken The XSTS token previously fetched.
	 * @return XSTS token fetched
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static XSTSToken getXSTSToken(XSTSToken xblToken) throws IOException, InterruptedException {
		String payload = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken.token + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";

		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder(XBLAUTH_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json").header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("XSTS token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return XSTSToken.fromJson(json);
	}
	
	public static MCAuthToken getMCAuthToken(XSTSToken xblToken, XSTSToken xstsToken) throws IOException, InterruptedException {
		String payload = "{\"identityToken\":\"XBL3.0 x=" + xblToken.displayClaims.xui[0].uhs + ";" + xstsToken.token + "\", \"ensureLegacyEnabled\":true}";

		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder(XBLOGIN_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json").header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("MC Auth token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return MCAuthToken.fromJson(json);
	}
	
	public static EntitlementToken getEntitlementToken(MCAuthToken mcAuthToken) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder(ENTITLEMENT_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json").header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + mcAuthToken.accessToken).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("Entitlement token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return EntitlementToken.fromJson(json);
	}
	
	public static ProfileToken getProfileToken(MCAuthToken mcAuthToken) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();

		HttpRequest request = HttpRequest.newBuilder(PROFILE_URL).header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
				.header("Accept", "application/json").header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + mcAuthToken.accessToken).build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		String responseString = response.body();

		int status = response.statusCode();
		if (status != HttpURLConnection.HTTP_OK) {
			throw new IllegalArgumentException("Entitlement token could not be fetched. Invalid status code " + status);
		}

		JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
		return ProfileToken.fromJson(json);
	}
	
	public static void requestAuthToken(Consumer<AuthToken> onDataReceived) {
		boolean success = startServer(onDataReceived);
		if(success) {
			Util.getOperatingSystem().open("https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID
					+ "&response_type=code&redirect_uri=http://127.0.0.1:42069&scope=XboxLive.signin%20offline_access&prompt=select_account");
		}
	}

	public static boolean startServer(Consumer<AuthToken> onDataReceived) {
		try {
			replyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 42069), 0);
			RefreshTokenHandler handler = new RefreshTokenHandler();
			handler.setConsumer(onDataReceived);
			replyServer.createContext("/", handler);
			replyServer.setExecutor(null);
			replyServer.start();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void stopServer() {
		if (replyServer == null)
			return;

		replyServer.stop(0);
		replyServer = null;
	}

	static class RefreshTokenHandler implements HttpHandler {
		private Consumer<AuthToken> onDataReceived;

		@Override
		public void handle(HttpExchange t) throws IOException {
			URI requestURI = t.getRequestURI();
			String query = requestURI.getQuery();
			String[] parameterPairs = query.split("&");

			boolean found = false;
			for (String params : parameterPairs) {
				String[] paramSplit = params.split("=");
				if (paramSplit.length == 2) {
					if (paramSplit[0].equalsIgnoreCase("code")) {
						found = true;
						try {
							AuthToken token = getAccessToken(paramSplit[1]);
							onDataReceived.accept(token);
						} catch (Exception e) {
							LogUtils.getLogger()
									.info("[Aoba] Error occured while fetching Access Tokens from Microsoft");
							onDataReceived.accept(null);
						}

						break;
					}
				}
			}

			if (!found) {
				LogUtils.getLogger().info("[Aoba] Unable to get Access Tokens from Microsoft");
				onDataReceived.accept(null);
			}

			if (replyServer == null)
				return;

			replyServer.stop(0);
			replyServer = null;
		}

		private AuthToken getAccessToken(String code) throws IOException, InterruptedException {
			String payload = "client_id=" + CLIENT_ID + "&code=" + code
					+ "&grant_type=authorization_code&redirect_uri=http://127.0.0.1:42069";

			HttpClient client = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL)
					.build();

			HttpRequest request = HttpRequest.newBuilder(TOKEN_URL).header("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
					.header("Accept", "application/json").header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			String responseString = response.body();

			int status = response.statusCode();
			if (status != HttpURLConnection.HTTP_OK) {
				throw new IllegalArgumentException("Device token could not be fetched. Invalid status code " + status);
			}

			JsonObject json = new Gson().fromJson(responseString, JsonObject.class);
			return AuthToken.fromJson(json);
		}

		public void setConsumer(Consumer<AuthToken> consumer) {
			onDataReceived = consumer;
		}
	}
}
