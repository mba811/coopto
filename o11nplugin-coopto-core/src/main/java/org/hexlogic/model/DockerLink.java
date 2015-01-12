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

import java.util.UUID;
import com.vmware.o11n.plugin.sdk.annotation.VsoConstructor;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;

@VsoFinder(name = DockerLink.TYPE, idAccessor = "getId()")
@VsoObject(description = "A Docker link object for linking containers together", create = true, strict = true, name = "DockerLink")
public class DockerLink
{
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerLink";
	
	@VsoProperty(readOnly = true, hidden = true, description = "Unique identifier of this Docker link")
	private String id; // UUID used for vCO object creation
	
	@VsoProperty(readOnly = true, hidden = false, description = "Container to link to")
	private DockerContainer container;

	@VsoProperty(readOnly = true, hidden = false, description = "Link alias to use for the link to the container")
	private String linkAlias; 

	@VsoConstructor(description = "Creates a new DockerLink for linking containers together")
	public DockerLink(DockerContainer container, String linkAlias)
	{
		this.id = UUID.randomUUID().toString();
		this.container = container;
		this.linkAlias = linkAlias;
	}
	
	@VsoMethod(showInApi = false, name = "getId")
	public String getId()
	{
		return id;
	}
	
	@VsoMethod(showInApi = true, name = "getContainer")
	public DockerContainer getContainer()
	{
		return container;
	}

	@VsoMethod(showInApi = true, name = "getLinkAlias")
	public String getLinkAlias()
	{
		return linkAlias;
	}
}
