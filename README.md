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

![Coopto scope](/doc/github/readme/coopto-scope-01.png)

From our point of view both technologies, virtualization and containerization, currently have their advantages and disadvantages and therefore a right to exist. Time will show how both develop and eventually one will be the winner or both will merge into a new, even more powerful technology.
However this will turn out, within the here and now we want to combine the best of the two technologies. That’s where Coopto comes in – and due to its open source nature you can be a part of it, if you wish to.

#Version details
We can't say this often enough: be aware that this is still a **early version of the plugin** and besides that we cannot and will not guarantee stability and function of any component, due to heavy development many things can change in future.
This means that if you're using the current version of the plugin in order to build your custom workflows, you may need to change those workflows when upgradeing the Coopto plugin. There are plans to minimize the chances that user-created workflows need to be changed on plugin-changes as long as you're using the available actions and workflows only. So far we can not guarantee that yet, what basicly means that you'll have to check functionality of your Coopto workflows after upgradeing to a newer version of Coopto. It's self-explanatory that you should not do that in production.

#Getting started
If you own a copy of vRealize Orchestrator, you got everything you need to get started. Other products such as vRealize Automation may be combined but are not required at all if you just want to get some basic Docker functionality within your vSphere stack.

To get started you should visit the following sections of the plug-in wiki:

1. [Basic plug-in installation](https://github.com/m451/coopto/wiki/Basic-plug-in-installation)
3. [Adding a Docker node](https://github.com/m451/coopto/wiki/Adding-a-Docker-node)
4. [Usage examples](https://github.com/m451/coopto/wiki/Usage-examples)

#Help and issues
To provide you with the support you need to get things going we’ve put together a [cosy wiki](https://github.com/m451/coopto/wiki) page for you. There's also a section about [debugging](https://github.com/m451/coopto/wiki/Debugging). This is the first place you should visit if you got any issues or questions. 

If the issue you’re facing isn’t covered [within the wiki](https://github.com/m451/coopto/wiki) you might consider visiting the [Coopto thread](https://communities.vmware.com/thread/498430) within the [community forums of vRO](https://communities.vmware.com/community/vmtn/vcenter/orchestrator). Feel like you run into a software bug related to Coopto itself? Be free to submit a new issue on the [issues page](https://github.com/m451/coopto/issues). 

Please understand that the open source nature of this project also means that we – as in *we the Coopto open source community* – cannot guarantee to provide you with support and even less with support within a certain response time. 

If you require enterprise support with costs you might consider contacting [vmware@fum.de](mailto:vmware@fum.de) *but please be aware that this is out of scope of the open source process this project is based on and thus should not be submitted into the projects issues section*.


#Contributing to Coopto
You are very welcomed to work with us on Coopto. You may provide anything that is within the interest of the plug-in, including but not limited to:

- Documentation enhancements (Markdown - [wiki](https://github.com/m451/coopto/wiki))
- New plug-in functionalities and improvements (Java - [core](https://github.com/m451/coopto/tree/master/o11nplugin-coopto-core/src/main/java/org/hexlogic))
- New general Docker related vRO actions / workflows (JavaScript - [workflows & actions](https://github.com/m451/coopto/tree/master/o11nplugin-coopto-package/src/main/resources))
- New special actions / workflows that combine Coopto and build-in vRO plugin technologies and provide solutions for common use-cases (JavaScript -  [workflows & actions](https://github.com/m451/coopto/tree/master/o11nplugin-coopto-package/src/main/resources))
- New sandbox actions / workflows that demo plug-in functions that are not yet demoed by another sandbox action / workflow (JavaScript - [workflows & actions](https://github.com/m451/coopto/tree/master/o11nplugin-coopto-package/src/main/resources))
Please note that in order to keep the plug-in clean and its footprint small, we have to be picky about what additions we accept. *A rule of thumb for contributions* is that there has to be a general interest to the added functionality and that the functionality is not trivial, which means it cannot be implemented with little effort using the already available functions.

If you want to work on the Java core of Coopto, probably you should [check out this section of the wiki](https://github.com/m451/coopto/wiki/Exemplary-build-stack) to get you going. It'll give you initial direction about setting up an build environment.

#Security Disclosure
If you have any issue regarding security, please disclose the information responsibly by sending an email to vmware@fum.de.

#Licensing & Legal
Coopto - from now on “this project” or “this software” - is an open source project proudly presented by **Fritz & Macziol Software und Computervertrieb GmbH**.

This project is under GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the license, or (at your option) any later version. For more information, please review the license documents . *You may not use this software or any parts of it except in compliance with the license*. 

This software may include "Open Source Software", which means various open source software components licensed under the terms of applicable open source license agreements included in the materials relating to such software.
Open Source Software is composed of individual software components, each of which has its own copyright and its own applicable license conditions. Information about the used Open Source Software and their licenses can be found in the open_source_licenses.md file.
The Product may also include other components, which may contain additional open source software packages. One or more such open_source_licenses.md files may therefore accompany this Product.

Brought to you courtesy of our legal counsel. For more context, please see the notice and license documents. It is your responsibility to ensure that your use and/or transfer does not violate applicable laws.

All product and company names are trademarks™ or registered® trademarks of their respective holders. Use of them does not imply any affiliation with or endorsement by them.
