package com.kloudless.net;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kloudless.Kloudless;
import com.kloudless.exception.APIConnectionException;
import com.kloudless.exception.APIException;
import com.kloudless.exception.AuthenticationException;
import com.kloudless.exception.InvalidRequestException;
import com.kloudless.model.*;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import java.io.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class APIResource extends KloudlessObject {

	public static final Gson GSON = new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.registerTypeAdapter(Data.class, new DataDeserializer())
			.registerTypeAdapter(KloudlessRawJsonObject.class,
					new KloudlessRawJsonObjectDeserializer()).create();

	protected static String className(Class<?> clazz) {
		String className = clazz.getSimpleName().toLowerCase()
				.replace("$", " ");
		return className;
	}

	protected static String singleClassURL(Class<?> clazz) {
		return className(clazz);
	}

	protected static String classURL(Class<?> clazz) {
		HashSet<String> storageClasses = new HashSet<String>() {
			{
				add("file");
				add("folder");
				add("link");
			}
		};

		HashSet<String> teamClasses = new HashSet<String>() {
			{
				add("group");
				add("user");
			}
		};
		
		HashMap<String, String> crmClasses = new HashMap<String, String>() {{		
			put("crmaccount", "account");
			put("crmcontact", "contact");
			put("crmlead", "lead");
			// TODO: fixme
			put("crmopportunity", "opportunitie");
			put("crmcampaign", "campaigns");
			put("crmtask", "tasks");
			// Raw
			put("crmobject", "object");			
		}};
		
		String single = singleClassURL(clazz);
		String prefix = null;
		if (storageClasses.contains(single)) {
			prefix = String.format("%s/%s", "storage", single);
		} else if (teamClasses.contains(single)) {
			prefix = String.format("%s/%s", "team", single);
		} else if (crmClasses.containsKey(single)) {
			prefix = String.format("%s/%s", "crm", crmClasses.get(single));		
		} else {
			prefix = single;
		}
		return String.format("%ss", prefix);
	}

	protected static String instanceURL(Class<?> clazz, String id)
			throws InvalidRequestException {
		try {
			return String.format("%s/%s", classURL(clazz), urlEncode(id));
		} catch (UnsupportedEncodingException e) {
			throw new InvalidRequestException("Unable to encode parameters to "
					+ CHARSET
					+ ". Please contact support@kloudless.com for assistance.",
					null, e);
		}
	}

	protected static String detailURL(Class<?> clazz, String id)
			throws InvalidRequestException {
		try {
			return String.format("%s/%s/contents", classURL(clazz),
					urlEncode(id));
		} catch (UnsupportedEncodingException e) {
			throw new InvalidRequestException("Unable to encode parameters to "
					+ CHARSET
					+ ". Please contact support@kloudless.com for assistance.",
					null, e);
		}
	}

	public static final String CHARSET = "UTF-8";

	private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";

	/*
	 * Set this property to override your environment's default
	 * URLStreamHandler; Settings the property should not be needed in most
	 * environments.
	 */
	private static final String CUSTOM_URL_STREAM_HANDLER_PROPERTY_NAME = "com.kloudless.net.customURLStreamHandler";

	protected enum RequestMethod {
		GET, PATCH, POST, PUT, DELETE
	}

	protected static String urlEncode(String str)
			throws UnsupportedEncodingException {
		// Preserve original behavior that passing null for an object id will
		// lead
		// to us actually making a request to /v1/foo/null
		if (str == null) {
			return null;
		} else {
			return URLEncoder.encode(str, CHARSET);
		}
	}

	private static String urlEncodePair(String k, String v)
			throws UnsupportedEncodingException {
		return String.format("%s=%s", urlEncode(k), urlEncode(v));
	}

	static Map<String, String> addAuthHeaders(Map<String, String> headers,
			String url) {
		Map<String, String> keys = new HashMap<String, String>();
		headers.put("Accept-Charset", CHARSET);
		headers.put("User-Agent", String.format("Kloudless/v1 JavaBindings/%s",
				Kloudless.VERSION));

		if (Kloudless.apiKey != null) {
			keys.put("apiKey", Kloudless.apiKey);
		} else if (Kloudless.developerKey != null) {
			keys.put("developerKey", Kloudless.developerKey);
		} else if (Kloudless.bearerToken != null) {
			keys.put("bearerToken", Kloudless.bearerToken);
		}

		String appUrl = String.format("%s/v%s/%s", Kloudless.getApiBase(),
				Kloudless.apiVersion, Kloudless.APPLICATIONS);

		if (headers.get("Authorization") == null) {
			if (url.startsWith(appUrl)) {
				if (keys.get("developerKey") != null) {
					headers.put(
							"Authorization",
							String.format("DeveloperKey %s",
									keys.get("developerKey")));
				}
			} else {
				if (keys.get("apiKey") != null) {
					headers.put("Authorization",
							String.format("ApiKey %s", keys.get("apiKey")));
				} else if (Kloudless.bearerToken != null) {
					headers.put("Authorization",
							String.format("Bearer %s", Kloudless.bearerToken));
				}
			}
		}

		// debug headers
		String[] propertyNames = { "os.name", "os.version", "os.arch",
				"java.version", "java.vendor", "java.vm.version",
				"java.vm.vendor" };
		Map<String, String> propertyMap = new HashMap<String, String>();
		for (String propertyName : propertyNames) {
			propertyMap.put(propertyName, System.getProperty(propertyName));
		}
		propertyMap.put("bindings.version", Kloudless.VERSION);
		propertyMap.put("lang", "Java");
		propertyMap.put("publisher", "Kloudless");
		headers.put("X-Kloudless-Client-User-Agent", GSON.toJson(propertyMap));
		if (Kloudless.apiVersion != null) {
			headers.put("Kloudless-Version", Kloudless.apiVersion);
		}
		return headers;
	}

	private static java.net.HttpURLConnection createKloudlessConnection(
			String url, Map<String, String> headers) throws IOException {
		URL kloudlessURL = null;
		String customURLStreamHandlerClassName = System.getProperty(
				CUSTOM_URL_STREAM_HANDLER_PROPERTY_NAME, null);
		if (customURLStreamHandlerClassName != null) {
			// instantiate the custom handler provided
			try {
				@SuppressWarnings("unchecked")
				Class<URLStreamHandler> clazz = (Class<URLStreamHandler>) Class
						.forName(customURLStreamHandlerClassName);
				Constructor<URLStreamHandler> constructor = clazz
						.getConstructor();
				URLStreamHandler customHandler = constructor.newInstance();
				kloudlessURL = new URL(null, url, customHandler);
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			} catch (SecurityException e) {
				throw new IOException(e);
			} catch (NoSuchMethodException e) {
				throw new IOException(e);
			} catch (IllegalArgumentException e) {
				throw new IOException(e);
			} catch (InstantiationException e) {
				throw new IOException(e);
			} catch (IllegalAccessException e) {
				throw new IOException(e);
			} catch (InvocationTargetException e) {
				throw new IOException(e);
			}
		} else {
			kloudlessURL = new URL(url);
		}
		java.net.HttpURLConnection conn;
		if (url.startsWith("https://")) {
			conn = (javax.net.ssl.HttpsURLConnection) kloudlessURL
					.openConnection();
		} else {
			conn = (java.net.HttpURLConnection) kloudlessURL.openConnection();
		}
		conn.setConnectTimeout(30 * 1000);
		conn.setReadTimeout(80 * 1000);
		conn.setUseCaches(false);
		for (Map.Entry<String, String> header : addAuthHeaders(headers, url)
				.entrySet()) {
			conn.setRequestProperty(header.getKey(), header.getValue());
		}

		// custom headers
		for (Map.Entry<String, String> header : Kloudless.customHeaders
				.entrySet()) {
			conn.setRequestProperty(header.getKey(), header.getValue());
		}

		return conn;
	}

	private static java.net.HttpURLConnection createGetConnection(String url,
			String query, Map<String, String> headers) throws IOException {
		String getURL;
		if (!query.isEmpty()) {
			if (url.contains("?")) {
				getURL = String.format("%s&%s", url, query);
			} else {
				getURL = String.format("%s?%s", url, query);
			}
		} else {
			getURL = url;
		}
		java.net.HttpURLConnection conn = createKloudlessConnection(getURL,
				headers);
		conn.setRequestMethod("GET");
		return conn;
	}

	private static void allowPatchCommand(java.net.HttpURLConnection conn) {
		Object target = null;
		try {
			if (conn instanceof HttpsURLConnectionImpl) {
				final Field delegate = HttpsURLConnectionImpl.class
						.getDeclaredField("delegate");
				delegate.setAccessible(true);
				target = delegate.get(conn);
			} else {
				target = conn;
			}

			final Field f = HttpURLConnection.class.getDeclaredField("methods");
			f.setAccessible(true);
			int last = 6; // index 6 is TRACE
			// TODO: temp solution to replace trace with patch
			((String[]) f.get(target))[last] = "PATCH";
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// TODO: log
			e.printStackTrace();
		}
	}

	private static java.net.HttpURLConnection createPatchConnection(String url,
			Map<String, Object> params, String query,
			Map<String, String> headers) throws IOException {
		java.net.HttpURLConnection conn = createKloudlessConnection(url,
				headers);
		allowPatchCommand(conn);
		conn.setDoOutput(true);
		conn.setRequestMethod("PATCH");
		conn.setRequestProperty("Content-Type", "application/json");
		OutputStream output = null;
		try {
			if (params.containsKey("body")) {
				output = conn.getOutputStream();
				output.write((byte[]) params.get("body"));
			} else {
				output = conn.getOutputStream();
				output.write(GSON.toJson(params).getBytes());
			}
		} finally {
			if (output != null) {
				output.close();
			}
		}
		return conn;
	}

	private static java.net.HttpURLConnection createPutConnection(String url,
			Map<String, Object> params, String query,
			Map<String, String> headers) throws IOException {
		java.net.HttpURLConnection conn = createKloudlessConnection(url,
				headers);
		conn.setDoOutput(true);
		conn.setRequestMethod("PUT");
		OutputStream output = null;
		// put in body the data for a PUT
		try {
			if (params.containsKey("file")) {
				java.io.File file = (File) params.get("file");
				final int partNum = (int) params.get("part_number");
				final long partSize = (long) params.get("part_size");
				long endPosition = partNum * partSize;
				long startPos = 0;
				int readSize = 8192;
				if (endPosition > file.length()) {
					startPos = endPosition - partSize;
					endPosition = file.length();
				} else {
					startPos = endPosition - partSize;
				}
				conn.setRequestProperty("Content-Type",
						"application/octet-stream");

				long contentLength = endPosition - startPos;
				conn.setRequestProperty("Content-Length",
						String.valueOf(contentLength));
				conn.setFixedLengthStreamingMode(contentLength);

				try (BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(file));
						BufferedOutputStream bos = new BufferedOutputStream(
								conn.getOutputStream())) {

					long skip = 0;

					// TODO: not sure this while loop is needed since haven't
					// encountered
					// skip < startPos issue
					while ((skip = bis.skip(startPos)) < startPos) {
						skip = bis.skip(startPos - skip);
					}

					byte[] bytes = new byte[readSize];

					int read = bis.read(bytes, 0, readSize);
					int bytesSent = 0;
					while (read > 0 && bytesSent < contentLength) {
						bos.write(bytes, 0, read);
						bytesSent += read;
						if ((contentLength - bytesSent) < readSize) {
							readSize = (int) (contentLength - bytesSent);
							if (readSize > 0) {
								read = bis.read(bytes, 0, readSize);
								bos.write(bytes, 0, read);
							}
							read = -1;
						} else {
							read = bis.read(bytes, 0, readSize);
						}
					}
					if (read > 0) {
						bos.write(bytes, 0, read);
					}
					bos.flush();
				} catch (IOException ex) {
					throw ex;
				}
			} else if (params.containsKey("body")) {
				output = conn.getOutputStream();
				output.write((byte[]) params.get("body"));
			} else {
				conn.setRequestProperty("Content-Type", "application/json");
				output = conn.getOutputStream();
				output.write(GSON.toJson(params).getBytes());
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (output != null) {
				output.close();
			}
		}
		return conn;
	}

	private static java.net.HttpURLConnection createPostConnection(String url,
			Map<String, Object> params, String query,
			Map<String, String> headers) throws IOException {
		java.net.HttpURLConnection conn = createKloudlessConnection(url,
				headers);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		if (params == null) {
			params = new HashMap<>();
		} // hacky fix for create() APIKeys

		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			if (params.containsKey("file")) {
				conn.setRequestProperty("X-Kloudless-Metadata",
						(String) params.get("metadata"));
				conn.setRequestProperty("Content-Type",
						"application/octet-stream");
				// get file path
				File file = (java.io.File) params.get("file");
				bis = new BufferedInputStream(new FileInputStream(file));
				long fileSize = file.length();
				conn.setRequestProperty("Content-Length",
						String.valueOf(fileSize));
				conn.setFixedLengthStreamingMode(fileSize);

				bos = new BufferedOutputStream(conn.getOutputStream());

				int readSize = 8192;
				byte[] bytes = null;
				if (fileSize < readSize) {
					readSize = (int) fileSize;
				}

				bytes = new byte[readSize];
				int read = bis.read(bytes, 0, readSize);
				int hasRead = read;
				int mb = 0;
				int oneMb = 1024 * 1024;
				while (read != -1) {
					bos.write(bytes, 0, read);
					// TODO: should replace following line with listeners
					if (hasRead >= oneMb) {
						hasRead = 0;
						mb += 1;
						System.out.println(mb + " mb sent!");
					}
					read = bis.read(bytes, 0, readSize);
				}
			} else if (params.containsKey("body")) {
				conn.setRequestProperty("X-Kloudless-Metadata",
						(String) params.get("metadata"));
				conn.setRequestProperty("Content-Type",
						"application/octet-stream");				
				bos = new BufferedOutputStream(conn.getOutputStream());
				bos.write((byte[]) params.get("body"));
				long fileSize = ((byte []) params.get("body")).length;
				conn.setRequestProperty("Content-Length",
						String.valueOf(fileSize));
				conn.setFixedLengthStreamingMode(fileSize);
			} else {
				conn.setRequestProperty("Content-Type",
						String.format("application/json;charset=%s", CHARSET));
				bos = new BufferedOutputStream(conn.getOutputStream());
				String json = GSON.toJson(params);
				bos.write(json.getBytes());
			}
		} finally {
			if (bos != null) {
				bos.flush();
				bos.close();
			}
			if (bis != null) {
				bis.close();
			}
		}
		return conn;
	}

	private static java.net.HttpURLConnection createDeleteConnection(
			String url, String query, Map<String, String> headers)
			throws IOException {
		String deleteUrl = String.format("%s?%s", url, query);
		java.net.HttpURLConnection conn = createKloudlessConnection(deleteUrl,
				headers);
		conn.setRequestMethod("DELETE");
		return conn;
	}

	private static String createQuery(RequestMethod method,
			Map<String, Object> params) throws UnsupportedEncodingException,
			InvalidRequestException {
		Map<String, String> flatParams = flattenParams(params);
		StringBuilder queryStringBuffer = new StringBuilder();

		// PATCH and POST puts the parameters into the body
		if (method != RequestMethod.PATCH && method != RequestMethod.POST) {
			for (Map.Entry<String, String> entry : flatParams.entrySet()) {
				if (queryStringBuffer.length() > 0) {
					queryStringBuffer.append("&");
				}
				queryStringBuffer.append(urlEncodePair(entry.getKey(),
						entry.getValue()));
			}
		}
		return queryStringBuffer.toString();
	}

	private static Map<String, String> flattenParams(Map<String, Object> params)
			throws InvalidRequestException {
		if (params == null) {
			return new HashMap<String, String>();
		}
		Map<String, String> flatParams = new HashMap<String, String>();
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map<?, ?>) {
				Map<String, Object> flatNestedMap = new HashMap<String, Object>();
				Map<?, ?> nestedMap = (Map<?, ?>) value;
				for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
					flatNestedMap.put(
							String.format("%s[%s]", key, nestedEntry.getKey()),
							nestedEntry.getValue());
				}
				flatParams.putAll(flattenParams(flatNestedMap));
			} else if ("".equals(value)) {
				throw new InvalidRequestException("You cannot set '" + key
						+ "' to an empty string. "
						+ "We interpret empty strings as null in requests. "
						+ "You may set '" + key
						+ "' to null to delete the property.", key, null);
			} else if (value == null) {
				flatParams.put(key, "");
			} else if (value != null) {
				flatParams.put(key, value.toString());
			}
		}
		return flatParams;
	}

	private static class Error {
		@SuppressWarnings("unused")
		String type;

		String message;

		@SuppressWarnings("unused")
		String code;

		String param;
	}

	private static String getResponseBody(InputStream responseStream,
			ByteArrayOutputStream byteArrayOut) throws IOException {
		// \A is the beginning of
		// the stream boundary
		// String rBody = new Scanner(responseStream,
		// CHARSET).useDelimiter("\\A")
		// .next(); //

		BufferedInputStream bufferedInputStream = new BufferedInputStream(
				responseStream);
		int c;
		while ((c = bufferedInputStream.read()) != -1) {
			byteArrayOut.write(c);
		}

		String rBody = byteArrayOut.toString(CHARSET);
		responseStream.close();

		return rBody;
	}

	private static KloudlessResponse makeURLConnectionRequest(
			APIResource.RequestMethod method, Map<String, Object> params,
			String url, String query, Map<String, String> headers)
			throws APIConnectionException {
		java.net.HttpURLConnection conn = null;

		try {
			switch (method) {
			case GET:
				conn = createGetConnection(url, query, headers);
				break;
			case PATCH:
				conn = createPatchConnection(url, params, query, headers);
				break;
			case PUT:
				conn = createPutConnection(url, params, query, headers);
				break;
			case POST:
				conn = createPostConnection(url, params, query, headers);
				break;
			case DELETE:
				conn = createDeleteConnection(url, query, headers);
				break;
			default:
				throw new APIConnectionException(
						String.format(
								"Unrecognized HTTP method %s. "
										+ "This indicates a bug in the Kloudless bindings. Please contact "
										+ "support@kloudless.com for assistance.",
								method));
			}
			// trigger the request
			int rCode = conn.getResponseCode();
			String rBody = null;
			Map<String, List<String>> rHeaders;
			ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

			// convert responseBody and save responseStream
			if (rCode >= 200 && rCode < 300) {
				rBody = getResponseBody(conn.getInputStream(), byteArrayOut);
			} else {
				rBody = getResponseBody(conn.getErrorStream(), byteArrayOut);
			}

			rHeaders = conn.getHeaderFields();
			return new KloudlessResponse(rCode, rBody, rHeaders, byteArrayOut);

		} catch (IOException e) {
			throw new APIConnectionException(
					String.format(
							"Could not connect to Kloudless (%s). "
									+ "Please check your internet connection and try again. If this problem persists,"
									+ "you should check Kloudless's service status at https://twitter.com/KloudlessAPI,"
									+ " or let us know at support@kloudless.com.",
							Kloudless.getApiBase()), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	protected static KloudlessResponse request(
			APIResource.RequestMethod method, String path,
			Map<String, Object> params, Map<String, String> headers)
			throws AuthenticationException, InvalidRequestException,
			APIConnectionException, APIException {
		String originalDNSCacheTTL = null;
		Boolean allowedToSetTTL = true;

		try {
			originalDNSCacheTTL = java.security.Security
					.getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
			// disable DNS cache
			java.security.Security
					.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
		} catch (SecurityException se) {
			allowedToSetTTL = false;
		}

		try {
			return _request(method, path, params, headers);
		} finally {
			if (allowedToSetTTL) {
				if (originalDNSCacheTTL == null) {
					// value unspecified by implementation
					// DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
					java.security.Security.setProperty(
							DNS_CACHE_TTL_PROPERTY_NAME, "-1");
				} else {
					java.security.Security.setProperty(
							DNS_CACHE_TTL_PROPERTY_NAME, originalDNSCacheTTL);
				}
			}
		}
	}

	protected static KloudlessResponse _request(
			APIResource.RequestMethod method, String path,
			Map<String, Object> params, Map<String, String> headers)
			// Map<String, String> keys to have the .length() method
			throws AuthenticationException, InvalidRequestException,
			APIConnectionException, APIException {

		if (headers == null) {
			headers = new HashMap<String, String>();
		}

		if ((Kloudless.apiKey == null || Kloudless.apiKey.length() == 0)
				&& (headers.get("apiKey") == null || headers.get("apiKey")
						.length() == 0)
				&& (Kloudless.developerKey == null || Kloudless.developerKey
						.length() == 0)
				&& (headers.get("developerKey") == null || headers.get(
						"developerKey").length() == 0)
				&& (Kloudless.bearerToken == null || Kloudless.bearerToken
						.length() == 0)
				&& (headers.get("bearerToken") == null || headers.get(
						"bearerToken").length() == 0)
				&& headers.get("Authorization") == null) {
			throw new AuthenticationException(
					"No API Key, Developer Key or Bearer Token provided. (HINT: set your API key using 'Kloudless.apiKey = <API-KEY>'"
							+ " or 'Kloudless.developerKey = <DEV-KEY>'"
							+ " or 'Kloudless.bearerToken = <OAUTH 2.0 BEARER TOKEN>')."
							+ "You can generate API keys from the Kloudless web interface. "
							+ "See https://developers.kloudless.com/docs for details or email support@kloudless.com if you have questions.");
		}

		String query;
		String url = String.format("%s/v%s/%s", Kloudless.getApiBase(),
				Kloudless.apiVersion, path);

		System.out.format("url: %s\n", url);

		try {
			query = createQuery(method, params);
		} catch (UnsupportedEncodingException e) {
			throw new InvalidRequestException("Unable to encode parameters to "
					+ CHARSET
					+ ". Please contact support@kloudless.com for assistance.",
					null, e);
		}

		KloudlessResponse response;
		try {
			// HTTPSURLConnection verifies SSL cert by default
			response = makeURLConnectionRequest(method, params, url, query,
					headers);
		} catch (ClassCastException ce) {
			// appengine doesn't have HTTPSConnection, use URLFetch API
			String appEngineEnv = System.getProperty(
					"com.google.appengine.runtime.environment", null);
			if (appEngineEnv != null) {
				response = makeAppEngineRequest(method, params, url, query,
						headers);
			} else {
				// non-appengine ClassCastException
				throw ce;
			}
		}
		return response;
	}

	protected static void handleAPIError(String rBody, int rCode)
			throws InvalidRequestException, AuthenticationException,
			APIException {
		APIResource.Error error;
		try {
			error = GSON.fromJson(rBody, APIResource.Error.class);
		} catch (JsonSyntaxException e) {
			error = new APIResource.Error();
			error.message = rBody;
			error.param = "";
		}

		switch (rCode) {
		case 400:
			throw new InvalidRequestException(error.message, error.param, null);
		case 404:
			throw new InvalidRequestException(error.message, error.param, null);
		case 403:
			throw new AuthenticationException(error.message);
		case 401:
			throw new AuthenticationException(error.message);
		default:
			throw new APIException(error.message, null);
		}
	}

	/*
	 * This is slower than usual because of reflection but avoids having to
	 * maintain AppEngine-specific JAR
	 */
	private static KloudlessResponse makeAppEngineRequest(RequestMethod method,
			Map<String, Object> params, String url, String query,
			Map<String, String> headers) throws APIException {
		String unknownErrorMessage = "Sorry, an unknown error occurred while trying to use the "
				+ "Google App Engine runtime. Please contact support@kloudless.com for assistance.";
		try {
			if (method == RequestMethod.GET || method == RequestMethod.DELETE) {
				url = String.format("%s?%s", url, query);
			}
			URL fetchURL = new URL(url);

			Class<?> requestMethodClass = Class
					.forName("com.google.appengine.api.urlfetch.HTTPMethod");
			Object httpMethod = requestMethodClass.getDeclaredField(
					method.name()).get(null);

			Class<?> fetchOptionsBuilderClass = Class
					.forName("com.google.appengine.api.urlfetch.FetchOptions$Builder");
			Object fetchOptions = null;
			try {
				fetchOptions = fetchOptionsBuilderClass.getDeclaredMethod(
						"validateCertificate").invoke(null);
			} catch (NoSuchMethodException e) {
				System.err
						.println("Warning: this App Engine SDK version does not allow verification of SSL certificates;"
								+ "this exposes you to a MITM attack. Please upgrade your App Engine SDK to >=1.5.0. "
								+ "If you have questions, contact support@kloudless.com.");
				fetchOptions = fetchOptionsBuilderClass.getDeclaredMethod(
						"withDefaults").invoke(null);
			}

			Class<?> fetchOptionsClass = Class
					.forName("com.google.appengine.api.urlfetch.FetchOptions");

			// GAE requests can time out after 60 seconds, so make sure we leave
			// some time for the application to handle a slow Kloudless
			fetchOptionsClass.getDeclaredMethod("setDeadline",
					java.lang.Double.class)
					.invoke(fetchOptions, new Double(55));

			Class<?> requestClass = Class
					.forName("com.google.appengine.api.urlfetch.HTTPRequest");

			Object request = requestClass.getDeclaredConstructor(URL.class,
					requestMethodClass, fetchOptionsClass).newInstance(
					fetchURL, httpMethod, fetchOptions);

			Map<String, String> extraHeaders = addAuthHeaders(headers, url);
			if (query == null || query.length() == 0) {
				// This is a request that uses a json payload to make a request.
				// look at createQuery.
				requestClass.getDeclaredMethod("setPayload", byte[].class)
						.invoke(request, GSON.toJson(params).getBytes());
				extraHeaders.put("Content-Type",
						String.format("application/json;charset=%s", CHARSET));
			} else {
				requestClass.getDeclaredMethod("setPayload", byte[].class)
						.invoke(request, query.getBytes());
			}

			for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
				Class<?> httpHeaderClass = Class
						.forName("com.google.appengine.api.urlfetch.HTTPHeader");
				Object reqHeader = httpHeaderClass.getDeclaredConstructor(
						String.class, String.class).newInstance(
						header.getKey(), header.getValue());
				requestClass.getDeclaredMethod("addHeader", httpHeaderClass)
						.invoke(request, reqHeader);
			}

			// custom headers
			for (Map.Entry<String, String> header : Kloudless.customHeaders
					.entrySet()) {
				Class<?> httpHeaderClass = Class
						.forName("com.google.appengine.api.urlfetch.HTTPHeader");
				Object reqHeader = httpHeaderClass.getDeclaredConstructor(
						String.class, String.class).newInstance(
						header.getKey(), header.getValue());
				requestClass.getDeclaredMethod("setHeader", httpHeaderClass)
						.invoke(request, reqHeader);
			}

			Class<?> urlFetchFactoryClass = Class
					.forName("com.google.appengine.api.urlfetch.URLFetchServiceFactory");
			Object urlFetchService = urlFetchFactoryClass.getDeclaredMethod(
					"getURLFetchService").invoke(null);

			Method fetchMethod = urlFetchService.getClass().getDeclaredMethod(
					"fetch", requestClass);
			fetchMethod.setAccessible(true);
			Object response = fetchMethod.invoke(urlFetchService, request);

			// TODO: Convert headers and populate fields.
			// List<Object> headersList = (List<Object>) response.getClass()
			// .getDeclaredMethod("getHeaders").invoke(response);
			Map<String, List<String>> rHeaders = new HashMap<String, List<String>>();

			int responseCode = (Integer) response.getClass()
					.getDeclaredMethod("getResponseCode").invoke(response);

			String body = "";
			byte[] responseBytes = (byte[]) response.getClass()
					.getDeclaredMethod("getContent").invoke(response);
			ByteArrayOutputStream responseStream = new ByteArrayOutputStream(0);
			if (responseBytes != null) {
				body = new String(responseBytes, CHARSET);
				responseStream = new ByteArrayOutputStream(responseBytes.length);
				responseStream.write(responseBytes, 0, responseBytes.length);
			}

			return new KloudlessResponse(responseCode, body, rHeaders,
					responseStream);
		} catch (InvocationTargetException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (MalformedURLException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (NoSuchFieldException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (SecurityException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (NoSuchMethodException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (ClassNotFoundException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (IllegalArgumentException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (IllegalAccessException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (InstantiationException e) {
			throw new APIException(unknownErrorMessage, e);
		} catch (UnsupportedEncodingException e) {
			throw new APIException(unknownErrorMessage, e);
		}
	}

}
