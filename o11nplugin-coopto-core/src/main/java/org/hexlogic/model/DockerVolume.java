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
import com.github.dockerjava.api.model.Volume;
import com.vmware.o11n.plugin.sdk.annotation.VsoConstructor;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;

// Volume is the destination map binding of a specified source volume which is defined during container start
//TODO this object should be automatically build on every inspect operation and replace the current String attribute within container
@VsoFinder(name = DockerVolume.TYPE, idAccessor = "getId()")
@VsoObject(description = "A Docker volume object for mapping volumes in containers", create = true, strict = true, name = "DockerVolume")
public class DockerVolume
{
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerVolume";
	
	@VsoProperty(readOnly = true, hidden = true, description = "Unique identifier of this Docker volume")
	private String id; // UUID used for vCO object creation
	@VsoProperty(readOnly = true, hidden = false, description = "Mountpoint within the container")
	private String mountPoint;

	@VsoConstructor(description = "Creates a new DockerVolume for mapping volumes to")
	public DockerVolume(
			@VsoParam(description = "Mountpoint path to map volumes to")String mountPoint
			) throws Exception
	{
		this.id = UUID.randomUUID().toString();
		
		if(mountPoint != null && (!mountPoint.isEmpty()))
		{
			this.mountPoint = mountPoint;
		}
		else
		{
			throw new Exception("Error: no valid mountpoint specified");
		}

	}

	@VsoMethod(showInApi = false, name = "getId")
	public String getId()
	{
		return id;
	}
	
	@VsoMethod(showInApi = true, name = "getMountPoint")
	public String getMountPoint()
	{
		return mountPoint;
	}
	
	@VsoMethod(showInApi = false)
	public Volume toVolume()
	{
		return new Volume(mountPoint);
	}
}
