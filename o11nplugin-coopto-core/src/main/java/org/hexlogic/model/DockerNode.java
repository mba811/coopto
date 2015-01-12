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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.hexlogic.CooptoPluginAdaptor;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.ConflictException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.NotModifiedException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.RestartContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.vmware.o11n.plugin.sdk.annotation.Cardinality;
import com.vmware.o11n.plugin.sdk.annotation.VsoFinder;
import com.vmware.o11n.plugin.sdk.annotation.VsoMethod;
import com.vmware.o11n.plugin.sdk.annotation.VsoObject;
import com.vmware.o11n.plugin.sdk.annotation.VsoParam;
import com.vmware.o11n.plugin.sdk.annotation.VsoProperty;
import com.vmware.o11n.plugin.sdk.annotation.VsoRelation;
import com.vmware.o11n.plugin.sdk.spring.InventoryRef;
import com.vmware.o11n.plugin.sdk.spring.platform.GlobalPluginNotificationHandler;

@VsoFinder(name = DockerNode.TYPE, image = "images/node_32x32.png", idAccessor = "getId()", relations = { @VsoRelation( name = DockerNode.IMAGERELATION, type = DockerImage.TYPE, cardinality = Cardinality.TO_MANY), @VsoRelation(name = DockerNode.CONTAINERRELATION, type = DockerContainer.TYPE, cardinality = Cardinality.TO_MANY)})
// TODO older SDK versions allowed to group child objects by (inFolder = true, folderName = "Children", folderImage = "images/img.png") for relations, where's that gone?
// By setting the strict attribute to true, Orchestrator can only call the methods from this class that are mapped in the vso.xml file (== annoted methods).
// To allow scripting to instantiate a class set create=true and annotate the constructor
@VsoObject(create = false, strict = true)
public class DockerNode implements IDockerNode
{
	private static final Logger log = LogManager.getLogger(DockerNode.class);

	// vCO TYPE & RELATION information
	public static final String TYPE = "DockerNode";
	public static final String IMAGERELATION = "DockerImages";
	public static final String CONTAINERRELATION = "DockerContainers";

	// Minimum set of keys we need to persist the object properties, using K-V mapping in IEndpointConfiguration
	public final static String DISPLAYNAME = "DisplayName";
	public final static String HOSTNAME = "HostName";
	public final static String HOSTPORT = "HostPort";
	public final static String APIVERSION = "HostApiVerion";
	public final static String STATUS_OFFLINE = "OFFLINE";
	public final static String STATUS_ONLINE = "ONLINE";

	// Minimum set of values we need to persist the object properties, using K-V mapping in IEndpointConfiguration
	public final static int defaultPort = 2375; // Fall back to default 2375
	public final static String defaultApi = "1.15"; // Fall back to default 1.15
	
	// config done at runtime
	DockerClientConfig config;

	// Properties should never be edited directly, so we make them readOnly.
	@VsoProperty(readOnly = true, hidden = true,  displayName = "Id", description = "Unique identifier of this Docker node")
	private String id; // UUID used in vCO inventory for the vCO object representing the Docker-Host

	@VsoProperty(readOnly = true, hidden = false, displayName = "Status", description = "Connection status of this Docker node")
	private String status; // Connection status to this Docker node

	@VsoProperty(readOnly = true, hidden = false, displayName = "Display name", description = "Display name of this Docker node")
	private String displayName; // DisplayName used in vCO inventory for the vCO object representing the Docker-Object

	@VsoProperty(readOnly = true, hidden = false, displayName = "Host name", description = "FQDN or IP-address of this Docker node")
	private String hostName;

	@VsoProperty(readOnly = true, hidden = false, displayName = "Remote API port", description = "TCP portnumber this Docker node is listening on")
	private int hostPortNumber = defaultPort;

	@VsoProperty(readOnly = true, hidden = false, displayName = "Remove API version", description = "The Docker remote API version to use, has to be supported by the Docker node")
	private String dockerApiVersion = defaultApi;
	
	// TODO make this configurable on node creation time and add a disable / enable workflow
	// Inventory options to show all containers / images
	@VsoProperty(readOnly = true, hidden = false, displayName = "Show stopped containers", description = "Should stopped containers be displayed within the inventory?")
	private Boolean showStoppedContainers = true;
	@VsoProperty(readOnly = true, hidden = false, displayName = "Show related images", description = "Should related images be displayed within the inventory?")
	private Boolean showRelatedImages = false;

	// Hide children from the node details since they're displayed using the inventory relations
	@VsoProperty(readOnly = true, hidden = true, description = "Images available on this Docker node.")
	private List<DockerImage> images = Collections.synchronizedList(new ArrayList<DockerImage>());
	@VsoProperty(readOnly = true, hidden = true, description = "Containers on this Docker node.")
	private List<DockerContainer> containers = Collections.synchronizedList(new ArrayList<DockerContainer>());

	@Autowired
	private DockerNodeService service;
	@Autowired
	private GlobalPluginNotificationHandler notificationHandler;

	// --------------------------------------------------------------------------------------------------------------------------
	
	public DockerNode()
	{
		log.setLevel(Level.DEBUG);
		configureNode();
	}
	
	public void configureNode()
	{
		config = DockerClientConfig.createDefaultConfigBuilder()
				.withVersion(this.getDockerApiVersion())
			    .withUri("http://" + this.getHostName() + ":" + this.getHostPortNumber())
			    .build();
	}

