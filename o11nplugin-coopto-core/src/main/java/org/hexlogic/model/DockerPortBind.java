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

import java.util.UUID;
import com.vmware.o11n.plugin.sdk.annotation.VsoConstructor;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;

//TODO this object should be automatically build on every inspect operation and replace the current String attribute within container
@VsoFinder(name = DockerPortBind.TYPE, idAccessor = "getId()")
@VsoObject(description = "A Docker port bind object for binding exposed container ports to host ports", create = true, strict = true, name = "DockerPortBind")
public class DockerPortBind
{
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerPortBind";
	
	@VsoProperty(readOnly = true, hidden = true, description = "Unique identifier of this Docker port binding")
	private String id; // UUID used for vCO object creation
	@VsoProperty(readOnly = true, hidden = false, description = "Exposed container port to bind to")
	private DockerPort port; 
	@VsoProperty(readOnly = true, hidden = false, description = "The host port to map to the exposed container port")
	private int portNumber; // anything in between 1-65535

	@VsoConstructor(description = "Creates a new DockerPortBind for mapping exposed container ports to host ports")
	public DockerPortBind(
			@VsoParam(description = "The exposed container port to create a port mapping for")DockerPort port,
			@VsoParam(description = "The host port to map to the exposed container port to")int portNumber
			) throws Exception
	{
		this.id = UUID.randomUUID().toString();
		
		if(port != null)
		{
			this.port = port;
		}
		else
		{
			throw new Exception("Error: no valid exposed port specified");
		}

		if(portNumber > 0 && portNumber < 65535)
		{
			this.portNumber = portNumber;
		}
		else
		{
			throw new Exception("Error: no valid host port specified");
		}
	}
	
	@VsoMethod(showInApi = false, name = "getId")
	public String getId()
	{
		return id;
	}
	
	@VsoMethod(showInApi = true, name = "getPort")
	public DockerPort getPort()
	{
		return port;
	}

	@VsoMethod(showInApi = true, name = "getPortNumber")
	public int getPortNumber()
	{
		return portNumber;
	}
}