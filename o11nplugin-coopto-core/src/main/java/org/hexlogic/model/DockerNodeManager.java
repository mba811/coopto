/*	This file is part of project "Coopto", a computer software plugin for 		*
 *  utilizing Docker in VMware vRealize Orchestrator.							*
 *																				*
 *	Copyright (C) 2014  Robert Szymczak	(rszymczak@fum.de)						*
 *																				*
 *																				*
 *	This program is free software: you can redistribute it and/or modify		*
 *	it under the terms of the GNU General Public License as published by		*
 *	the Free Software Foundation, either version 3 of the License, or			*
 *	(at your option) any later version.											*
 *																				*
 *	This program is distributed in the hope that it will be useful,				*
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of				*
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  						*
 *	See the GNU General Public License for more details.						*
 *																				*
 *	You should have received a copy of the GNU General Public License			*
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.		*/
package org.hexlogic.model;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import ch.dunes.vso.sdk.api.IPluginFactory;

import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.spring.AbstractSpringPluginFactory;
import com.vmware.o11n.plugin.sdk.spring.platform.GlobalPluginNotificationHandler;
import com.vmware.o11n.plugin.sdk.spring.task.AsyncPluginTaskExecutor;
import com.vmware.o11n.plugin.sdk.spring.watch.WatchRequestService;

/* Scripting Object Class for adding and deleting top-level items in vCO. */
//TODO Implement method for returning all nodes
@VsoObject(singleton = true, strict = false)
public class DockerNodeManager
{
	private static final Logger log = LogManager.getLogger(DockerNodeManager.class);
	
	@Autowired 
	private DockerNodeService service;
    @Autowired
    private GlobalPluginNotificationHandler notificationHandler;
    @Autowired
    private AsyncPluginTaskExecutor asyncPluginTaskExecutor;
    @Autowired
    private WatchRequestService watchRequestService;

	/*
	 * To create a class to map to the Orchestrator JavaScript API, you add an instance of that class to an 
	 * instance of the IPluginFactory implementation by defining a method named createScriptingSingleton(). 
	 * When the plug-in adaptor instantiates the factory, it also instantiates the class to add to the 
	 * JavaScript API. This makes it possible to call all methods of this class statically from within vCO. 
	 * E.g. there is no need to instantiate this class anymore, you can call CooptoDockerNodeManager.myMethod() 
	 * directly. Note that this is a hard requirement for async method calls!
	 */
    public static DockerNodeManager createScriptingSingleton(IPluginFactory factory) 
    {
    	log.setLevel(Level.DEBUG);
    	/* TAKE CARE if you call another class within createScriptingObject, that class will be used 
    	 * E.g. whenever you call DockerNodeManager.myMethod() it will try to run ClassUsed.myMethod()
    	 * Such a typo results in very confusing behavior!
    	 * */
        AbstractSpringPluginFactory f = (AbstractSpringPluginFactory) factory;
        return f.createScriptingObject(DockerNodeManager.class);
    }
    

    @VsoMethod
    public void createNode(	@VsoParam(description = "The display name to be used for this Docker node", required = true) String displayName, 
    						@VsoParam(description = "The hostname or IP-address to be used for this Docker node", required = true) String hostName, 
    						@VsoParam(description = "The docker remote API port to be used for this Docker node", required = false) int hostPort,
    						@VsoParam(description = "The version of the docker remote API executed on this Docker node", required = false) String dockerApiVersion) throws NullPointerException
    {
    	log.debug("Running createNode(...)...");
    	if(displayName != null && !displayName.isEmpty())
    	{
    		if(hostName != null && !hostName.isEmpty())
    		{
    			// The same docker host may be added multiple times to the inventory - we have no way to prevent that - but the will be a different one
    			log.debug("Creating new Docker node configuration...");
    			service.createNode(displayName, hostName, hostPort, dockerApiVersion);
    			log.debug("Finished running createNode(...)...");
    		}
    		else
    		{
    			log.error("The parameter hostName may not be empty.");
    			log.debug("Finished running createNode(...)...");
    			throw new NullPointerException("The parameter hostName may not be empty.");
    		}
    	}
		else
		{
			log.error("The parameter displayName may not be empty.");
			log.debug("Finished running createNode(...)...");
			throw new NullPointerException("The parameter displayName may not be empty.");
		}
    }
    
    @VsoMethod
    public void deleteNode(@VsoParam(description = "The id of the Docker node to delete", required = true) String id) throws IOException
    {
    	log.debug("Running createNode(...)...");
    	try
		{
			service.deleteNode(id);
			log.debug("Finished running createNode(...)...");
		} catch (IOException e)
		{
			log.error("No Docker node with id '" + id + "' was found in the vCO configuration. Please check your vCO configuration.");
			log.debug("Finished running createNode(...)...");
			throw new IOException("No Docker node with id '" + id + "' was found in the vCO configuration. Please check your vCO configuration.");
		}
    }
    
// -------------------------------------------------------------------------------------------------------------------------------------------
// TODO Async calls do not work yet. WatcherEvent is never called. Also, when implementing, give it some love and exception handling
// Async operations may be executed using the DockerNodeManager. They will trigger the sync operations of a Docker node in a async manner.
//    @AsyncScriptingMethod
//	@VsoMethod // DockerNode node,
//	public Object pullImageAsync(DockerNode node, String imageName)
//	{
//    	try
//		{
//			return ""+node.pullImage(imageName);
//		}
//		catch (Exception e)
//		{
//			final StringWriter sw = new StringWriter();
//			final PrintWriter pw = new PrintWriter(sw, true);
//			e.printStackTrace(pw);
//			log.error("Error: " + sw.getBuffer().toString());
//		}
//    	return "";
//	}
//    
//
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    public Hashtable getPullAsyncResult(PluginTrigger trigger) {
//        return new Hashtable(watchRequestService.retrieveLostReply(trigger));
//    }

}