	// setId should normally not be used since we auto-generate the id. Thus we prevent access by not annoting this method with @VsoMethod
	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	@Override
	public void setId(String id)
	{
		this.id = id;
	}
	
	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void setStatus(String status)
	{
		this.status = status;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	@Override
	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	@Override
	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	@Override
	public void setHostPortNumber(int portNumber)
	{
		this.hostPortNumber = portNumber;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	@Override
	public void setDockerApiVersion(String apiVersion)
	{
		this.dockerApiVersion = apiVersion;

	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public DockerClient getDockerClient()
	{
		DockerClient dockerClient = null;
		try
		{
			configureNode();
			dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
			
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.error("Error: " + sw.getBuffer().toString());
		}

		return dockerClient;
	}
	
	// NO ACCESS from vCO, NO ACCESS from outside
	private void initNode()
	{
		if(isOnline())
		{
			this.status = STATUS_ONLINE;
		}
		else
		{
			this.status = STATUS_OFFLINE;
		}
	}
	
	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void reloadNode()
	{
		this.initNode();
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public List<DockerImage> getImages()
	{
		return this.images;
	}

	// NO ACCESS from vCO
	// find image by uuid attribute "id"
	@VsoMethod(showInApi = false)
	public DockerImage getImage(String id)
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.images) {
				
				Iterator<DockerImage> itr = this.images.iterator();
				while (itr.hasNext())
				{
					DockerImage image = itr.next();
					log.debug("Searching images. Current image: " + image.getId() + ".");
					if (image.getId().equals(id))
					{
						log.debug("Found image '" + id + "'.");
						return image;
					}
				}
			}
			
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Image with id '" + id + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadImages();
			}
		}

		// if not found in cache after cache was synchronized, the image dosn't exist on this host. Return null.
		log.debug("Image with id '" + id + "' not found on this docker node.");
		return null;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void setImages(ArrayList<DockerImage> images)
	{
		this.images = images;
	}

	// NO ACCESS from vCO, NO ACCESS from outside
	private synchronized void addImage(DockerImage item) throws Exception
	{
		images.add(item);
		// no reload since this is not triggered by vco but by internal methods
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void initImages(boolean allImages)
	{
		log.debug("About to load images for node '" + this.id + "'");
		if ((this.hostName != null) && (this.hostPortNumber > 0) && (this.hostPortNumber < 65535))
		{
			try
			{
				// Get the list of docker images from the host
				List<Image> newImages = null;
				try
				{	
					configureNode();
					DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
					log.debug("Loading Images with option all=" + allImages + ".");
					newImages = dockerClient.listImagesCmd().withShowAll(allImages).exec();
				}
				catch (Exception e)
				{
					log.error("Error occured while loading images: " + e.getMessage());
					// if a error occurs, clear the cache and throw an error to prevent the client from seeing out-dated data
					log.debug("Clearing image cache on node '" + displayName + "'.");
					this.images.clear();
					throw e;
				}
				
				// handle init / update in a synchronized manner
				synchronized (this.images) 
				{
					// 1. Collect diff information
					// 1a. Only in newList == addedImages, add and notifyInvalidate(node) when done with everything. newList - oldList = addedImages
					List<Image> addedImages = new ArrayList<Image>(newImages);
					Iterator<Image> addItr = addedImages.iterator();
					log.debug("Checking for new images...");
					while (addItr.hasNext())
					{
						Image img = addItr.next();
						Iterator<DockerImage> itr = this.images.iterator();
						while (itr.hasNext())
						{
							DockerImage image = itr.next();
							if (img.getId().equals(image.getImageId()))
							{
								// if the image exists in the oldList, remove it from the addedImages
								log.debug("Image '" + img.getId() + "' seems to be already cached.");
								addItr.remove();
							}
						}
					}
					log.debug("New images found are: " +addedImages.toString());
					
					// 1b. Only in oldList == deletedImages, delete and notifyDelete(image). oldList - newList = deletedImages
					List<DockerImage> deletedImages = new ArrayList<DockerImage>();
					Iterator<DockerImage> delItr = this.images.iterator();
					log.debug("Checking for deleted images...");
					deleteloop:
					while (delItr.hasNext())
					{
						DockerImage image = delItr.next();
						Iterator<Image> itr = newImages.iterator();
						while (itr.hasNext())
						{
							Image img = itr.next();
							if(img.getId().equals(image.getImageId()))
							{
								// if the image exists within the oldList, it's not deleted - check the next image
								continue deleteloop;
							}
						}
						// loop was not exited, so our image was not found in the newList and can be considered as deleted
						log.debug("Image '" + image.getImageId() + "' seems to be deleted.");
						deletedImages.add(image);
					}
					log.debug("Deleted images found are: " +deletedImages.toString());
					
					// 1c. In newList and in oldList == eventually updated images, run inspectImage and notifyUpdated(image). in both == updatedImages		
					Iterator<Image> updItr = newImages.iterator();
					log.debug("Updateing loaded images on node's cache...");
					while (updItr.hasNext())
					{
						Image img = updItr.next();
						Iterator<DockerImage> itr = this.images.iterator();
						while (itr.hasNext())
						{
							DockerImage image = itr.next();
							if(img.getId().equals(image.getImageId()))
							{
								// if a image exists within the old and the new list, just update it's details
								log.debug("Found image in node's cache. Updateing image info...");
								image.reloadImage();
							}
						}		
					}
					log.debug("Finished updateing loaded images on node's cache.");
					
					if(!addedImages.isEmpty())
					{
						// for every added image, add it to our image list. This will also call updateChildInventory for that image
						// Do not use for-loops for this, since it will cause ConcurrentModificationExceptions to be thrown!
						log.debug("Adding loaded images to node's cache...");
						Iterator<Image> iterator = addedImages.iterator();
						while (iterator.hasNext())
						{
							Image i = iterator.next();
							String[] tags = i.getRepoTags();
							// Tags may look like this : m451/drum-machine:latest
							// Or this : base:ubuntu-12.10
							String tag = tags[0];
							String id = i.getId();

							try
							{
								log.debug("Adding image " + tag);
								this.addImage((new DockerImage(this, tag, id)));
								
								//TODO call some notify method to let the node know there's a new child! (currently the constructor only calls update!)
								
							}
							catch (Exception e)
							{
								log.error("Error while adding image to image-list. " + e.getMessage());
							}
						}
						log.debug("Finished adding loaded images to node's cache.");
					}
					
					if(!deletedImages.isEmpty())
					{
						// for every deleted image, delete it from our list.
						// Do not use for-loops for this, since it will cause ConcurrentModificationExceptions to be thrown!
						log.debug("Removeing deleted images from node cache...");
						Iterator<DockerImage> iterator = deletedImages.iterator();
						while (iterator.hasNext())
						{
							DockerImage i = iterator.next();
							try
							{
								Iterator<DockerImage> itr = this.images.iterator();
								while(itr.hasNext())
								{
									DockerImage img = itr.next();
									if(img.getImageId().equals(i.getImageId()))
									{
										log.debug("Deleting image '" + img.getDisplayName() + "' from node cache...");
										InventoryRef ref = img.toRef();
										itr.remove();
										
										notificationHandler.notifyElementDeleted(ref);
									}
								}
							}
							catch (Exception e)
							{
								log.error("Error while removeing deleted image from image-list. " + e.getMessage());
							}
						}
						log.debug("Finished removeing deleted images from node's cache.");
					}
				}
			}
			catch (Exception e)
			{
				// this method is never called directly by a user, thus, don't throw exceptions but rather log them
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				log.error("Error: " + sw.getBuffer().toString());
			}
		}
		else
		{
			log.error("Failed to load images from node '" + this.id + "'. Check the node's settings");
		}
		log.debug("Finished loading images for node '" + this.id + "' into cache.");
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void reloadImages()
	{
		this.reloadNode();
		this.initImages(showRelatedImages);
	}
	
	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void clearImages()
	{
		log.debug("Clearing image cache...");
		// handle update in a synchronized manner
		synchronized (this.images) 
		{
			this.images = new ArrayList<DockerImage>();
			notificationHandler.notifyElementInvalidate(toRef());
		}
	}

	// --------------------------------------------------------------------------------------------------------------------------

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public List<DockerContainer> getContainers()
	{
		return containers;
	}

	// NO ACCESS from vCO
	// find container by uuid attribute "id"
	@VsoMethod(showInApi = false)
	public DockerContainer getContainer(String id)
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.containers) 
			{
				Iterator<DockerContainer> itr = this.containers.iterator();
				while (itr.hasNext())
				{
					DockerContainer container = itr.next();
					log.debug("Searching containers. Current container: " + container.getId() + ".");
					if (container.getId().equals(id))
					{
						log.debug("Found container '" + id + "'.");
						return container;
					}
				}
			}
			
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Container with id '" + id + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadContainers();
			}
		}

