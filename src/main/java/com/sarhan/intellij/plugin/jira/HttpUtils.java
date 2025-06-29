/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarhan.intellij.plugin.jira;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling HTTP operations. Provides methods to perform HTTP GET
 * requests, create connections, read responses, and log API error details.
 *
 * @author Akmal Sarhan
 */
public final class HttpUtils {

	private HttpUtils() {
	}

	private static final Logger LOG = Logger.getInstance(HttpUtils.class);

	private static final int HTTP_OK = 200;

	private static final String BEARER_PREFIX = "Bearer ";

	public static Optional<String> doGet(@NotNull String apiUrl, @NotNull String authToken)
			throws IOException, URISyntaxException {
		if (StringUtils.isBlank(authToken)) {
			return Optional.empty();
		}
		HttpURLConnection connection = createConnection(apiUrl, authToken);

		try {
			int responseCode = connection.getResponseCode();
			if (responseCode == HTTP_OK) {
				return Optional.of(readResponse(connection.getInputStream()));
			}
			else {
				logApiError(connection, responseCode);
				return Optional.empty();
			}
		}
		finally {
			connection.disconnect();
		}
	}

	public static HttpURLConnection createConnection(@NotNull String apiUrl, @NotNull String authToken)
			throws IOException, URISyntaxException {
		URL url = new URI(apiUrl).toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", BEARER_PREFIX + authToken);
		connection.setRequestProperty("Accept", "application/json");
		connection.setConnectTimeout(10000); // 10 seconds
		connection.setReadTimeout(30000); // 30 seconds
		return connection;
	}

	public static String readResponse(@NotNull InputStream inputStream) throws IOException {
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
		}
		return response.toString();
	}

	public static void logApiError(@NotNull HttpURLConnection connection, int responseCode) {
		LOG.warn("API request failed with response code: " + responseCode);
		try (InputStream errorStream = connection.getErrorStream()) {
			if (errorStream != null) {
				String errorResponse = readResponse(errorStream);
				LOG.warn("Error response: " + errorResponse);
			}
		}
		catch (IOException ex) {
			LOG.debug("Could not read error response", ex);
		}
	}

}
