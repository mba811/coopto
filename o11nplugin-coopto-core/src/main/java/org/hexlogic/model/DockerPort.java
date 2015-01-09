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
import com.github.dockerjava.api.model.ExposedPort;
import com.vmware.o11n.plugin.sdk.annotation.VsoConstructor;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;

//Port is the container port that should be opened which is mapped to a host port during container start
// TODO this object should be automatically build on every inspect operation and replace the current String attribute within container
@VsoFinder(name = DockerPort.TYPE, idAccessor = "getId()")
@VsoObject(description = "A Docker port object for opening ports in containers", create = true, strict = true, name = "DockerPort")
public class DockerPort
{
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerPort";
	
	@VsoProperty(readOnly = true, hidden = true, description = "Unique identifier of this Docker port")
	private String id; // UUID used for vCO object creation
	@VsoProperty(readOnly = true, hidden = false, description = "Layer 4 type of the service to expose")
	private String type; // udp or tcp. Defaults to tcp
	@VsoProperty(readOnly = true, hidden = false, description = "Port number to expose")
	private int portNumber; // anything in between 1-65535

	@VsoConstructor(description = "Creates a new DockerPort for exposing container ports")
	public DockerPort(
			@VsoParam(description = "Layer 4 type of the service to expose. Accepts 'tcp' or 'udp'. Defaults to 'tcp'")String type,
			@VsoParam(description = "Port number to expose")int portNumber
			) throws Exception
	{
		this.id = UUID.randomUUID().toString();
		
		if(type != null && (type.equals("udp")))
		{
			this.type = "udp";
		}
		else
		{
			this.type = "tcp";
		}

		if(portNumber > 0 && portNumber < 65535)
		{
			this.portNumber = portNumber;
		}
		else
		{
			throw new Exception("Error: no valid port specified");
		}
	}
	
	@VsoMethod(showInApi = false, name = "getId")
	public String getId()
	{
		return id;
	}
	
	@VsoMethod(showInApi = true, name = "getType")
	public String getType()
	{
		return type;
	}

	@VsoMethod(showInApi = true, name = "getPortNumber")
	public int getPortNumber()
	{
		return portNumber;
	}
	
	@VsoMethod(showInApi = false)
	public ExposedPort toExposedPort()
	{
		if(type.equals("udp"))
		{
			return ExposedPort.udp(portNumber);
		}
		else
		{
			return ExposedPort.tcp(portNumber);
		}
	}
}