		// if not found in cache after cache was synchronized, the container dosn't exist on this host. Return null.
		log.debug("Container with id '" + id + "' not found on this docker node.");
		return null;
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void setContainers(ArrayList<DockerContainer> containers)
	{
		this.containers = containers;
	}

	// NO ACCESS from vCO, NO ACCESS from outside
	private synchronized void addContainer(DockerContainer item) throws IllegalArgumentException, IOException
	{
		containers.add(item);
		// no reload since this is not triggert by vco but by internal methods
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void initContainers(boolean allContainers)
	{
		log.debug("About to load containers for node '" + this.id + "'");
		if ((this.hostName != null) && (this.hostPortNumber > 0) && (this.hostPortNumber < 65535))
		{
			try
			{
				// Get the list of docker containers from the host
				List<Container> newContainers = null;
				try
				{
					configureNode();
					DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
					log.debug("Loading containers with option all=" + allContainers + ".");
					newContainers = dockerClient.listContainersCmd().withShowAll(allContainers).exec();
				}
				catch (Exception e)
				{
					log.error("Error occured while loading containers: " + e.getMessage());
					// if a error occurs, clear the cache and throw an error to prevent the client from seeing outdated data
					log.debug("Clearing container cache on node '" + displayName + "'.");
					this.containers.clear();
					throw e;
				}
				
				// handle init / update in a synchronized manner
				synchronized (this.containers) 
				{
					// 1. Collect diff information
					// 1a. Only in newList == addedContainers, add and notifyInvalidate(node) when done with everything. newList - oldList = addedContainers
					List<Container> addedContainers = new ArrayList<Container>(newContainers);
					Iterator<Container> addItr = addedContainers.iterator();
					log.debug("Checking for new containers...");
					while (addItr.hasNext())
					{
						Container cnt = addItr.next();
						Iterator<DockerContainer> itr = this.containers.iterator();
						while (itr.hasNext())
						{
							DockerContainer container = itr.next();
							if (container.getContainerId().equals(cnt.getId()))
							{
								// if the container exists in the oldList, remove it from the addedContainers
								log.debug("Container '" + cnt.getId() + "' seems to be already cached.");
								addItr.remove();
							}
						}
					}
					log.debug("New containers found are: " +addedContainers.toString());
					
					// 1b. Only in oldList == deletedContainers, delete and notifyDelete(container). oldList - newList = deletedContainers
					List<DockerContainer> deletedContainers = new ArrayList<DockerContainer>();
					Iterator<DockerContainer> delItr = this.containers.iterator();
					log.debug("Checking for deleted containers...");
					deleteloop:
					while (delItr.hasNext())
					{
						DockerContainer container = delItr.next();
						Iterator<Container> itr = newContainers.iterator();
						while (itr.hasNext())
						{
							Container cnt = itr.next();
							if(cnt.getId().equals(container.getContainerId()))
							{
								// if the container exists within the oldList, it's not deleted - check the next container
								continue deleteloop;
							}
						}
						// loop was not exited, so our container was not found in the newList and can be considered as deleted
						log.debug("Container '" + container.getContainerId() + "' seems to be deleted.");
						deletedContainers.add(container);
					}
					log.debug("Deleted containers found are: " +deletedContainers.toString());


					// 1c. In newList and in oldList == eventually updated containers, run inspectContainer and notifyUpdated(container). in both == updatedContainers		
					Iterator<Container> updItr = newContainers.iterator();
					log.debug("Updateing loaded containers on node's cache...");
					while (updItr.hasNext())
					{
						Container cnt = updItr.next();
						Iterator<DockerContainer> itr = this.containers.iterator();
						while (itr.hasNext())
						{
							DockerContainer container = itr.next();
							if(cnt.getId().equals(container.getContainerId()))
							{
								// if a container exists within the old and the new list, just update it's details
								log.debug("Found container in node's cache. Updateing container info...");
								container.reloadContainer();
							}
						}		
					}
					log.debug("Finished updateing loaded containers on node's cache.");
					
					if(!addedContainers.isEmpty())
					{
						// for every added container, add it to our container list. This will also call updateChildInventory for that container
						// Do not use for-loops for this, since it will cause ConcurrentModificationExceptions to be thrown!
						log.debug("Adding loaded containers to node's cache...");
						Iterator<Container> iterator = addedContainers.iterator();
						while (iterator.hasNext())
						{
							Container c = iterator.next();
							try
							{
								log.debug("Adding container " + c.getId());
								this.addContainer((new DockerContainer(this, c.getNames()[0], c.getId())));
								
								//TODO call some notify method to let the node know there's a new child! (currently the constructor only calls update!)
							}
							catch (Exception e)
							{
								log.error("Error while adding container to container-cache. " + e.getMessage());
							}
						}
						log.debug("Finished adding loaded containers to node's cache.");
					}
					
					if(!deletedContainers.isEmpty())
					{
						// for every deleted container, delete it from our list.
						// Do not use for-loops for this, since it will cause ConcurrentModificationExceptions to be thrown!
						log.debug("Removeing deleted containers from node's cache...");
						Iterator<DockerContainer> iterator = deletedContainers.iterator();
						while (iterator.hasNext())
						{
							DockerContainer c = iterator.next();
							try
							{
								Iterator<DockerContainer> itr = this.containers.iterator();
								while(itr.hasNext())
								{
									DockerContainer cnt = itr.next();
									if(cnt.getContainerId().equals(c.getContainerId()))
									{
										log.debug("Deleting container '" + cnt.getDisplayName() + "' from node cache...");
										InventoryRef ref = cnt.toRef();
										itr.remove();
										
										notificationHandler.notifyElementDeleted(ref);
									}
								}
							}
							catch (Exception e)
							{
								log.error("Error while removeing deleted container from container-list. " + e.getMessage());
							}
						}
						log.debug("Finished removeing deleted containers from node's cache.");
					}
				}
			}
			catch (Exception e)
			{
				// this method is never called directly by a user, thus, don't throw exceptions but rather log them
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				log.error("Error: " + sw.getBuffer().toString());
			}
		}
		else
		{
			log.error("Failed to load containers from node '" + this.id + "'. Check the node's settings");
		}
		log.debug("Finished loading containers for node '" + this.id + "' into cache.");
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void reloadContainers()
	{
		this.reloadNode();
		this.initContainers(showStoppedContainers);
	}
	
	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public void clearContainer()
	{
		log.debug("Clearing container cache...");
		// handle update in a synchronized manner
		synchronized (this.containers) 
		{
			this.containers = new ArrayList<DockerContainer>();
			notificationHandler.notifyElementInvalidate(toRef());
		}
	}

	// NO ACCESS from vCO
	@VsoMethod(showInApi = false)
	public InventoryRef toRef()
	{
		return InventoryRef.valueOf(TYPE, id);
	}

	// NO ACCESS from vCO - user should work with the inventory objects and the container methods
	@VsoMethod(showInApi = false)
	public InspectContainerResponse inspectDockerContainer(DockerContainer container) throws Exception
	{
		log.debug("Running inspectDockerContainer...");
		DockerClient dockerClient = null;
		try
		{
			configureNode();
			dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			return dockerClient.inspectContainerCmd(container.getContainerId()).exec();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error running inspectDockerContainer: " + sw.getBuffer().toString());
			throw e;
		}
	}

	// NO ACCESS from vCO - user should work with the inventory objects and the container methods
	@VsoMethod(showInApi = false)
	public InspectImageResponse inspectDockerImage(DockerImage image) throws Exception
	{
		log.debug("Running inspectDockerImage...");
		DockerClient dockerClient = null;
		try
		{
			configureNode();
			dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			return dockerClient.inspectImageCmd(image.getImageId()).exec();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error running inspectDockerImage: " + sw.getBuffer().toString());
			throw e;
		}
	}
	
	// access to notificationHandler from child-classes (e.g. DockerImage, DockerContainer)
	public void updateChildInventory(InventoryRef ref)
	{
		notificationHandler.notifyElementUpdated(ref);
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// Public vCO API
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------------------------------------
	// Property access -------------------------------------------------------------------------------------------------

	// showInApi: controls if the method will be displayed as a method of the object within the vCO API-explorer
	// name: used for the *.vso file creation
	// vsoReturnType: adds a hint of the return type of this method to the vCO API-explorer
	@VsoMethod(showInApi = true, name = "getId", description = "Returns the unique identifier of this Docker node", vsoReturnType = "String")
	@Override
	public String getId()
	{
		return id;
	}
	
	@VsoMethod(showInApi = true, name = "getStatus", description = "Returns the connection status of this Docker node (online / offline)", vsoReturnType = "String")
	public String getStatus()
	{
		return status;
	}
	
	@VsoMethod(showInApi = true, name = "getDisplayName", description = "Returns the display name of this Docker node", vsoReturnType = "String")
	@Override
	public String getDisplayName()
	{
		return displayName;
	}

	@VsoMethod(showInApi = true, name = "getHostName", description = "Returns the host name of this Docker node", vsoReturnType = "String")
	@Override
	public String getHostName()
	{
		return hostName;
	}

	@VsoMethod(showInApi = true, name = "getHostPortNumber", description = "Returns the tcp portnumber for the remote API of this Docker node", vsoReturnType = "int")
	@Override
	public int getHostPortNumber()
	{
		return hostPortNumber;
	}

	@VsoMethod(showInApi = true, name = "getDockerApiVersion", description = "Returns the configured API version of this Docker node", vsoReturnType = "String")
	@Override
	public String getDockerApiVersion()
	{
		return dockerApiVersion;
	}
	
	@VsoMethod(showInApi = true)
	public Boolean getShowStoppedContainers()
	{
		return showStoppedContainers;
	}
	
	@VsoMethod(showInApi = true)
	public void setShowStoppedContainers(Boolean showStoppedContainers)
	{
		this.showStoppedContainers = showStoppedContainers;
	}
	
	@VsoMethod(showInApi = true)
	public Boolean getShowRelatedImages()
	{
		return showRelatedImages;
	}
	
	@VsoMethod(showInApi = true)
	public void setShowRelatedImages(Boolean showRelatedImages)
	{
		this.showRelatedImages = showRelatedImages;
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// Method access -------------------------------------------------------------------------------------------------

	/*
	 * TODO Implement better exception handling
	 * 
	 * On failure: always throw an exception of type 'Exception' including the error message that occurred so the vCO workflow will
	 * fail and print details about the error. If the error is unclear (generic catch all exception) at the end of every try-catch)
	 * throw an 'Exception' and include the stack-trace!
	 * 
	 * On success return the return value specified in the method header. If the value is empty, return an empty array or null.
	 */

	/*
	 * TODO Implement common access strategy
	 * 
	 * Methods that are accessable from vCO and alter images, containers or nodes should never be called using the id. Instead we
	 * should always require the user to submit the object that CRUD methods should be applied to. This way we guarantee that the
	 * object we want to work on is known to our inventory and don't have to care about the sync state of the inventory with the
	 * actual docker-node inventory.
	 */
	// Host operations -------------------------------------------------------------------------------------------------

	@VsoMethod(showInApi = true, name = "getHostInfo", description = "Returns information about the docker node")
	public String getHostInfo() throws Exception
	{
		log.debug("Getting host info...");
		DockerClient dockerClient = null;
		try
		{
			configureNode();
			dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
			return dockerClient.infoCmd().exec().toString();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			log.error("Error while getting host info: " + sw.getBuffer().toString());
			// Throw error detail message so vCO can display it
			throw new Exception("Error while getting host info: " + sw.getBuffer().toString());
		}
	}
	
	@VsoMethod(showInApi = true, name = "isOnline", description = "Returns true of the host is reachable, false otherwise.")
	public Boolean isOnline()
	{
		try
		{
			if(this.getHostInfo() != null)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch(Exception e)
		{
			return false;
		}
	}

	// Image operations -------------------------------------------------------------------------------------------------

	// CRUD - Create
	@VsoMethod(showInApi = true, name = "pullImage", description = "Pull the image matching the given string from the docker hub repository, saving it on the docker host.")
	public String pullImage(String imageName) throws Exception
	{
		log.debug("Pulling image '" + imageName + "'...");

		@SuppressWarnings("rawtypes")
		MappingIterator<Map> it = null;
		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
			System.out.println("Starting pull operation...");

			/*
			 * We will check the final result by comparing the initial image id, which is the first ID provided by the stream such as:
			 * 
			 * {status=Pulling image (latest) from dockerfile/nodejs, progressDetail={}, id=406eb4a4dcad}
			 * 
			 * to the image id of the last entity which owns id AND status which will look something like:
			 * {status=Download complete, progressDetail={}, id=406eb4a4dcad}
			 * 
			 * If both IDs match, we know that the latest layer is the same as the requested image layer.
			 * So the next step is to compare the download status of that layer
			 */
			String firstId = null;
			String lastId = "";
			String lastStatus = "undefined";

			/*
			 * In addition to the download status of the layer, we provide additional information about how the process went by
			 * returning information to the user using the last entity which has no id and only a status, which looks like this:
			 * {status=Status: Image is up to date for dockerfile/nodejs}
			 * or
			 * {status=Status: Downloaded newer image for dockerfile/nodejs}
			 * or
			 * {status=Repository dockerfile/nodejs already being pulled by another client. Waiting.}
			 */
			String finalStatus = "undefined";

			for (it = new ObjectMapper().readValues(new JsonFactory().createJsonParser(dockerClient.pullImageCmd(imageName).exec()), Map.class); it.hasNext();)
			{
				Map<?, ?> element = it.next();
				String id = "";
				String status = "";
				String progress = "";

				// info OUTPUT
				// System.out.println("info: " + element);

				try
				{
					id = element.get("id").toString();
				}
				catch (NullPointerException e)
				{/* catch exception if key was not found */
				}
				try
				{
					status = element.get("status").toString();
				}
				catch (NullPointerException e)
				{/* catch exception if key was not found */
				}
				try
				{
					progress = element.get("progress").toString();
				}
				catch (NullPointerException e)
				{/* catch exception if key was not found */
				}

				// if the key was found and we got some status
				if (!id.isEmpty() && !status.isEmpty())
				{
					// remember the first id of the output stream, which is the id of the image we want to pull
					if (firstId == null)
					{
						System.out.println("Remembering first id: " + id);
						firstId = id;
					}

					// if the same layer is returned multiple times in a row, don't log everything but just the progress
					if (id.equals(lastId))
					{
						lastId = id;
						lastStatus = status;
						if (!progress.isEmpty())
						{
							System.out.println("Progress: " + progress);
						}
					}
					else
					{
						lastId = id;
						System.out.println("Image '" + id + "' status is: " + status + ".");
						if (!progress.isEmpty())
						{
							System.out.println("Progress: " + progress);
						}
					}
				}

				if (!status.isEmpty())
				{
					finalStatus = status;
				}
			}

			// TODO find a more robust way to handle downloadStatus and finalStatus
			String downloadStatus = "undefined";
			if (lastId.equals(firstId))
			{
				System.out.println("Last download layer id does match the requested image id: " + firstId);
				if (StringUtils.containsIgnoreCase(lastStatus, "Download complete"))
				{
					downloadStatus = "successed";
					log.debug("The requested layer was downloaded successfuly.");
				}
				else
				{
					downloadStatus = "failed";
					log.error("The requested layer failed to download.");
					// throw exception in order for the workflow to fail
					throw new IllegalStateException("The requested layer failed to download.");
				}
			}

			// reload images from docker node
			this.reloadImages();
			// update inventory - another way to do this would be to update our ArrayList and call notifyElementDeleted on the image object
			notificationHandler.notifyElementInvalidate(toRef());

			log.debug("Pull operation " + downloadStatus + ". " + finalStatus + ".");
			return "Pull operation " + downloadStatus + ". " + finalStatus + ".";

		}
		catch (InternalServerErrorException e)
		{
			// image dosn't exist
			log.error("Error: the image was not found.");
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the image was not found.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while pulling image: " + sw.getBuffer().toString());
			// Throw error detail message so vCO can display it
			throw new Exception("Error while pulling image: " + sw.getBuffer().toString());
		}
		finally
		{
			if (it != null)
			{
				log.debug("Closeing pullImage stream...");
				it.close();
				log.debug("Closed pullImage stream.");
			}
		}

	}
	
	// CRUD - Read
	@VsoMethod(showInApi = true, name = "getImageByTag")
	public DockerImage getImageByTag(String imageTag) throws Exception
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.images) 
			{
				Iterator<DockerImage> itr = this.images.iterator();
				while (itr.hasNext())
				{
					DockerImage image = itr.next();
					log.debug("Searching images. Current image: " + image.getImageId() + ".");
					if (image.getImageTag().equals(imageTag))
					{
						log.debug("Found image '" + imageTag + "'.");
						return image;
					}
				}
			}
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Image with tag '" + imageTag + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadImages();
			}
		}

		// if not found in cache after cache was synchronized, the image dosn't exist on this host. Return null.
		throw new Exception("Image with tag '" + imageTag + "' not found on this node.");
	}
	
