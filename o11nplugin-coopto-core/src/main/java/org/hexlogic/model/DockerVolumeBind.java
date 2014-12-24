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
@VsoFinder(name = DockerVolumeBind.TYPE, idAccessor = "getId()")
@VsoObject(description = "A Docker bind object for binding containers volumes to physical volumes", create = true, strict = true, name = "DockerVolumeBind")
public class DockerVolumeBind
{
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerVolumeBind";
	
	@VsoProperty(readOnly = true, hidden = true, description = "Unique identifier of this Docker volume binding")
	private String id; // UUID used for vCO object creation
	
	@VsoProperty(readOnly = true, hidden = false, description = "The host mountpoint that should be bound to the container volume")
	private String mountPoint;
	
	@VsoProperty(readOnly = true, hidden = false, description = "The access mode that should be provided to the container for the specified host mount point. Accepts 'rw' or 'ro'. Defaults to 'rw'")
	private String accessMode;
	
	@VsoProperty(readOnly = true, hidden = false, description = "The container volume to bind the host mointpoint to")
	private DockerVolume volume;

	@VsoConstructor(description = "Creates a new DockerLink for linking containers together")
	public DockerVolumeBind(
				@VsoParam(description = "The host mountpoint that should be bound to the container volume")String mountPoint,
				@VsoParam(description = "The access mode that should be provided to the container for the specified host mount point. Accepts 'rw' or 'ro'. Defaults to 'rw'")String accessMode,
				@VsoParam(description = "The container volume to bind the host mointpoint to")DockerVolume volume
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
		
		
		if(accessMode != null && (accessMode.equals("ro")))
		{
			this.accessMode = "ro";
		}
		else
		{
			this.accessMode = "rw";
		}
		
		if(volume != null)
		{
			this.volume = volume;
		}
		else
		{
			throw new Exception("Error: no valid volume specified");
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

	@VsoMethod(showInApi = true, name = "getAccessMode")
	public String getAccessMode()
	{
		return accessMode;
	}

	@VsoMethod(showInApi = true, name = "getVolume")
	public DockerVolume getVolume()
	{
		return volume;
	}
}
