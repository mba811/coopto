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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.VolumeBind;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;
import com.vmware.o11n.plugin.sdk.spring.InventoryRef;

@VsoFinder(name = DockerContainer.TYPE, image = "images/container_32x32.png", idAccessor = "getId()")
// By setting the strict attribute to true, Orchestrator can only call the methods from this class that are mapped in the vso.xml file (== annoted methods).
// To allow scripting to instantiate a class set create=true and annotate the constructor
@VsoObject(description = "A Container running on a Docker node", create = false, strict = true)
public class DockerContainer
{
	private static final Logger log = LogManager.getLogger(DockerContainer.class);
	
	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerContainer";

	// Properties should never be edited directly, so we make them readOnly.
	// vRO object id != containerID because we may have the same containerID placed on multiple hosts but the vRO id has to be unique across all hosts
	@VsoProperty(readOnly = true, hidden = true, displayName = "Id", description = "Unique identifier of this Docker container vRO object")
	private String id; // UUID used in vRO inventory for the vRO object

	@VsoProperty(readOnly = false, hidden = false, displayName = "Display name", description = "Display name of this Docker container")
	private String displayName; // DisplayName used in vRO inventory for the vRO object

	// Every container has a Docker node
	@VsoProperty(readOnly = true, hidden = true, description = "The Docker node this Docker container is running on")
	private DockerNode dockerNode = null;

