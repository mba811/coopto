Coopto – a Docker Plug-in for VMware vRealize Orchestrator
======

Purpose of this project is to create a community process of plug-in development for vRealize Orchestrator (vRO) by fully integrating Docker, a new container technology.

Because the project focus is primary set on integrating functionality into vRO and not on making the Docker API accessible from Java, this project is using [docker-java](https://github.com/docker-java/docker-java), a free library which aims to do exactly that. Any functionality provided by the Docker API and not available within the [docker-java](https://github.com/docker-java/docker-java) library should not be implemented within Coopto, but contributed to the [docker-java](https://github.com/docker-java/docker-java) project.

#About Coopto
Coopto is a plug-in for VMware’s orchestration engine vRealize Orchestrator. It aims to provide full Docker functionality within the central automation component of the VMware stack in order to utilize and combine the power of container technology with virtualization technology.

We think that containers and VM may not only coexist but greatly benefit from each other. Parts where virtualization alone so far has greatly failed due to the high variation of competing formats in a very dynamic market can be moved to the shiny parts or container technology, e.g. the independent and therefore shiftable format.
Other aspects of cluster computation where virtualization has matured and proven enterprise ready within the last years can furthermore be implemented on the trusted and well known virtualization stack currently in use in most modern datacenter.

Digging in deeper the combination of both can result in an even more powerful computation stack then possible with just one of the technologies. A typical real world use-case would be providing a persistent storage to a container by leveraging existing vSphere storage APIs to create and attach a virtual HDD exclusively for that container, implicitly simplifying container management and backup for business critical data.

In order to bring both worlds together we use a simple to use and yet very common orchestration engine: vRealize Orchestrator. In fact: if you’re running on a vSphere stack, you probably already own vRealize Orchestrator.

![Coopto scope](/doc/github/readme/coopto-scope.png)

From our point of view both technologies, virtualization and containerization, currently have their advantages and disadvantages and therefore a right to exist. Time will show how both develop and eventually one will be the winner or both will merge into a new, even more powerful technology.
However this will turn out, within the here and now we want to combine the best of the two technologies. That’s where Coopto comes in – and due to its open source nature you can be a part of it, if you wish to.

#Version details
We can't say this often enough: be aware that this is still a early version of the plugin and besides that we cannot and will not guarantee stability and function of any component, due to heavy development many things can change in future.
This means that if you're using the current version of the plugin in order to build your custom workflows, you may need to change those workflows when upgradeing the Coopto plugin. There are plans to minimize the chances that user-created workflows need to be changed on plugin-changes as long as you're using the available actions and workflows only. So far we can not guarantee that yet, what basicly means that you'll have to check functionality of your Coopto workflows after upgradeing to a newer version of Coopto. It's self-explanatory that you should not do that in production.

#Getting started
If you own a copy of vRealize Orchestrator, you got everything you need to get started. Other products such as vRealize Automation may be combined but are not required at all if you just want to get some basic Docker functionality within your vSphere stack.

##Basic plug-in installation
This section explains how to download and install Coopto. Please note that we’re unable to provide you with a ready-to-run binary in the *.vmoapp format because the plugin will require some libraries that are intellectual property of VMware. This is why – at this time – we require you to compile the plug-in yourself. We’re already working on a solution to this legal limitation but as with any legal affairs it will take some time. Till then: don’t worry, it’s not that hard at all and we’ll lead you though the process step by step.

1.	Download the latest release of Coopto in the [release section of the project](https://github.com/m451/coopto/releases).
2.	You’ll get a *.zip file containing the source files of the release you selected. Unzip it and change to the folder.
3.	Follow the instructions within the [quick build section](https://github.com/m451/coopto#quick-build) of this document.
4.	Visit the configuration page, usually available at *https://your-vro-server:8283*. 
5.	Login, browse to plugins and upload the *.vmoapp file. For further advice please read the official vRO documentation available at http://www.vmware.com/de/support/vcenter-orchestrator.
6.	Once installed, you have to restart your vRO service. Still on the configuration website of vRO browse to start up options and select restart service. Depending on the hardware backing your vRO this should usually take between 1-3 minutes.
7.	Connect to your vRO using your vRO client. Again, if you have no idea how to do that you should take a look at the official vRO documentation.
8.	Within your vRO client in the inventory tab you now should have a new plug-in named Coopto. If not, repeat the steps described in this section.
9.	Notice that you should also have new objects within the COOPTO namespace available in your vRO API.
10.	The Coopto plug-in comes with a ready to use stack of actions and workflows. 
11.	You should find the workflows at
  - Library/Coopto
12.	You should find the actions at
  - de.fum.coopto.*

**Important**: if you’re unable to find the workflows and actions this is probably due to the fact that the *.package file containing the workflows is not signed with a yet trusted certificate in your vRO install. In order to get the workflows imported into your environment, please follow the instructions in the next section.

##Manually installing workflows
The *.package file that contains all workflows is a part of the *.vmoapp file and usually should be installed when you install the plug-in. Because of the open source nature of this project, we’re using a self-signed certificate for our open-source releases. The plug-in import utility of vRO however doesn’t provide a functionality to force trust on a new plug-in package. Thus, even after you installed the *.vmoapp the *.package file containing the workflows may not be installed and you end up with no Coopto workflows or actions being available within vRO. Please follow the instructions below for manually installing the Coopto workflows into your vRO.

1.	Extract the content of the *.vmoapp file using some ZIP program
2.	Within the extracted folder you should find a *.dar file. Once again, extract the content of that file using some ZIP program.
3.	Within the extracted content of the *.dar file browse to resources --> packages. You should see a *.package file.
4.	Open your vRO Client and change the view from “Run” (default) to “Design”. Switch to the “Packages” tab.
5.	In the “Packages” tab use the “import package” command and select the *.package file you just extracted.
6.	When prompted, select “trust once” if you wish to trust the certificate we use only this time or “always” if you wish to always trust the certificate we use. Please keep in mind that the private key used for the certificate is publicly available within the source files of Coopto, which may compromise the security of your vRO install if you choose to “always” trust the certificate. We refer you to the official vRO documentation for further information.

##Adding a Docker node
This section explains how to configure your first Docker node within Coopto

###Requirements
Please note that Coopto currently only supports the following configuration:
- Docker with a remote API v >= 1.15*
- Docker remote API service running and listening on a TCP port
- The Docker remote API port is reachable from vRO
- The underlying Linux distribution that Docker runs on is negligible

*Other versions may work and we even provide a selection down to 1.13 but please note that those versions are not tested and may result in unexpected side effects or poor user experience. Also please note that the development process currently is done on Docker remote API v. 1.15.

If you want to secure the setup so your Docker remote API is not exposed to your entire network, giving everyone on that network control over your Docker service, consider protecting the remote API port using iptables. An alternative could be adding a second network interface only used by the remote API that runs on an isolated VLAN that only exists for the Docker hosts and your vRO server.

###Configuration
You can manage multiple Docker servers using Coopto. Adding a new Docker node is easy:

1.	Assuming that plug-in and workflow installation have been completed, open the workflow tab within vRO and browse to Library --> Coopto --> Configuration.
2.	Run the workflow “Add Docker Node” in order to add your first Docker server.
3.	Enter a display name, which will be used within your vRO inventory so you can easily identify your server.
4.	Enter the full qualified domain name of your Docker server or it’s IP address. Remember that the Docker remote API has to be configured to listen on that address.
5.	Enter the Docker remote API port number you’re using, 2375 by default.
6.	Select the API version of your Docker remote API. Check the requirements for further details regarding supported versions.
7.	Click submit

After the workflow finishes, you should see a new Docker node below the Coopto plug-in within the inventory tab in vRO. If any images or containers exist on that Docker node, they should be listed below the node, resulting in a view similar to the following screenshot.

![Coopto inventory](/doc/github/readme/vro-coopto-inventory.png)

###Usage best practices
Please notice the following best practices when working with Coopto:
-	Docker servers managed by Coopto should not be managed by any other software that manages Docker
-	Docker servers managed by Coopto should be exclusively managed using Coopto, manual operations should be avoided

The reason for this is that Coopto is only performing updates on changes (pull) but is not notified by Docker if something is changed (push). Thus, if you wish to have an always up-to-date inventory view on your Docker environment in vRO, all CRUD operations should be performed using Coopto. A side effect of a not up-to-date inventory view in vRO are failed workflows due to missing objects in Docker that are still assumed to exist within vRO. Even though a failed workflow is not the end of the world and the inventory will be updated after that fail, you probably want to prevent such errors. Thus you should only use Coopto for management. 

If you absolutely have to manage a Docker host manually, please update the inventory in vRO afterwards by right clicking on the Coopto object in the inventory tab in vRO and selecting “reload”. This will update the inventory to represent the current state of the Docker nodes. If you wish to automate this task, feel free to create a scheduled task that will update the inventory for you. 

#Usage examples
This plugin aims to provide all Docker operations within vRO. It’s up to you how to use and combine them with other technologies. This section will lead you through the process of deploying a custom container. If you want to get more usage examples please consider visiting our [sandbox examples wiki](https://github.com/m451/coopto/wiki/Sandbox-examples).

##Creating new containers
The plugin doesn’t limit you to any specific image or container functionality. Nearly all features supported by the Docker API are available to you. So this example will demonstrate how to run virtually any Docker image using Coopto. 
Once you installed the plugin as described within the [getting started section](https://github.com/m451/coopto#getting-started), start up your vRO client and connect to the vRO server you installed Coopto at.

###Part I – searching and pulling new images
Assuming you already added the Docker node you want to deploy to, as described within [getting started](https://github.com/m451/coopto#getting-started), the next step is to download the image you want to deploy a container from. This example will use the official Docker registry and the PostgreSQL image, but you could use any other registry and image.

1.	Start the Coopto workflow Library/Coopto/Images/**Pull image**
2.	Select the Docker node to use and a search method. There are two modes:
  -	enter-mode will require you to know the exact tag of the image you want to download but is quick and can be automated. In case of PostgreSQL the tag is *postgres:latest*.
  -	search-mode is supposed to be used if you’re looking for an image but you don’t  know the exact tag of it yet. It’ll let you input a search term and – after you give it some time to query the Docker registry – provide you with a list of images that contain the term you searched.
3.  We’ll select the enter-mode by choosing no and input *postgres:latest* within the textfield. Next press submit.
4.	The Docker node will now start processing your download. Depending on the image you’re pulling and the internet connection of your Docker node, this may take a while. Once finished, the workflow should end successfully and within the Coopto inventory you should see the new image (if not, click the little refresh icon on top of your vRO client’s inventory view once the workflow run has finished).

###Part II – creating a container
If you’re familiar with Docker you’re probably aware of the docker run command. What you probably don’t know is that in detail what docker run does is running two separate commands in a row: create and start. This is the way we’re doing it in Coopto. 

1.	Start the Coopto workflow /Library/Coopto/Containers/**Create Container**
2.	You’ll be presented with a quite big table of options. Don’t be afraid: you don’t need to know about all of them for now and you only have to input the information required for the image you want to deploy. Visit the information page of the PostgresSQL image on Docker Hub.
3.	As you can read on the information page, all you really need to provide to the container is:
  - *-e* parameter with the attribute *POSTGRES_PASSWORD=mysecretpassword*.
4.	So, knowing this we’ll do just that using the vRO UI we’re presented with. As image select the image you just pulled.
5.	Optionally, if you want to give the container a specific name, input a container name.
  ![GitHub Logo](/doc/github/wiki/create-conainert-basicsettings.png)
6.  Now the postgres image requires us to provide the initial root password within the environment variable called *POSTGRES_PASSWORD*. That option is hidden within the advanced parameters section. In order to access those, select yes for the advanced parameters input.
7.	Click next to open the advanced parameters section of the workflow and scroll down till you see the input for the environment variables. 
8.	Click into the input field of the ENV attribute, within the window that opens enter *POSTGRES_PASSWORD=mysecretpassword* where *mysecretpassword* is the password you want to use and click *insert value*
9.	Click *accept* and then *submit*.
  ![GitHub Logo](/doc/github/wiki/create-conainert-advsettings.png)
10.	Once finished, the workflow should end successfully and within the Coopto inventory you should see the new container (if not, click the little refresh icon on top of your vRO client’s inventory view once the workflow run has finished).

###Part III – starting a container
As we already mentioned within the previous section after you create your container you probably want to start it. We’ll do so now.

1.	Start the Coopto workflow /Library/Coopto/Containers/**Start Container**
2.	Leave the default settings selected and click *submit*
3.	Once finished, the workflow should end successfully and within the Coopto inventory you should see the state of the container changed from stopped to running (if not, click the little refresh icon on top of your vRO client’s inventory view once the workflow run has finished).

###Part IV – get connection information
Now that you rolled out your container, you probably want to connect to it. You can just visit the inventory view, select your container and lookup the information you need within the port bindings attribute. However, if you want to automate that process this probably is not convenient. In such a case, follow the steps provided next.

1.	Start the Coopto workflow Library/Coopto/Containers/**Get Connection**
2.	Leave the default settings selected and click *submit*
3.	The workflow will return the information you need for connecting to your Postgre database.
4.	Use any PostgreSQL client if you wish to connect to your container.
5.	You’re done!

#Help and issues
To provide you with the support you need to get things going we’ve put together a cosy wiki page for you. This is the first place you should visit if you got any issues or questions. 

If the issue you’re facing isn’t covered within the wiki you might consider visiting the Coopto thread within the [community forums of vRO](https://communities.vmware.com/community/vmtn/vcenter/orchestrator). Nothing there? Feel free to submit a new issue on the [issues page](https://github.com/m451/coopto/issues). 

Please understand that the open source nature of this project also means that we – as in “we the Coopto open source community” – cannot guarantee to provide you with support and even less with support within a certain response time. 

If you require enterprise support you might consider contacting vmware@fum.de but please be aware that this is out of scope of the open source process this project is based on and thus should not be submitted into the projects issues section.

#Contributing to Coopto
You are very welcomed to work with us on Coopto. You may provide anything that is within the interest of the plug-in, including but not limited to:
- Documentation enhancements
- New plug-in functionalities and improvements (Java)
- New general Docker related vRO actions / workflows (JavaScript)
- New special actions / workflows that combine Coopto and build-in vRO plugin technologies and provide solutions for common use-cases (JavaScript)
- New sandbox actions / workflows that demo plug-in functions that are not yet demoed by another sandbox action / workflow (JavaScript)
Please note that in order to keep the plug-in clean and its footprint small, we have to be picky about what additions we accept. *A rule of thumb for contributions* is that there has to be a general interest to the added functionality and that the functionality is not trivial, which means it cannot be implemented with little effort using the already available functions.

#Building
If you want to contribute, you’ll have to setup you environment first. The following sections are supposed to give you initial direction about the requirements and teach you everything you need to know for a basic compilation. If you’re a developer and want to contribute you might take a look at the [exemplary build stack section](https://github.com/m451/coopto/wiki/Exemplary-build-stack) within the projects wiki.

##Build requirements
This section lists the requirements for a successful build of this project.
- Connection to the internet (be aware that using a proxy may require additional Maven configuration) on your build machine
- A copy of vRealize Orchestrator (version >= 5.5.2), reachable from your build machine
- Maven version >= 2 installed on your build machine, download and install Maven version >= 2 from [maven.apache.org](http://maven.apache.org/download.cgi).
- Maven added to your path environment variable, see the install instructions on [maven.apache.org](http://maven.apache.org/download.cgi) for details.
- vRO Maven repository added to your Maven setup, see [maven documentation](http://maven.apache.org/guides/mini/guide-multiple-repositories.html) for details.
- Sonatypes OSS  repository added to your Maven setup, see [maven documentation](http://maven.apache.org/guides/mini/guide-multiple-repositories.html) for details.
- Maven central repository added to your Maven setup, see [maven documentation](http://maven.apache.org/guides/mini/guide-multiple-repositories.html) for details.

##Quick build
If all [build requirements](https://github.com/m451/coopto#build-requirements) are met, this section will lead you though the process of a quick build so you end up with an installable binary (*.vmoapp) of Coopto.

1.	Go to the folder where you unpacked the source files to
2.	You now have to find out the URL to your vRO Maven repository. This should be *http://your-vro-server:8280/vco-repo*. You may test if you’re able to connect to that repository by just opening the URL within your browser. Again: be aware of proxy settings.
3.	Now that you have found your vRO repository, you have to configure the project’s pom. Open the pom.xml file within the Coopto root folder. Search for the <properties> section and change the <repoUrl> so it fits you vRO repository.
4.	Depending on your Maven setup, this may already be enough for maven to fetch all dependent libraries. If downloading from the repository in the following steps fails for you, consider adding the content of <repositories> and <pluginRepositories> to your ~/.m2/settings.xml file. When doing so, remember to replace the ${repoUrl} variable with the URL of your vRO repository.
5.	Open a command prompt and change the directory to the unpacked Coopto folder.
6.	Run mv install
7.	Maven will compile Coopto and present you with a ready to use *.vmoapp file within the bin subfolder.

#Security Disclosure
If you have any issue regarding security, please disclose the information responsibly by sending an email to vmware@fum.de.

#Legal
Brought to you courtesy of our legal counsel. For more context, please see the notice and license documents. It is your responsibility to ensure that your use and/or transfer does not violate applicable laws.
All product and company names are trademarks™ or registered® trademarks of their respective holders. Use of them does not imply any affiliation with or endorsement by them.

#Licensing
Coopto is and open source project proudly presented by **Fritz & Macziol Software und Computervertrieb GmbH**.

This project is under GNU General Public License as published by the Free Software Foundation, either version 3 of the license, or (at your option) any later version. For more information, please review the *LICENSE* document. *You may not use this software or any parts of it except in compliance with the license*.

Please note that this project may include open source files not authored by the authors of this project and therefore may use different license models which you have to respect.

If you require to use this project or parts of it without the requirements of the General Public License, please contact the author(s).
