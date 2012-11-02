/*
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.parworks.androidlibrary.ar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;

import android.util.Log;

import com.parworks.androidlibrary.response.ARResponseHandler;
import com.parworks.androidlibrary.response.ARResponseHandlerImpl;
import com.parworks.androidlibrary.response.BasicResponse;
import com.parworks.androidlibrary.response.GetSiteInfoResponse;
import com.parworks.androidlibrary.response.NearbySitesResponse;
import com.parworks.androidlibrary.response.SiteInfo;
import com.parworks.androidlibrary.utils.AsyncHttpUtils;
import com.parworks.androidlibrary.utils.HMacShaPasswordEncoder;
import com.parworks.androidlibrary.utils.HttpCallback;
import com.parworks.androidlibrary.utils.HttpUtils;

/**
 * Used for Synchronously and Asynchronously finding, managing, and creating ARSites
 * @author Jules White, Adam Hickey
 *
 */
public class ARSites {
	
	private String mApiKey; 
	private String mSignature;
	private String mTime;
		
	public ARSites(String apiKey, String secretKey) {
		
		mApiKey = apiKey;
		
		HMacShaPasswordEncoder encoder = new HMacShaPasswordEncoder(256,true);
		mTime = ""+System.currentTimeMillis();
		mSignature = encoder.encodePassword(secretKey, mTime);
	}
	
	
	/**
	 * Asynchronously create an ARSite
	 * @param id the id of the site. Will be used for all site accesses
	 * @param name the name of the site
	 * @param lon longitude of the site
	 * @param lat latitude of the site
	 * @param desc site description
	 * @param feature site feature
	 * @param channel site channel
	 * @param listener callback that provides an ARSite object when the call completes
	 */
	public void create(final String id, final String name, final double lon, final double lat, final String desc, final String feature, final String channel, final ARListener<ARSite> listener){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("id", id);
		parameterMap.put("name",name);
		parameterMap.put("lon",Double.toString(lon));
		parameterMap.put("lat",Double.toString(lat));
		parameterMap.put("description",desc);
		parameterMap.put("feature", feature);
		parameterMap.put("channel",channel);
		
		AsyncHttpUtils asyncHttpUtils = new AsyncHttpUtils(mApiKey,mTime,mSignature);
		HttpCallback callback = new HttpCallback() {
			
			@Override
			public void onResponse(HttpResponse serverResponse) {
				HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
				PayloadExtractor<ARSite> extractor = new PayloadExtractor<ARSite>() {

					@Override
					public ARSite extract(HttpResponse httpResponseFromExtractor) {
						ARResponseHandler responseHandler = new ARResponseHandlerImpl();
						BasicResponse addSiteResponse = responseHandler.handleResponse(httpResponseFromExtractor, BasicResponse.class);						
						
						if(addSiteResponse.getSuccess() == true ) {
							return new ARSiteImpl(id,mApiKey,mTime,mSignature);
							
						} else {
							throw new ARException("Successfully communicated with the server, but couldn't create the site. The site Id is probably already in use.");
						}
					}
					
				};
				ARResponse<ARSite> addArSiteResponse = ARResponse.from(serverResponse, extractor);
				listener.handleResponse(addArSiteResponse);
			}			
			
			
			@Override
			public void onError(Exception e) {
				throw new ARException(e);
				
			}
		};
		asyncHttpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.ADD_SITE_PATH, parameterMap, callback);
		
		
	}
	
	/**
	 * Asynchronously create an ARSite
	 * @param id the id of the site. Will be used for all site accesses
	 * @param desc site description
	 * @param channel site channel
	 * @param listener callback that provides an ARSite object when the call completes
	 */
	public void create(final String id, final String desc, final String channel, final ARListener<ARSite> listener){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("id", id);
		parameterMap.put("description",desc);
		parameterMap.put("channel",channel);
		
		AsyncHttpUtils asyncHttpUtils = new AsyncHttpUtils(mApiKey,mTime,mSignature);
		HttpCallback callback = new HttpCallback() {
			
			@Override
			public void onResponse(HttpResponse serverResponse) {
				HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
				PayloadExtractor<ARSite> extractor = new PayloadExtractor<ARSite>() {

					@Override
					public ARSite extract(HttpResponse httpResponseFromExtractor) {
						ARResponseHandler responseHandler = new ARResponseHandlerImpl();
						BasicResponse nearbySites = responseHandler.handleResponse(httpResponseFromExtractor, BasicResponse.class);						
						
						if(nearbySites.getSuccess() == true ) {
							return new ARSiteImpl(id, mApiKey,mTime,mSignature);
						} else {
							throw new ARException("Successfully communicated with the server, but couldn't create the site. The site Id is probably already in use.");
						}
					}
					
				};
				ARResponse<ARSite> addArSiteResponse = ARResponse.from(serverResponse, extractor);
				listener.handleResponse(addArSiteResponse);
			}			
			
			
			@Override
			public void onError(Exception e) {
				throw new ARException(e);
				
			}
		};
		asyncHttpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.ADD_SITE_PATH, parameterMap, callback);
	}
	
	/**
	 * Asynchronously finds the site nearest a given latitude and longitude. This method does not specify the maximum number of sites, and so the default is one. Use near(double, double, int, double, ARListener)
	 * to retrieve multiple sites.
	 * @param lat latitude
	 * @param lon longitude
	 * @param sites the callback which provides a list of sites nearest the coordinates
	 */
	public void near(double lat, double lon, ARListener<List<ARSite>> sites) {
		near(Double.toString(lat),Double.toString(lon),"","",sites);
	}
	
	/**
	 * Asynchronously finds the sites nearest a set of coordinates
	 * @param lat latitude
	 * @param lon longitude
	 * @param max the maximum number of sites to return.
	 * @param radius the radius in which to search
	 * @param sites the callback which provides a list of the nearest ARSites
	 */
	public void near(double lat, double lon, int max, double radius,ARListener<List<ARSite>> sites){
		near(Double.toString(lat),Double.toString(lon),Integer.toString(max),Double.toString(radius),sites);
	}
	
	private void near(String lat, String lon, String max, String radius,final ARListener<List<ARSite>> sites){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("lat", lat);
		parameterMap.put("lon", lon);
		parameterMap.put("max", max);
		parameterMap.put("radius", radius);
		
		AsyncHttpUtils asyncHttpUtils = new AsyncHttpUtils(mApiKey,mTime,mSignature);
		HttpCallback callback = new HttpCallback() {
			
			@Override
			public void onResponse(HttpResponse serverResponse) {
				HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
				PayloadExtractor<List<ARSite>> extractor = new PayloadExtractor<List<ARSite>>() {

					@Override
					public List<ARSite> extract(HttpResponse httpResponseFromExtractor) {
						ARResponseHandler responseHandler = new ARResponseHandlerImpl();
						NearbySitesResponse nearbySites = responseHandler.handleResponse(httpResponseFromExtractor, NearbySitesResponse.class);						
						
						if(nearbySites.getSuccess() == true ) {
							List<SiteInfo> sitesInfo = nearbySites.getSites();
							List<ARSite> nearbySitesList = new ArrayList<ARSite>();
							for(SiteInfo info : sitesInfo) {
								nearbySitesList.add(new ARSiteImpl(info.getId(), mApiKey,mTime,mSignature));				
							}
							return nearbySitesList;
						} else {
							throw new ARException("Successfully communicated with the server, but the server was unsuccessful in finding nearby sites.");
						}
					}
					
				};
				ARResponse<List<ARSite>> nearbySitesARResponse = ARResponse.from(serverResponse, extractor);
				sites.handleResponse(nearbySitesARResponse);
			}			
			
			
			@Override
			public void onError(Exception e) {
				throw new ARException(e);
				
			}
		};
		asyncHttpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.NEARBY_SITE_PATH, parameterMap, callback);
	}
	
	/**
	 * Asynchronously get a previously created site
	 * @param id the id of the site
	 * @param listener the callback which provides the ARSite once the call completes
	 */
	public void getExisting(String id, final ARListener<ARSite> listener){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("site", id);
		
		AsyncHttpUtils asyncHttpUtils = new AsyncHttpUtils(mApiKey,mTime,mSignature);
		HttpCallback callback = new HttpCallback() {
			
			@Override
			public void onResponse(HttpResponse serverResponse) {
				HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
				PayloadExtractor<ARSite> extractor = new PayloadExtractor<ARSite>() {

					@Override
					public ARSite extract(HttpResponse httpResponseFromExtractor) {
						ARResponseHandler responseHandler = new ARResponseHandlerImpl();
						GetSiteInfoResponse siteInfoResponse = responseHandler.handleResponse(httpResponseFromExtractor, GetSiteInfoResponse.class);						
						
						if(siteInfoResponse.getSuccess() == true ) {
							return new ARSiteImpl(siteInfoResponse.getSite().getId(), mApiKey,mTime,mSignature);
						} else {
							throw new ARException("Successfully communicated with the server, but the server couldn't get a site. Maybe it doesn't exist.");
						}
					}
					
				};
				ARResponse<ARSite> nearbySitesARResponse = ARResponse.from(serverResponse, extractor);
				listener.handleResponse(nearbySitesARResponse);
			}			
			
			
			@Override
			public void onError(Exception e) {
				throw new ARException(e);
				
			}
		};
		asyncHttpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.GET_SITE_INFO_PATH, parameterMap, callback);
		
	}
	

	
	
	/*
	 *  synchronous methods
	 * 
	 */
	
	/**
	 * Synchronously get a previously created site
	 * @param id site id
	 * @return the ARSite
	 */
	public ARSite getExisting(String id){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("site", id);
		
		HttpUtils httpUtils = new HttpUtils(mApiKey,mTime,mSignature);
		HttpResponse serverResponse;
		serverResponse = httpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.GET_SITE_INFO_PATH, parameterMap);
		
		HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
		
		ARResponseHandler responseHandler = new ARResponseHandlerImpl();
		GetSiteInfoResponse getSiteResponse = responseHandler.handleResponse(serverResponse, GetSiteInfoResponse.class);
		

		
		if(getSiteResponse.getSuccess() == true) {
			ARSite newSite = new ARSiteImpl(getSiteResponse.getSite().getId(), mApiKey,mTime,mSignature);
			return newSite;
		} else {
			throw new ARException("Successfully communicated with the server, but failed to get siteinfo. The most likely cause is that a site with the specificed ID does not exist.");
		}
	}

	
	/**
	 * Synchronously create an ARSite
	 * @param id the site id. Will be used for accessing the site.
	 * @param desc the site description
	 * @param channel the site channel
	 * @return the newly created ARSite
	 */
	public ARSite create(String id, String desc, String channel) {
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("id", id);
		parameterMap.put("description",desc);
		parameterMap.put("channel",channel);
		
		HttpUtils httpUtils = new HttpUtils(mApiKey,mTime,mSignature);
		HttpResponse serverResponse;
		serverResponse = httpUtils.doPost(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.ADD_SITE_PATH, parameterMap);
		
		HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
		
		ARResponseHandler responseHandler = new ARResponseHandlerImpl();
		BasicResponse addSiteResponse = responseHandler.handleResponse(serverResponse, BasicResponse.class);
		

		
		if(addSiteResponse.getSuccess() == true) {
			ARSite newSite = getExisting(id);
			return newSite;
		} else{
			throw new ARException("Successfully communicated with the server, but failed to create a new site. The site id could already be in use, or a problem occurred.");
		}
	}
	
	/**
	 * Synchronously create an ARSite
	 * @param id the site id. Will be used for accessing the site.
	 * @param name the name of the site
	 * @param lon longitude
	 * @param lat latitude
	 * @param desc description
	 * @param feature site feature
	 * @param channel site channel
	 * @return the newly created ARSite
	 */
	public ARSite create(String id, String name, double lon, double lat, String desc, String feature, String channel){
		Map<String,String> parameterMap = new HashMap<String,String>();
		parameterMap.put("id", id);
		parameterMap.put("name",name);
		parameterMap.put("lon",Double.toString(lon));
		parameterMap.put("lat",Double.toString(lat));
		parameterMap.put("description",desc);
		parameterMap.put("feature", feature);
		parameterMap.put("channel",channel);
		
		HttpUtils httpUtils = new HttpUtils(mApiKey,mTime,mSignature);
		HttpResponse serverResponse;
		serverResponse = httpUtils.doPost(HttpUtils.PARWORKS_API_BASE_URL+HttpUtils.ADD_SITE_PATH, parameterMap);
		
		HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());
		
		ARResponseHandler responseHandler = new ARResponseHandlerImpl();
		BasicResponse addSiteResponse = responseHandler.handleResponse(serverResponse, BasicResponse.class);
		

		
		if(addSiteResponse.getSuccess() == true) {
			ARSite newSite = getExisting(id);
			return newSite;
		} else {
			throw new ARException("Successfully communicated with the server, but failed to create a new site. The site id could already be in use, or a problem occurred.");
		}

		
	}
	
	/**
	 * Synchronously finds the site closest to the given latitude and longitude.
	 * @param lat latitude
	 * @param lon longitude
	 * @return a list containing the closest ARSite to the given coordinates
	 */
	public List<ARSite> near(double lat, double lon) {
		return near(Double.toString(lat),Double.toString(lon),"","");
	}
	
	/**
	 * Synchronously finds the closest sites to the given coordinates.
	 * @param lat latitude
	 * @param lon longitude
	 * @param max the maximum number of sites to return
	 * @param radius the distance from the coordinates in which to search
	 * @return a list of the nearby sites
	 */
	public List<ARSite> near(double lat, double lon, int max, double radius ) {
		return near(Double.toString(lat),Double.toString(lon),Integer.toString(max),Double.toString(radius));
	}
	private List<ARSite> near(String lat, String lon, String max, String radius) {
		Map<String, String> parameterMap = new HashMap<String,String>();
		parameterMap.put("lat", lat);
		parameterMap.put("lon", lon);
		parameterMap.put("max",max);
		parameterMap.put("radius",radius);
		
		HttpResponse serverResponse;
		HttpUtils httpUtils = new HttpUtils(mApiKey,mTime,mSignature);
		serverResponse = httpUtils.doGet(HttpUtils.PARWORKS_API_BASE_URL + HttpUtils.NEARBY_SITE_PATH, parameterMap);
		HttpUtils.handleStatusCode(serverResponse.getStatusLine().getStatusCode());

		ARResponseHandler responseHandler = new ARResponseHandlerImpl();
		NearbySitesResponse nearbySites = responseHandler.handleResponse(serverResponse, NearbySitesResponse.class);
		
		if(nearbySites.getSuccess() == true ) {
			List<SiteInfo> sitesInfo = nearbySites.getSites();
			List<ARSite> nearbySitesList = new ArrayList<ARSite>();
			for(SiteInfo info : sitesInfo) {
				nearbySitesList.add(new ARSiteImpl(info.getId(), mApiKey,mTime,mSignature));				
			}
			return nearbySitesList;
		} else {
			throw new ARException("Successfully communicated with the server, but the server was unsuccessful in handling the request.");
		}	
		
	}
}
