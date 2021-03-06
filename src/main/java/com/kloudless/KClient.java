package com.kloudless;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.kloudless.exception.APIConnectionException;
import com.kloudless.exception.APIException;
import com.kloudless.exception.AuthenticationException;
import com.kloudless.exception.InvalidRequestException;
import com.kloudless.model.Account;
import com.kloudless.model.AccountCollection;
import com.kloudless.model.Application;
import com.kloudless.model.ApplicationCollection;
import com.kloudless.model.CRMAccount;
import com.kloudless.model.CRMObject;
import com.kloudless.model.CRMObjectCollection;
import com.kloudless.model.EventCollection;
import com.kloudless.model.File;
import com.kloudless.model.Folder;
import com.kloudless.model.Group;
import com.kloudless.model.GroupCollection;
import com.kloudless.model.Link;
import com.kloudless.model.LinkCollection;
import com.kloudless.model.MetadataCollection;
import com.kloudless.model.Permission;
import com.kloudless.model.PermissionCollection;
import com.kloudless.model.Property;
import com.kloudless.model.PropertyCollection;
import com.kloudless.model.User;
import com.kloudless.model.UserCollection;
import com.kloudless.net.APIResource;
import com.kloudless.net.KloudlessResponse;

/**
 * The KClient class is a wrapper around the static methods of the
 * Kloudless Java SDK that takes in a specific bearer token and account ID.
 * 
 * There is a hierarchy of abstraction from making requests based on a specific
 * Kloudless Object down to a raw request to the Kloudless API.
 * 
 * All methods are public so that a developer can choose which degree of
 * abstraction fits their use case.
 * 
 * This class implements the general methods:
 * 	- all
 * 	- retrieve
 * 	- create
 * 	- save
 * 	- update
 * 	- delete
 * 
 * The class includes an authenticatedRequest method that will automatically
 * add any custom headers and the bearer token authorization to a request.
 * 
 * Finally, the class includes a rawRequest method to make any arbitrary
 * request to the Kloudless API.
 */
public class KClient extends APIResource {

	protected String bearerToken = null;
	protected String accountId = null;
	protected HashMap<String, String> additionalHeaders = null;
	protected Gson gson = new Gson();
	
	/*
	 * Main Constructor
	 * 
	 * @param bearerToken - token used for OAuth 2.0
	 * @param accountId - ID of the account
	 */
	public KClient(String bearerToken, String accountId,
			HashMap<String, String> customHeaders) {
		this.bearerToken = bearerToken;
		this.accountId = accountId;
		this.additionalHeaders = new HashMap<String, String>();
		if (customHeaders != null) {
			this.additionalHeaders.putAll(customHeaders);			
		}
	}