	// CRUD - Read
	@VsoMethod(showInApi = true, name = "getImageById")
	public DockerImage getImageById(String imageId) throws Exception
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.images) 
			{
				Iterator<DockerImage> itr = this.images.iterator();
				while (itr.hasNext())
				{
					DockerImage image = itr.next();
					log.debug("Searching images. Current image: " + image.getImageId() + ".");
					if (image.getImageId().equals(imageId))
					{
						log.debug("Found image '" + imageId + "'.");
						return image;
					}
				}
			}
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Image with image id '" + imageId + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadImages();
			}
		}

		// if not found in cache after cache was synchronized, the image dosn't exist on this host. Return null.
		throw new Exception("Image with image id '" + imageId + "' not found on this node.");
	}

	// CRUD - Delete
	@VsoMethod(showInApi = true, name = "deleteImage", description = "Deletes an image from the docker node")
	public void removeImage(String id, boolean force) throws Exception
	{
		log.debug("Removing image '" + id + "'.");
		configureNode();
		DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

		try
		{
			log.debug("Executing...");
			dockerClient.removeImageCmd(id).withForce(force).exec();
		}
		catch (NotFoundException e)
		{
			// image was not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the image was not found.");
		}
		catch (ConflictException e)
		{
			// image is in use by some container
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the image is used by one or more containers.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while removeing image: " + sw.getBuffer().toString());
			// Throw error detail message so vCO can display it
			throw new Exception("Error while removing image: " + sw.getBuffer().toString());
		}

		// reload images from docker node
		this.reloadImages();
		// update inventory - another way to do this would be to update our ArrayList and call notifyElementDeleted on the image object
		notificationHandler.notifyElementInvalidate(toRef());
	}

	// Other
	@VsoMethod(showInApi = true, name = "searchImage", description = "Returns a list of images from the docker hub repository matching the input string.")
	public String[] searchImage(String imageName) throws Exception
	{
		return searchImage(imageName, 0);
	}

	// Other
	@VsoMethod(showInApi = true, name = "searchImage", description = "Returns a list of images from the docker hub repository matching the input string.")
	public String[] searchImage(String imageName, int limit) throws Exception
	{
		log.debug("Searching image '" + imageName + "' with limit " + limit + ".");
		// prevent negative values
		if (limit < 0)
		{
			limit = 0;
		}

		DockerClient dockerClient = null;
		List<SearchItem> result = null;
		try
		{
			configureNode();
			dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
			result = dockerClient.searchImagesCmd(imageName).exec();
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while searching image: " + sw.getBuffer().toString());
			// Throw error detail message so vCO can display it
			throw new Exception("Error while searching image: " + sw.getBuffer().toString());
		}

		ArrayList<String> dyn = new ArrayList<String>();
		// return a string array so we can use the output in our vCO search fields
		Iterator<SearchItem> iterator = result.iterator();
		while (iterator.hasNext())
		{
			dyn.add(iterator.next().getName());

			// if we have a limit
			if (limit > 0)
			{
				// do not iterate more then limit
				if (dyn.size() >= limit)
				{
					break;
				}
			}
		}
		return dyn.toArray(new String[dyn.size()]);
	}

	// Container operations -------------------------------------------------------------------------------------------------

	// CRUD - Create
	@VsoMethod(showInApi = true, name = "createContainer", description = "Create a new container from an image on the docker node")
	public String createContainer(
			// complex objects
			@VsoParam(description = "The image to build the container from") DockerImage image,
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
		if (image == null)
		{
			throw new Exception("Error: no image specified.");
		}

		log.debug("Creating new container...");

		CreateContainerResponse response = null;
		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			CreateContainerCmd command = dockerClient
					.createContainerCmd(image.getImageId())
					.withAttachStdin(attachStdIn)
					.withAttachStdout(attachStdOut)
					.withAttachStderr(attachStdErr)
					.withTty(tty);

			if (cmd != null && (cmd.length > 0))
			{
				command.withCmd(cmd);
			}
			if(env != null && (env.length > 0))
			{
				command.withEnv(env);
			}
			if (name != null && !name.isEmpty())
			{
				command.withName(name);
			}
			if (ports != null && (ports.length > 0))
			{
				ArrayList<ExposedPort> newPorts = new ArrayList<ExposedPort>();
				for (DockerPort port : ports)
				{
					newPorts.add(port.toExposedPort());
				}
				//convert ArrayList to Array
				ExposedPort[] portArray = new ExposedPort[newPorts.size()];
				portArray = newPorts.toArray(portArray);

				command.withExposedPorts(portArray);
			}
			if(openStdIn)
			{
				// defaults to false
				command.withStdinOpen(true);
			}
			if(stdInOnce)
			{
				// defaults to false
				command.withStdInOnce(true);
			}
			if(workingDir != null && !workingDir.isEmpty())
			{
				command.withWorkingDir(workingDir);
			}
			if(hostname != null && !hostname.isEmpty())
			{
				command.withHostName(hostname);
			}
			if(NetworkDisabled)
			{
				// defaults to false
				command.withDisableNetwork(true);
			}
			if(user != null && !user.isEmpty())
			{
				command.withUser(user);
			}
			if(volumes != null && (volumes.length > 0))
			{
				//  Destination mount point within the container
				ArrayList<Volume> newVolumes = new ArrayList<Volume>();
				for(DockerVolume volume : volumes)
				{
					newVolumes.add(volume.toVolume());
				}
				//convert ArrayList to Array
				Volume[] volumeArray = new Volume[newVolumes.size()];
				volumeArray = newVolumes.toArray(volumeArray);
				command.withVolumes(volumeArray);
			}

			response = command.exec();
			log.debug("Created container '" + response.getId() + "'.");
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (ConflictException e)
		{
			// Named container already exists
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: a container with the specified name already exists.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while creating container: " + sw.getBuffer().toString());
			throw new Exception("Error while creating container: " + sw.getBuffer().toString());
		}

		// reload images from docker node
		this.reloadContainers();
		// update inventory - another way to do this would be to update our ArrayList and call notifyElementUpdated on the container object
		notificationHandler.notifyElementInvalidate(toRef());
		
		// return container id
		return response.getId();
	}

	// CRUD - Read
	@VsoMethod(showInApi = true, name = "getContainerByName")
	public DockerContainer getContainerByName(String containerName) throws Exception
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.containers) 
			{
				Iterator<DockerContainer> itr = this.containers.iterator();
				while (itr.hasNext())
				{
					DockerContainer container = itr.next();
					log.debug("Searching containers. Current container: " + container.getName() + ".");
					if (container.getName().equals(containerName))
					{
						log.debug("Found container '" + containerName + "'.");
						return container;
					}
				}
			}
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Container with name '" + containerName + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadContainers();
			}
		}

		// if not found in cache after cache was synchronized, the container dosn't exist on this host. Return null.
		throw new Exception("Container with name '" + containerName + "' not found on this node.");
	}
	
	// CRUD - Read
	@VsoMethod(showInApi = true, name = "getContainerById")
	public DockerContainer getContainerById(String containerId) throws Exception
	{
		for(int runs = 0; runs < 2; runs++)
		{
			//try to find in cache - must be in synchronized block
			synchronized (this.containers) 
			{
				Iterator<DockerContainer> itr = this.containers.iterator();
				while (itr.hasNext())
				{
					DockerContainer container = itr.next();
					log.debug("Searching containers. Current container: " + container.getName() + ".");
					if (container.getContainerId().equals(containerId))
					{
						log.debug("Found container '" + containerId + "'.");
						return container;
					}
				}
			}
			// if not found in cache, refresh the cache and re-run the cache search
			if(runs  == 0)
			{
				log.debug("Container with container id '" + containerId + "' not found in cache of this docker node. Refreshing cache...");
				this.reloadContainers();
			}
		}

		// if not found in cache after cache was synchronized, the container dosn't exist on this host. Return null.
		throw new Exception("Container with container id '" + containerId + "' not found on this node.");
	}
	
	// CRUD - Delete
	@VsoMethod(showInApi = true, name = "deleteContainer")
	public void removeContainer(DockerContainer container, boolean force, boolean removeVolumes) throws Exception
	{
		if(container == null)
		{
			throw new Exception("Error: no container specified.");
		}
		
		log.debug("Deleting container '" + container.getContainerId() + "' with force '" + force + "' and removeVolumes '" + removeVolumes + "'.");

		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			dockerClient
			.removeContainerCmd(container.getContainerId())
			.withForce(force)
			.withRemoveVolumes(removeVolumes)
			.exec();
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while deleting container: " + sw.getBuffer().toString());
			throw new Exception("Error while deleting container: " + sw.getBuffer().toString());
		}

		log.debug("Delete operation finished.");
		// reload images from docker node
		this.reloadContainers();
		// update inventory - another way to do this would be to update our ArrayList and call notifyElementDeleted on the container object
		notificationHandler.notifyElementInvalidate(toRef());
	}
	
	// Other
	@VsoMethod(showInApi = true, name = "startContainer")
	public void startContainer(
			// complex objects
			@VsoParam(description = "The container to start") DockerContainer container,
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
		if(container == null)
		{
			throw new Exception("Error: no container specified.");
		}

		log.debug("Starting container '" + id + "'.");
		
		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			StartContainerCmd command = dockerClient
					.startContainerCmd(container.getContainerId())
					.withPublishAllPorts(publishAllPorts);
			
			if(portBindings != null && (portBindings.length > 0))
			{
				Ports pb = new Ports();
				for(DockerPortBind portBind : portBindings)
				{
					pb.bind(portBind.getPort().toExposedPort(), Ports.Binding(portBind.getPortNumber()));
				}
				command.withPortBindings(pb);
			}
			if(volumeBindings != null && (volumeBindings.length > 0))
			{
				ArrayList<Bind> newBinds = new ArrayList<Bind>();
				for(DockerVolumeBind bind : volumeBindings)
				{
					Bind b;
					if(bind.getAccessMode().equals("ro"))
					{
						b = new Bind(bind.getMountPoint(), bind.getVolume().toVolume(), AccessMode.ro);
					}
					else
					{
						b = new Bind(bind.getMountPoint(), bind.getVolume().toVolume(), AccessMode.rw);
					}
					newBinds.add(b);
				}
				//convert ArrayList to Array
				Bind[] bindArray = new Bind[newBinds.size()];
				bindArray = newBinds.toArray(bindArray);
				command.withBinds(bindArray);
			}
			if(dns != null && (dns.length > 0))
			{
				command.withDns(dns);
			}
			if(dnsSearch != null && (dnsSearch.length > 0))
			{
				command.withDnsSearch(dnsSearch);
			}
			if(links != null && (links.length > 0))
			{
				ArrayList<Link> linksArray = new ArrayList<Link>();
				for(DockerLink link : links)
				{
					Link dockerLink = new Link(link.getContainer().getName(), link.getLinkAlias());
					linksArray.add(dockerLink);
				}
				
				//convert ArrayList to Array
				Link[] dockerLinks = new Link[linksArray.size()];
				dockerLinks = linksArray.toArray(dockerLinks);
				
				command.withLinks(dockerLinks);
			}
			command.exec();
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (NotModifiedException e)
		{
			// Container already started
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container is already started.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while starting container: " + sw.getBuffer().toString());
			throw new Exception("Error while starting container: " + sw.getBuffer().toString());
		}
		log.debug("Start operation finished.");
		
		container.reloadContainer();
	}

	// Other
	@VsoMethod(showInApi = true, name = "stopContainer")
	public void stopContainer(DockerContainer container, int wait) throws Exception
	{
		if(container == null)
		{
			throw new Exception("Error: no container specified.");
		}
		
		log.debug("Stopping container '" + id + "'.");

		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			StopContainerCmd command = dockerClient.stopContainerCmd(container.getContainerId());
			if (wait < 0)
			{
				command.withTimeout(0);
			}
			else
			{
				command.withTimeout(wait);
			}
			command.exec();
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (NotModifiedException e)
		{
			// Container already stopped
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container is already stopped.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while stopping container: " + sw.getBuffer().toString());
			throw new Exception("Error while stopping container: " + sw.getBuffer().toString());
		}
		log.debug("Stop operation finished.");

		container.reloadContainer();
	}

	// Other
	@VsoMethod(showInApi = true, name = "restartContainer")
	public void restartContainer(DockerContainer container, int wait) throws Exception
	{
		if(container == null)
		{
			throw new Exception("Error: no container specified.");
		}
		
		log.debug("Restarting container '" + container.getContainerId() + "'.");

		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();
			RestartContainerCmd command = dockerClient.restartContainerCmd(container.getContainerId());
			
			if (wait < 0)
			{
				command.withtTimeout(0);
			}
			else
			{
				command.withtTimeout(wait);
			}
			command.exec();
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while restarting container: " + sw.getBuffer().toString());
			throw new Exception("Error while restarting container: " + sw.getBuffer().toString());
		}
		log.debug("Reload operation finished.");
		
		container.reloadContainer();
	}

	// Other
	@VsoMethod(showInApi = true, name = "killContainer")
	public void killContainer(DockerContainer container, String signal) throws Exception
	{
		if(container == null)
		{
			throw new Exception("Error: no container specified.");
		}
		
		log.debug("Killing container '" + container.getContainerId() + "' with signal '" + signal + "'.");

		try
		{
			configureNode();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).withServiceLoaderClassLoader(CooptoPluginAdaptor.class.getClassLoader()).build();

			KillContainerCmd command = dockerClient.killContainerCmd(container.getContainerId());
			if (signal != null && !signal.isEmpty())
			{
				command.withSignal(signal);
			}
			command.exec();
		}
		catch (NotFoundException e)
		{
			// container not found
			log.error(e.getMessage());
			// Throw error detail message so vCO can display it
			throw new Exception("Error: the container was not found.");
		}
		catch (Exception e)
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);

			log.error("Error while killing container: " + sw.getBuffer().toString());
			throw new Exception("Error while killing container: " + sw.getBuffer().toString());
		}
		log.debug("Kill operation finished.");

		container.reloadContainer();
	}
}
