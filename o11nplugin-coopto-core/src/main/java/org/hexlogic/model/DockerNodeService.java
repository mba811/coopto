/*	This file is part of project "Coopto", a computer software plugin for 		*
 *  utilizing Docker in VMware vRealize Orchestrator.							*
 *																				*
 *	Copyright (C) 2014-2015  Robert Szymczak	(rszymczak@fum.de)				*
 *																				*
 *																				*
 *	This program is free software: you can redistribute it and/or modify		*
 *	it under the terms of the GNU Lesser General Public License as published 	*
 *	by the Free Software Foundation, either version 3 of the License, or		*
 *	(at your option) any later version.											*
 *																				*
 *	This program is distributed in the hope that it will be useful,				*
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of				*
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  						*
 *	See the GNU Lesser General Public License for more details.					*
 *																				*
 *	You should have received a copy of the GNU Lesser General Public License	*
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.		*/
package org.hexlogic.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.o11n.plugin.sdk.spring.InventoryRef;
import com.vmware.o11n.plugin.sdk.spring.platform.GlobalPluginNotificationHandler;

import ch.dunes.vso.sdk.endpoints.IEndpointConfiguration;
import ch.dunes.vso.sdk.endpoints.IEndpointConfigurationService;

//Java-Class for CRUD operations of all model objects! Not Accessable from within vCO (use ObjectManager or the model objects themself for this!)
@Component
public class DockerNodeService
{
    @Autowired
	private IEndpointConfigurationService service;
    @Autowired
    private GlobalPluginNotificationHandler notificationHandler;
    
	private static final Logger log = LogManager.getLogger(DockerNodeService.class);
	public DockerNodeService()
	{
		log.setLevel(Level.DEBUG);
	}
	
	public void createNode(String displayName, String hostName, int hostPort, String dockerApiVersion)
	{
		log.debug("Running createNode()...");
		try
		{
			// serialize a host to a collection of key value pairs
			// UUID of our object is used as UUID of the configuration element.
			// Generate some random UUID. If another vCO object with this UUID exists, vCO action will fail. so this is considered safe.
			IEndpointConfiguration config = service.newEndpointConfiguration(UUID.randomUUID().toString());
			config.setString(DockerNode.DISPLAYNAME, displayName);
			config.setString(DockerNode.HOSTNAME, hostName);
			
			if(hostPort > 0 && hostPort <65535)
			{
				config.setInt(DockerNode.HOSTPORT, hostPort);
			}
			else
			{
				config.setInt(DockerNode.HOSTPORT, DockerNode.defaultPort);
			}
			
			if(dockerApiVersion != null && !dockerApiVersion.isEmpty())
			{
				config.setString(DockerNode.APIVERSION, dockerApiVersion);
			}
			else
			{
				config.setString(DockerNode.APIVERSION, DockerNode.defaultApi);
			}

			service.saveEndpointConfiguration(config);
			
			// Invalidate the plugin-inventory by calling notifyElementsInvalidate
			// This will trigger 'sdk-invalidate' on object 'Coopto'
			log.debug("Reloading inventory...");
			notificationHandler.notifyElementsInvalidate();
			
			log.debug("Finished running createNode().");
		}
		catch (IOException e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.error("Error: " + sw.getBuffer().toString());
		}
	}
	
	public void deleteNode(String id) throws IOException
	{	
		log.debug("Running deleteNode()...");
		service.deleteEndpointConfiguration(id);

		// Notify plugin-inventory about the deleted node by calling notifyElementDeleted
		// This will trigger 'sdk-del' on object 'DockerNode/ID'
		log.debug("Reloading inventory...");
		notificationHandler.notifyElementDeleted(InventoryRef.valueOf(DockerNode.TYPE, id));
		log.debug("Finished running deleteNode().");
	}
	
	
	public Collection<IEndpointConfiguration> getNodes()
	{
		log.debug("Running getNodes() and loading nodes from vCO EndpointConfiguration...");
		try
		{
			Collection<IEndpointConfiguration> endpointConfigurations = service.getEndpointConfigurations();
			if(endpointConfigurations != null && !endpointConfigurations.isEmpty())
			{
				log.debug(endpointConfigurations.size() + " nodes are currently stored within the vCO EndpointConfiguration. Returning...");
				log.debug("Finished running getNodes().");
				return endpointConfigurations;
			}
		}
		catch (IOException e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.error("Error: " + sw.getBuffer().toString());
		}
		log.debug("Finished running getNodes().");
		return Collections.emptyList();
	}
	
	
	public IEndpointConfiguration getNode(String id)
	{
		try
		{
			IEndpointConfiguration endpointConfiguration = service.getEndpointConfiguration(id);
			if(endpointConfiguration != null)
			{
				return endpointConfiguration;
			}
			
			// In case we can not find the  saved configuration, return null
			throw new NullPointerException("No configuration for the DockerNode with id '" + id + "' found.");
		}
		catch (IOException e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.error("Error: " + sw.getBuffer().toString());
		}
		return null;
	}
}