	/**
	 * Makes a Kloudless API Request to create a resource. Pass in any of the
	 * com.kloudless.model objects that correspond with a Kloudless Object.
	 * Specify any query parameters or body parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param bodyParams
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <S, T> T create(Map<String, String> queryParams, Class<S> clazz,
			Map<String, Object> bodyParams) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		return (T) this.authenticatedRequest(null, "POST",
				queryParams, this.formatPath(clazz, null, null), bodyParams, clazz);
	}

	/**
	 * Makes a Kloudless API Request to retrieve all resources of a
	 * specific Kloudless object (com.kloudless.model). The corresponding
	 * ObjectCollection of resources will be returned. Specify any query
	 * parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <S, T> T all(Map<String, String> queryParams, Class<S> clazz)
			throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		Class<T> responseClazz = null;
		if (clazz == Account.class) {
			responseClazz = (Class<T>) AccountCollection.class;
		} else if (clazz == Application.class) {
			responseClazz = (Class<T>) ApplicationCollection.class;
		} else if (clazz == Link.class) {
			responseClazz = (Class<T>) LinkCollection.class;
		} else if (clazz == CRMAccount.class || clazz == CRMObject.class) {
			responseClazz = (Class<T>) CRMObjectCollection.class;
		}
		
		return (T) this.authenticatedRequest(null, "GET",
				queryParams, this.formatPath(clazz, null, null), null, responseClazz);
	}
	
	/**
	 * Makes a Kloudless API Request to retrieve a resource's metadata.
	 * Requires the Object Class and Object ID. Specify any query parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param id
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public <S> S retrieve(Map<String, String> queryParams, Class<S> clazz,
			String id) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		return (S) this.authenticatedRequest(null, "GET",
				queryParams, this.formatPath(clazz, id, null), null, clazz);
	}	

	/**
	 * Makes a Kloudless API Request to update a resource's metadata.
	 * Requires the Object Class and Object ID. Specify any query parameters
	 * and the corresponding fields changed within the body parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param id
	 * @param bodyParams
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <S, T> T update(Map<String, String> queryParams, Class<S> clazz,
			String id, Map<String, Object> bodyParams) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		return (T) this.authenticatedRequest(null, "PATCH", queryParams,
				this.formatPath(clazz, id, null), bodyParams, clazz);
	}	

	/**
	 * Makes a Kloudless API Request to save a resource's contents.
	 * Requires the Object Class and Object ID. Specify any query parameters
	 * and the corresponding fields changed within the body parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param id
	 * @param bodyParams
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <S, T> T save(Map<String, String> queryParams, Class<S> clazz,
			String id, Map<String, Object> bodyParams) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		return (T) this.authenticatedRequest(null, "PUT", queryParams,
				this.formatPath(clazz, null, null), bodyParams, clazz);
	}	
	
	/**
	 * Makes a Kloudless API Request to delete a resource.
	 * Requires the Object Class and Object ID. Specify any query parameters.
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param id
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public <S> KloudlessResponse delete(Map<String, String> queryParams, Class<S> clazz,
			String id) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		return (KloudlessResponse) this.authenticatedRequest(null, "DELETE",
				queryParams, this.formatPath(clazz, id, null), null, null);
	}

	/**
	 * Returns a formatted path to the resource url of the API request.
	 * Converts the class name to the correct url route. The detail parameter
	 * is used as a suffix to append to any instance/class url.
	 * 
	 * @param clazz
	 * @param id
	 * @param detail
	 * @return
	 * @throws InvalidRequestException
	 */
	protected <T> String formatPath(Class<T> clazz, String id, String detail)
			throws InvalidRequestException {

		String path;
		if (clazz == Account.class || clazz == null) {
			if (detail == null) {
				path = String.format("%s",
						instanceURL(Account.class, this.accountId));				
				
			} else {
				path = String.format("%s/%s",
						instanceURL(Account.class, this.accountId), detail);				
			}
			
		} else if (id == null) {
			path = String.format("%s/%s",
					instanceURL(Account.class, this.accountId),
					classURL(clazz));			
		} else if (detail == null) {
			path = String.format("%s/%s",
					instanceURL(Account.class, this.accountId),
					instanceURL(clazz, id));
		} else {
			path = String.format("%s/%s/%s",
					instanceURL(Account.class, this.accountId),
					instanceURL(clazz, id), detail);
		}
		return path;
	}
	
