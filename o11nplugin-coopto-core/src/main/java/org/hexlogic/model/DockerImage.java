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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;
import com.vmware.o11n.plugin.sdk.spring.InventoryRef;

@VsoFinder(name = DockerImage.TYPE, image = "images/image_32x32.png", idAccessor = "getId()")
//By setting the strict attribute to true, Orchestrator can only call the methods from this class that are mapped in the vso.xml file (== annoted methods).
//To allow scripting to instantiate a class set create=true and annotate the constructor
@VsoObject(description = "A Docker image on a Docker node", create = false, strict = true)
public class DockerImage
{
	private static final Logger log = LoggerFactory.getLogger(DockerImage.class);
	
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerImage";
	
	// Properties should never be edited directly, so we make them readOnly.
	// vRO object id != imageID because we may have the same image placed on multiple hosts but the vRO id has to be unique across all hosts
	@VsoProperty(readOnly = true, hidden = true, displayName = "Id", description = "Unique identifier of this Docker image vRO object")
	private String id; // UUID used in vRO inventory for the vRO object

	@VsoProperty(readOnly = false, hidden = false, displayName = "Display name", description = "Display name of this Docker image")
	private String displayName; // DisplayName used in vRO inventory for the vRO object

	// Every image has a node
	@VsoProperty(readOnly = true, hidden = true, description = "The Docker node this Docker image is placed on")
	private DockerNode dockerNode = null;
	