	// --------------------------------------------------------------------------------------------------------------------------
	@VsoProperty(readOnly = true, hidden = false, displayName = "Container Id", description = "Docker container id")
	private String containerId = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Host name")
	private String hostname = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Container name")
	private String name = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Domain name")
	private String domainname = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "DNS server")
	private ArrayList<String> dns = new ArrayList<String>();
	@VsoProperty(readOnly = true, hidden = false, displayName = "DNS search")
	private ArrayList<String> dnsSearch = new ArrayList<String>();
	@VsoProperty(readOnly = true, hidden = false, displayName = "Run state")
	private String state = "Unkown";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Image id")
	private String imageId = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Image tag")
	private String imageTag = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "CMD")
	private ArrayList<String> cmd = new ArrayList<String>();
	@VsoProperty(readOnly = true, hidden = false, displayName = "ENV")
	private ArrayList<String> env = new ArrayList<String>();
	@VsoProperty(readOnly = true, hidden = false, displayName = "Entrypoint")
	private ArrayList<String> entrypoint = new ArrayList<String>();
	@VsoProperty(readOnly = true, hidden = false, displayName = "Working dir")
	private String workingDir = "";

	@VsoProperty(readOnly = true, hidden = false, displayName = "Std. in")
	private boolean stdIn = false;
	@VsoProperty(readOnly = true, hidden = false, displayName = "Std. out")
	private boolean stdOut = false;
	@VsoProperty(readOnly = true, hidden = false, displayName = "Std. error")
	private boolean stdErr = false;
	@VsoProperty(readOnly = true, hidden = false, displayName = "TTY")
	private boolean tty = false;
	
	@VsoProperty(readOnly = true, hidden = false, displayName = "User")
	private String user = "";

	@VsoProperty(readOnly = true, hidden = false, displayName = "Exposed ports")
	private ArrayList<String> exposedPorts = new ArrayList<String>(); // "<port>/<tcp|udp>: {}"
	@VsoProperty(readOnly = true, hidden = false, displayName = "Port bindings")
	private ArrayList<String> portBindings = new ArrayList<String>(); // "<port>/<protocol>: [{ "HostPort": "<port>" }"
	@VsoProperty(readOnly = true, hidden = false, displayName = "Primary binding")
	private String primaryBind = "";
	@VsoProperty(readOnly = true, hidden = false, displayName = "Links")
	private ArrayList<String> links = new ArrayList<String>(); // "container_name:alias"
	@VsoProperty(readOnly = true, hidden = false, displayName = "Volumes")
	private ArrayList<String> volumes = new ArrayList<String>(); // "/dest/mntpoint={}"
	@VsoProperty(readOnly = true, hidden = false, displayName = "Volume bindings")
	private ArrayList<String> volumeBindings = new ArrayList<String>(); // "/dest/mntpoint:/src/mntpoint"

	// --------------------------------------------------------------------------------------------------------------------------
	
	public DockerContainer(DockerNode node, String containerName, String containerId)
	{
		log.setLevel(Level.DEBUG);
		// Do not use a random UUID here. We're not persisting container objects. Thus, on inventory reload we would loose reference to the container object if re-generating the UUID every time.
		// Rather then using a random UUID, generate a unique ID using the persistent node id + the unique container id.
		this.id = node.getId() + ":" + containerId;
		
		this.dockerNode = node;
		this.containerId = containerId;
		this.displayName = containerName.substring(1); // Don't include the forwarding slash (/) returned by the docker API for the displayName
		
		try
		{
			this.reloadContainer();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			
			log.warn("Warning: unable to call reloadContainer(). " + sw.getBuffer().toString());
		}
	}

	// NO ACCESS from vRO
	// use unique UUID for type-ref
	@VsoMethod(showInApi = false)
	public InventoryRef toRef()
	{
		return InventoryRef.valueOf(TYPE, id);
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setContainerId(String containerId)
	{
		if(containerId.isEmpty())
		{
			this.containerId = "";
		}
		else
		{
			this.containerId = containerId;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setHostname(String hostname)
	{
		if(hostname.isEmpty())
		{
			this.hostname = "";
		}
		else
		{
			this.hostname = hostname;
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setName(String name)
	{
		if(name.isEmpty())
		{
			this.name = "";
		}
		else
		{
			this.name = name;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setDomainname(String domainname)
	{
		if(domainname.isEmpty())
		{
			this.domainname = "";
		}
		else
		{
			this.domainname = domainname;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setDns(ArrayList<String> dns)
	{
		if(dns != null)
		{
			this.dns = dns;
		}
		else
		{
			this.dns = new ArrayList<String>();
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setDns(String[] dns)
	{
		if (dns != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : dns)
			{
				temp.add(c);
			}
			setDns(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setDns(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setDnsSearch(ArrayList<String> dnsSearch)
	{
		if(dnsSearch != null)
		{
			this.dnsSearch = dnsSearch;
		}
		else
		{
			this.dnsSearch = new ArrayList<String>();
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setDnsSearch(String[] dnsSearch)
	{
		if (dnsSearch != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : dnsSearch)
			{
				temp.add(c);
			}
			setDnsSearch(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setDnsSearch(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setState(String state)
	{
		this.state = state;
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
	public void setCmd(ArrayList<String> cmd)
	{
		if(cmd != null)
		{
			this.cmd = cmd;
		}
		else
		{
			this.cmd = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setCmd(String[] cmd)
	{
		if (cmd != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : cmd)
			{
				temp.add(c);
			}
			setCmd(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setCmd(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setEnv(ArrayList<String> env)
	{
		if(env != null)
		{
			this.env = env;
		}
		else
		{
			this.env = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setEnv(String[] env)
	{
		if (env != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : env)
			{
				temp.add(c);
			}
			setEnv(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setEnv(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setEntrypoint(ArrayList<String> entrypoint)
	{
		if(entrypoint != null)
		{
			this.entrypoint = entrypoint;
		}
		else
		{
			this.entrypoint = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setEntrypoint(String[] entrypoint)
	{
		if (entrypoint != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : entrypoint)
			{
				temp.add(c);
			}
			setEntrypoint(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setEntrypoint(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setWorkingDir(String workingDir)
	{
		if(workingDir.isEmpty())
		{
			this.workingDir = "";
		}
		else
		{
			this.workingDir = workingDir;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setStdIn(boolean stdIn)
	{
		this.stdIn = stdIn;
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setStdOut(boolean stdOut)
	{
		this.stdOut = stdOut;
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setStdErr(boolean stdErr)
	{
		this.stdErr = stdErr;
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setTty(boolean tty)
	{
		this.tty = tty;
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setUser(String user)
	{
		if(user.isEmpty())
		{
			this.user = "";
		}
		else
		{
			this.user = user;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setExposedPorts(ArrayList<String> exposedPorts)
	{
		if(exposedPorts != null)
		{
			this.exposedPorts = exposedPorts;
		}
		else
		{
			this.exposedPorts = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setPortBindings(ArrayList<String> portBindings)
	{
		if(portBindings != null)
		{
			this.portBindings = portBindings;
		}
		else
		{
			this.portBindings = new ArrayList<String>();
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setPrimaryBind(String primaryBind)
	{
		if(primaryBind.isEmpty())
		{
			this.primaryBind = "";
		}
		else
		{
			this.primaryBind = primaryBind;
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setLinks(ArrayList<String> links)
	{
		if(links != null)
		{
			this.links = links;
		}
		else
		{
			this.links = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setLinks(String[] links)
	{
		if (links != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String c : links)
			{
				temp.add(c);
			}
			setLinks(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setLinks(temp);
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setVolumes(ArrayList<String> volumes)
	{
		if(volumes != null)
		{
			this.volumes = volumes;
		}
		else
		{
			this.volumes = new ArrayList<String>();
		}
	}
	
	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void setVolumeBindings(ArrayList<String> volumeBindings)
	{
		if(volumeBindings != null)
		{
			this.volumeBindings = volumeBindings;
		}
		else
		{
			this.volumeBindings = new ArrayList<String>();
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	private void setVolumeBindings(String[] volumeBindings)
	{
		if (volumeBindings != null)
		{
			ArrayList<String> temp = new ArrayList<String>();
			for (String vb : volumeBindings)
			{
				temp.add(vb);
			}
			setVolumeBindings(temp);
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add("");
			setVolumeBindings(temp);
		}
	}

	// NO ACCESS from vRO
	@VsoMethod(showInApi = false)
	public void reloadContainer() throws Exception
	{
		InspectContainerResponse response = this.dockerNode.inspectDockerContainer(this);
		if(response != null)
		{
			ContainerConfig containerConfig = response.getConfig();
			// if response or containerConfig are null, leave it to the default values
			if(containerConfig != null)
			{
				setHostname(response.getConfig().getHostName());
				setName(response.getName());
				setDomainname(response.getConfig().getDomainName());
				setDns(response.getHostConfig().getDns());
				setDnsSearch(response.getHostConfig().getDnsSearch());
				
				String imageId = response.getImageId();
				setImageId(imageId);
				
				DockerImage img = this.dockerNode.getImageById(imageId);
				if(img != null)
				{
					setImageTag(img.getImageTag());
				}

				setCmd(response.getConfig().getCmd());
				setEnv(response.getConfig().getEnv());
				setEntrypoint(response.getConfig().getEntrypoint());
				setWorkingDir(response.getConfig().getWorkingDir());

				setStdIn(response.getConfig().isAttachStdin());
				setStdOut(response.getConfig().isAttachStdout());
				setStdErr(response.getConfig().isAttachStderr());
				setTty(response.getConfig().isTty());

				setUser(response.getConfig().getUser());
			}
			else
			{
				log.error("Error: unable to load properties from container config!");
			}
		}
		else
		{
			log.error("Error: unable to load properties from response! No InspectContainerResponse received.");
		}

		try
		{
			if (response.getState().isRunning())
			{
				setState("Running. Started at: " + response.getState().getStartedAt());
	
			}
			else if (response.getState().isPaused())
			{
				setState("Paused");
			}
			else
			{
				setState("Stopped. Finished at: " + response.getState().getFinishedAt());
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'state'. This is normal if a given property does not yet exist for the container.");
			
			setState("Unkown");
		}

		// Exposed Ports
		try
		{
			ExposedPort[] exposedPorts = response.getConfig().getExposedPorts();
			if(exposedPorts != null && exposedPorts.length >0)
			{
				ArrayList<String> newExposedPorts = new ArrayList<String>();
				for (ExposedPort port : exposedPorts)
				{
					newExposedPorts.add(port.toString());
				}
				setExposedPorts(newExposedPorts);
			}
			else
			{
				this.exposedPorts = new ArrayList<String>();
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'exposedPorts'. This is normal if a given property does not yet exist for the container.");
			
			this.exposedPorts = new ArrayList<String>();
		}
		
		// Port Binding
		try
		{
			Map<ExposedPort, Binding[]> portBinds = response.getNetworkSettings().getPorts().getBindings();
			if(portBinds != null && !portBinds.isEmpty())
			{
				// TODO instead of building simple string objects for output in the inventory view, with the next release we should consider using some custom objects for this job
				ArrayList<String> newPortBinds = new ArrayList<String>();
				Boolean firstEntry = true;
				for (Map.Entry<ExposedPort, Binding[]> entry : portBinds.entrySet())
				{
					StringBuilder portBind = new StringBuilder();
					// Result should look like [443/udp -> null], [80/tcp -> [0.0.0.0:8080, 0.0.0.0:80]]
					portBind.append("[");
					portBind.append(entry.getKey().getPort());
					portBind.append("/");
					portBind.append(entry.getKey().getProtocol().toString());
					portBind.append("->");
					
					Binding[] binds = entry.getValue();
					if(binds != null && binds.length > 0)
					{
						for (Binding bind : binds)
						{
							portBind.append(bind.getHostIp());
							portBind.append(":");
							portBind.append(bind.getHostPort());
							portBind.append(", ");
							
							// remember primary bind for easy access from vco
							// TODO instead of building simple string objects for output in the inventory view, with the next release we should consider using some custom objects for this job
							if(firstEntry)
							{
								this.setPrimaryBind(bind.getHostPort().toString());
								firstEntry = false;
							}
						}
					}
					else
					{
						portBind.append("null");
					}
					portBind.append("]");
					newPortBinds.add(portBind.toString());
				}
				setPortBindings(newPortBinds);
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'portBinds'. This is normal if a given property does not yet exist for the container.");
			
			this.exposedPorts = new ArrayList<String>();
		}

		// Links
		try
		{
			Link[] links = response.getHostConfig().getLinks().getLinks();
			if(links != null && links.length >0)
			{
				ArrayList<String> newLinks = new ArrayList<String>();
				for (Link link : links)
				{
					newLinks.add(link.toString());
				}
				setLinks(newLinks);
			}
			else
			{
				this.links = new ArrayList<String>();
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'links'. This is normal if a given property does not yet exist for the container.");
			
			this.links = new ArrayList<String>();
		}

		// Exposed Volumes
		try
		{
			Map<String, ?> volumes = response.getConfig().getVolumes();
			if(volumes != null && volumes.size() >0)
			{
				ArrayList<String> newVolumes = new ArrayList<String>();
				for (Map.Entry<String, ?> entry : volumes.entrySet())
				{
					newVolumes.add(entry.getKey() + " : " + entry.getValue());
				}
				setVolumes(newVolumes);
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'volumes'. This is normal if a given property does not yet exist for the container.");
			
			this.volumes = new ArrayList<String>();
		}
		
		// Volume bindings
		try
		{
			VolumeBind[] volumeBinds = response.getVolumes();
			if(volumeBinds != null && volumeBinds.length > 0)
			{
				// TODO instead of building simple string objects for output in the inventory view, with the next release we should consider using some custom objects for this job
				ArrayList<String> newVolumeBinds = new ArrayList<String>();
				for (VolumeBind vb : volumeBinds)
				{
					// Result should look like [[/path/on/container:/path/on/host], [/path/on/container:/path/on/host], ...]
					StringBuilder volumeBind = new StringBuilder();
					volumeBind.append("[");
					volumeBind.append(vb.getContainerPath().toString());
					volumeBind.append(":");
					volumeBind.append(vb.getHostPath().toString());
					volumeBind.append("]");
					newVolumeBinds.add(volumeBind.toString());
				}
				setVolumeBindings(newVolumeBinds);
			}
			else
			{
				this.volumes = new ArrayList<String>();
			}
		}
		catch(Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.warn("Error while loading container property 'volumesBinds'. This is normal if a given property does not yet exist for the container.");
			
			this.volumes = new ArrayList<String>();
		}

		// Inform the plugin-invetory about the property changes
		dockerNode.updateChildInventory(toRef());
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// Public vRO API
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------------------------------------
	// Property access -------------------------------------------------------------------------------------------------
	
	// TODO For all getters, when called first re-sync using node.inspectImage, then return the requested value. First check how vCO is accessing the attributes: if getters are used, re-syncing on every get request would end up in a loop.
	@VsoMethod(showInApi = true, name = "getContainerId")
	public String getContainerId()
	{
		return containerId;
	}
	
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

	@VsoMethod(showInApi = true)
	public String getHostname()
	{
		return hostname;
	}
	
	@VsoMethod(showInApi = true)
	public String getName()
	{
		return name;
	}

	@VsoMethod(showInApi = true)
	public String getDomainname()
	{
		return domainname;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getDns()
	{
		return dns;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getDnsSearch()
	{
		return dnsSearch;
	}

	@VsoMethod(showInApi = true)
	public String getState()
	{
		return state;
	}

	@VsoMethod(showInApi = true)
	public String getImageId()
	{
		return imageId;
	}
	
	@VsoMethod(showInApi = true)
	public String getImageTag()
	{
		return imageTag;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getCmd()
	{
		return cmd;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getEnv()
	{
		return env;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getEntrypoint()
	{
		return entrypoint;
	}

	@VsoMethod(showInApi = true)
	public String getWorkingDir()
	{
		return workingDir;
	}

	@VsoMethod(showInApi = true)
	public boolean isStdIn()
	{
		return stdIn;
	}

	@VsoMethod(showInApi = true)
	public boolean isStdOut()
	{
		return stdOut;
	}

	@VsoMethod(showInApi = true)
	public boolean isStdErr()
	{
		return stdErr;
	}

	@VsoMethod(showInApi = true)
	public boolean isTty()
	{
		return tty;
	}
	
	@VsoMethod(showInApi = true)
	public String getUser()
	{
		return user;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getExposedPorts()
	{
		return exposedPorts;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getPortBindings()
	{
		return portBindings;
	}
	
	@VsoMethod(showInApi = true)
	public String getPrimaryBind()
	{
		return primaryBind;
	}

	@VsoMethod(showInApi = true)
	public ArrayList<String> getLinks()
	{
		return links;
	}
	
	@VsoMethod(showInApi = true)
	public ArrayList<String> getVolumes()
	{
		return volumes;
	}
	
	@VsoMethod(showInApi = true)
	public ArrayList<String> getVolumeBindings()
	{
		return volumeBindings;
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// Method access -------------------------------------------------------------------------------------------------
	@VsoMethod(showInApi = true, name = "start")
	public void start(
			// complex objects
			@VsoParam(description = "A array of links for the container") DockerLink[] links,
			@VsoParam(description = "A array of port bindings for the container") DockerPortBind[] portBindings,
			@VsoParam(description = "A array of volume bindings for the container") DockerVolumeBind[] volumeBindings,
			
			// strings
			@VsoParam(description = "A list of dns servers for the container to use") String[] dns,
			@VsoParam(description = "A list of DNS search domains") String[] dnsSearch,
			
			// bools
			@VsoParam(description = "Allocates a random host port for all of a container's exposed ports") boolean publishAllPorts
			) throws Exception
	{
		this.dockerNode.startContainer(this, links, portBindings, volumeBindings, dns, dnsSearch, publishAllPorts);
	}

	@VsoMethod(showInApi = true, name = "stop")
	public void stop(int wait) throws Exception
	{
		this.dockerNode.stopContainer(this, wait);
	}

	@VsoMethod(showInApi = true, name = "restart")
	public void restart(int wait) throws Exception
	{
		this.dockerNode.restartContainer(this, wait);
	}

	@VsoMethod(showInApi = true, name = "kill")
	public void kill(String signal) throws Exception
	{
		this.dockerNode.killContainer(this, signal);
	}

	@VsoMethod(showInApi = true, name = "remove")
	public void remove(boolean force, boolean removeVolumes) throws Exception
	{
		this.dockerNode.removeContainer(this, force, removeVolumes);
	}

	@VsoMethod(showInApi = true, name = "isRunning", description = "Returns true of the container is running, false otherwise. Combine with isPaused to determine stopped state.")
	public boolean isRunning() throws Exception
	{
		// update inventory
		this.reloadContainer();

		// return state
		InspectContainerResponse response = this.dockerNode.inspectDockerContainer(this);
		return response.getState().isRunning();
	}

	@VsoMethod(showInApi = true, name = "isPaused", description = "Returns true of the container is paused, false otherwise. Combine with isRunning to determine stopped state.")
	public boolean isPaused() throws Exception
	{
		// update inventory
		this.reloadContainer();

		// return state
		InspectContainerResponse response = this.dockerNode.inspectDockerContainer(this);
		return response.getState().isPaused();
	}
}