	/**
	 * Makes a request to the Kloudless API with provided headers,
	 * method, query parameters, path, body parameters, and converts
	 * to the desired response class.
	 * 
	 * The request will add any authorization headers based on the
	 * bearerToken and customHeaders.
	 * 
	 * @param headers
	 * @param method
	 * @param queryParams
	 * @param path
	 * @param bodyParams
	 * @param responseClazz
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <T> T authenticatedRequest(Map<String, String> headers,
			String method, Map<String, String> queryParams,
			String path, Map<String, Object> bodyParams, Class<T> responseClazz)
			throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		
		headers.put("Authorization",
				String.format("Bearer %s", this.bearerToken));

		if (this.additionalHeaders.size() > 0) {
			headers.putAll(this.additionalHeaders);
		}
		
		KloudlessResponse response = this.rawRequest(headers, method, queryParams, path, bodyParams);
		int rCode = response.getResponseCode();
		String rBody = response.getResponseBody();
		if (rCode < 200 || rCode >= 300) {
			handleAPIError(rBody, rCode);
		}
		
		if (responseClazz != null) {
			return GSON.fromJson(rBody, responseClazz);			
		} else {
			return (T) response;
		}
	}
	
	/**
	 * Exposes the underlying request mechanism, so the client can
	 * make a raw request to the Kloudless API.
	 * 
	 * @param headers
	 * @param method
	 * @param queryParams
	 * @param path
	 * @param bodyParams
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public KloudlessResponse rawRequest(Map<String, String> headers,
			String method, Map<String, String> queryParams,
			String path, Map<String, Object> bodyParams) throws APIException,
			AuthenticationException, InvalidRequestException,
			APIConnectionException, UnsupportedEncodingException {

		String requestPath;
		if (queryParams == null) {
			requestPath = path;
		} else {
			StringBuilder queryParamString = new StringBuilder("?");
			for (String key : queryParams.keySet()) {
				if (queryParamString.length() != 1) {
					queryParamString.append("&");
				}
				queryParamString.append(String.format("%s=%s", urlEncode(key),
						urlEncode(queryParams.get(key))));
			}
			requestPath = String.format("%s%s", path, queryParamString);
		}
		
		RequestMethod httpMethod = RequestMethod.GET;
		switch (method.toUpperCase()) {
		case "GET":
			httpMethod = RequestMethod.GET;
			break;
		case "POST":
			httpMethod = RequestMethod.POST;
			break;
		case "PATCH":
			httpMethod = RequestMethod.PATCH;
			break;
		case "PUT":
			httpMethod = RequestMethod.PUT;
			break;
		case "DELETE":
			httpMethod = RequestMethod.DELETE;
			break;
		default:
			httpMethod = RequestMethod.GET;
			break;
		}
		
		return request(httpMethod, requestPath, bodyParams, headers);
	}
	
	
	/***
	 * Common Storage Methods
	 */

	/**
	 * Makes a request to the commonly used contents endpoint to retrieve
	 * the contents of a resource (usually File/Folder).
	 * 
	 * @param queryParams
	 * @param clazz
	 * @param id
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public <S, T> T contents(Map<String, String> queryParams, Class<S> clazz,
			String id) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {
		
		String path = String.format("%s/%s",
				instanceURL(Account.class, this.accountId),
				detailURL(clazz, id));

		Class<T> responseClazz = null;
		if (clazz == Folder.class) {
			responseClazz = (Class<T>) MetadataCollection.class;
		}

		return (T) this.authenticatedRequest(null, "GET", queryParams, path,
				null, responseClazz);
	}
	
	/**
	 * Makes a request to upload a file easily.
	 * 
	 * @param parent_id
	 * @param name
	 * @param overwrite
	 * @param path - path to a file
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public File uploadFile(String parent_id, String name, boolean overwrite,
			String path) throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {
		java.io.File file = new java.io.File(path);
		HashMap<String, Object> fileParams = new HashMap<String, Object>();
		HashMap<String, String> metadata = new HashMap<String, String>();
		HashMap<String, String> queryParams = new HashMap<String, String>();
		if (overwrite) {
			queryParams.put("overwrite", "true");
		}
		metadata.put("parent_id", parent_id);
		metadata.put("name", name);
		fileParams.put("metadata", gson.toJson(metadata));
		fileParams.put("file", file);
		File newFile = this.create(null, File.class, fileParams);
		return newFile;
	}
	
	/**
	 * Makes a request to retrieve permissions for a specific file or folder.
	 * 
	 * @param id
	 * @param clazz
	 * @return
	 * @throws InvalidRequestException
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public <T> PermissionCollection listPermissions(String id, Class<T> clazz)
			throws InvalidRequestException, APIException,
			AuthenticationException, APIConnectionException, UnsupportedEncodingException {
		String path = this.formatPath(clazz, id, "permissions");		
		return (PermissionCollection) this.authenticatedRequest(null, "GET",
				null, path, null, PermissionCollection.class);
	}
	
	/**
	 * Makes a request to update permissions of a specific file or folder.
	 * Overwrite makes use of a PUT request instead of a PATCH.
	 * A PermissionCollection is used for better usability.
	 * 
	 * @param id
	 * @param clazz
	 * @param overwrite
	 * @param newPermissions
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public <T> PermissionCollection setPermissions(String id, Class<T> clazz,
			boolean overwrite, PermissionCollection newPermissions) throws APIException, AuthenticationException, InvalidRequestException, APIConnectionException, UnsupportedEncodingException {
		String path = this.formatPath(clazz, id, "permissions");
		String method = overwrite ? "PUT" : "PATCH";
		
		HashMap<String, Object> bodyParams = new HashMap<String, Object>();
		bodyParams.put("body", gson.toJson(newPermissions.permissions)
				.toString().getBytes());
		
		return (PermissionCollection) this.authenticatedRequest(null, method,
				null, path, bodyParams, PermissionCollection.class);
	}	

	/***
	 * Common CRM Methods
	 */
	