	//--------------------------------------------------------------------------------------------------------------------------
	@VsoProperty(readOnly = true, hidden = false, displayName = "Author")
	private String author = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Docker version", description = "The Docker version this image requires")
	private String dockerVersion = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Image id", description = "Docker image id")
	private String imageId = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Image tag", description = "Primary Docker image tag")
	private String imageTag = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "OS")
	private String os = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Created")
	private String created = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Architecture")
	private String arch = "";
	
	//--------------------------------------------------------------------------------------------------------------------------
	public DockerImage(DockerNode node, String imageTag, String imageId)
	{
		// Do not use a random UUID here. We're not persisting image objects. Thus, on inventory reload we would loose reference to the image object if re-generating the UUID every time.
		// Rather then using a random UUID, generate a unique ID using the persistent node id + the unique image id.
		this.id = node.getId() + "_" + imageId;
		
		this.dockerNode = node;
		this.imageId = imageId;
		this.displayName = imageTag;
		this.imageTag = imageTag;
		
		try
		{
			this.reloadImage();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			
			log.warn("Warning: unable to call reloadImage(). " + sw.getBuffer().toString());
		}
	}
	
	// NO ACCESS from vRO
	// use unique UUID for type-ref
	@VsoMethod(showInApi = false)
    public InventoryRef toRef() {
        return InventoryRef.valueOf(TYPE, id);
    }
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void reloadImage() throws Exception
	{
		log.info("Running reloadImage()...");
		InspectImageResponse response = this.dockerNode.inspectDockerImage(this);
		if(response != null)
		{
			// if response or is null, leave it to the default values
			this.setAuthor(response.getAuthor());
			this.setDockerVersion(response.getDockerVersion());
			this.setArch(response.getArch());
			this.setOs(response.getOs());
			this.setCreated(response.getCreated());
		}
		else
		{
			log.error("Error: unable to load properties from response! No InspectImageResponse received.");
		}

		// Inform the plugin-invetory about the property changes
		dockerNode.updateChildInventory(toRef());
		log.info("Finished running reloadImage().");
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setAuthor(String author)
	{
		if(author.isEmpty())
		{
			this.author = "";
		}
		else
		{
			this.author = author;
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setImageId(String imageId)
	{
		if(imageId.isEmpty())
		{
			this.imageId = "";
		}
		else
		{
			this.imageId = imageId;
		}
		
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setImageTag(String imageTag)
	{
		if(imageTag.isEmpty())
		{
			this.imageTag = "";
		}
		else
		{
			this.imageTag = imageTag;
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setDockerVersion(String dockerVersion)
	{
		if(dockerVersion.isEmpty())
		{
			this.dockerVersion = "";
		}
		else
		{
			this.dockerVersion = dockerVersion;
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setOs(String os)
	{
		if(os.isEmpty())
		{
			this.os = "";
		}
		else
		{
			this.os = os;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setCreated(String created)
	{
		if(created.isEmpty())
		{
			this.created = "";
		}
		else
		{
			this.created = created;
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setArch(String arch)
	{
		if(arch.isEmpty())
		{
			this.arch = "";
		}
		else
		{
			this.arch = arch;
		}
	}
	
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// Public vRO API
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	
	// --------------------------------------------------------------------------------------------------------------------------
	// Property access			-------------------------------------------------------------------------------------------------
	
	// TODO For all getters, when called first re-sync using node.inspectImage, then return the requested value. First check how vCO is accessing the attributes: if getters are used, re-syncing on every get request would end up in a loop.
	@VsoMethod(showInApi = true, name = "getId")
	public String getId()
	{
		return id;
	}

	@VsoMethod(showInApi = true, name = "getDisplayName")
	public String getDisplayName()
	{
		return displayName;
	}

	@VsoMethod(showInApi = true, name = "getDockerNode")
	public DockerNode getDockerNode()
	{
		return dockerNode;
	}
	
	@VsoMethod(showInApi = true, name = "getAuthor")
	public String getAuthor()
	{
		return author;
	}
	
	@VsoMethod(showInApi = true, name = "getDockerVersion")
	public String getDockerVersion()
	{
		return dockerVersion;
	}
	
	@VsoMethod(showInApi = true, name = "getImageId")
	public String getImageId()
	{
		return imageId;
	}
	
	@VsoMethod(showInApi = true, name = "getImageTag")
	public String getImageTag()
	{
		return imageTag;
	}

	@VsoMethod(showInApi = true, name = "getOs")
	public String getOs()
	{
		return os;
	}
	
	@VsoMethod(showInApi = true, name = "getCreated")
	public String getCreated()
	{
		return created;
	}
	
	@VsoMethod(showInApi = true, name = "getArch")
	public String getArch()
	{
		return arch;
	}
	// --------------------------------------------------------------------------------------------------------------------------
	// Method access			-------------------------------------------------------------------------------------------------
	
	@VsoMethod(showInApi = true, name = "remove")
	public void remove(Boolean force) throws Exception
	{
		this.dockerNode.removeImage(this.imageId, force);
	}
	
	@VsoMethod(showInApi = true, name = "createContainer")
	public String createContainer(
			
			// complex objects
			@VsoParam(description = "Array of ports to expose on this container") DockerPort[] ports,
			@VsoParam(description = "An object mapping mountpoint paths inside the container to empty objects.") DockerVolume[] volumes,
			
			// strings
			@VsoParam(description = "Name for the container to use. Must be unique on every node") String name,
			@VsoParam(description = "Command(s) to run specified as a array of strings") String[] cmd,
			@VsoParam(description = "A string array of environment variables, each in the form of key=value") String[] env,
			@VsoParam(description = "A string value containing the working dir for commands to run in") String workingDir,
			@VsoParam(description = "A string value containing the desired hostname to use for the container") String hostname,
			@VsoParam(description = "A string value containg the user to use inside the container") String user,
			
			// bools
			@VsoParam(description = "Attaches container to stdin") boolean attachStdIn, 
			@VsoParam(description = "Attaches container to stdout") boolean attachStdOut, 
			@VsoParam(description = "Attaches container to stderr") boolean attachStdErr,
			@VsoParam(description = "Attach standard streams to a tty, including stdin if it is not closed") boolean tty,
			@VsoParam(description = "Opens stdin") boolean openStdIn,
			@VsoParam(description = "Close stdin after the 1 attached client disconnects") boolean stdInOnce,
			@VsoParam(description = "Boolean value, when true disables neworking for the container") boolean NetworkDisabled
			) throws Exception
	{
		return this.dockerNode.createContainer(this, ports, volumes, name, cmd, env, workingDir, hostname, user, attachStdIn, attachStdOut, attachStdErr, tty, openStdIn, stdInOnce, NetworkDisabled);
	}
}