	/**
	 * Makes a request to search for CRM content based on the query
	 * and lang.
	 * 
	 * @param query
	 * @param lang
	 * @param page_size
	 * @param page
	 * @return
	 * @throws APIConnectionException 
	 * @throws InvalidRequestException 
	 * @throws AuthenticationException 
	 * @throws APIException 
	 * @throws UnsupportedEncodingException 
	 */
	public CRMObjectCollection crmSearch(String query, String lang,
			int page_size, String page) throws APIException, AuthenticationException, InvalidRequestException, APIConnectionException, UnsupportedEncodingException {
		
		String path = this.formatPath(null, null, "crm/search");
		
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("q", query);
		queryParams.put("lang", lang);
		queryParams.put("page_size", String.valueOf(page_size));
		queryParams.put("page", page);

		return (CRMObjectCollection) this.authenticatedRequest(null, "GET",
				queryParams, path, null, CRMObjectCollection.class);
	}
	
	public CRMObjectCollection crmSchema(String type, int page_size, String page)
			throws InvalidRequestException, APIException,
			AuthenticationException, APIConnectionException,
			UnsupportedEncodingException {

		String path;
		HashMap<String, String> queryParams = new HashMap<String, String>();
		if (type == null || type.isEmpty()) {
			path = this.formatPath(null, null, "crm/schemas");
			if (page != null && !page.isEmpty()) {
				queryParams.put("page_size", String.valueOf(page_size));
				queryParams.put("page", page);				
			}
		} else {
			path = this.formatPath(null, null,
					String.format("crm/schemas/%s", type));			
		}

		return (CRMObjectCollection) this.authenticatedRequest(null, "GET",
				queryParams, path, null, CRMObjectCollection.class);
	}
	
	/***
	 * Common Event Methods
	 */
	
	/**
	 * Makes a request to the commonly used events endpoint to retrieve
	 * events of the account.
	 * 
	 * @param queryParams
	 * @return
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws InvalidRequestException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException 
	 */
	public EventCollection events(Map<String, String> queryParams)
			throws APIException, AuthenticationException,
			InvalidRequestException, APIConnectionException, UnsupportedEncodingException {

		String path = String.format("%s/events",
				instanceURL(Account.class, this.accountId));

		return (EventCollection) this.authenticatedRequest(null, "GET",
				queryParams, path, null, EventCollection.class);
	}

	/***
	 * Common Team Methods
	 */

	/**
	 * Makes a request for all of the user's group memberships.
	 * 
	 * @param userId
	 * @return
	 * @throws InvalidRequestException
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException
	 */
	public GroupCollection teamMemberships(String userId)
			throws InvalidRequestException, APIException,
			AuthenticationException, APIConnectionException,
			UnsupportedEncodingException {

		String path = this.formatPath(User.class, userId, "memberships");

		return (GroupCollection) this.authenticatedRequest(null, "GET", null,
				path, null, GroupCollection.class);
	}

	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws InvalidRequestException
	 * @throws APIException
	 * @throws AuthenticationException
	 * @throws APIConnectionException
	 * @throws UnsupportedEncodingException
	 */
	public UserCollection teamMembers(String groupId)
			throws InvalidRequestException, APIException,
			AuthenticationException, APIConnectionException,
			UnsupportedEncodingException {

		String path = this.formatPath(Group.class, groupId, "members");

		return (UserCollection) this.authenticatedRequest(null, "GET", null,
				path, null, UserCollection.class);
	}
	
}
